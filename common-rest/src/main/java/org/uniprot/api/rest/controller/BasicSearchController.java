package org.uniprot.api.rest.controller;

import static org.uniprot.api.rest.output.UniProtMediaType.*;
import static org.uniprot.api.rest.output.header.HeaderFactory.createHttpDownloadHeader;
import static org.uniprot.api.rest.output.header.HeaderFactory.createHttpSearchHeader;

import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Stream;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import lombok.extern.slf4j.Slf4j;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.http.*;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.context.request.async.DeferredResult;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import org.uniprot.api.common.concurrency.Gatekeeper;
import org.uniprot.api.common.exception.ImportantMessageServiceException;
import org.uniprot.api.common.exception.ResourceNotFoundException;
import org.uniprot.api.common.repository.search.ProblemPair;
import org.uniprot.api.common.repository.search.QueryResult;
import org.uniprot.api.rest.download.model.DownloadJob;
import org.uniprot.api.rest.download.repository.DownloadJobRepository;
import org.uniprot.api.rest.output.UniProtMediaType;
import org.uniprot.api.rest.output.context.FileType;
import org.uniprot.api.rest.output.context.MessageConverterContext;
import org.uniprot.api.rest.output.context.MessageConverterContextFactory;
import org.uniprot.api.rest.output.job.JobStatusResponse;
import org.uniprot.api.rest.pagination.PaginatedResultsEvent;
import org.uniprot.api.rest.request.StreamRequest;
import org.uniprot.core.util.Utils;

/**
 * @param <T>
 * @author lgonzales
 */
@Slf4j
public abstract class BasicSearchController<T> {
    protected final ApplicationEventPublisher eventPublisher;
    private final MessageConverterContextFactory<T> converterContextFactory;
    private final ThreadPoolTaskExecutor downloadTaskExecutor;
    private final MessageConverterContextFactory.Resource resource;
    private final Gatekeeper downloadGatekeeper;
    public static final int EXCEPTION_CODE = 50;

    protected BasicSearchController(
            ApplicationEventPublisher eventPublisher,
            MessageConverterContextFactory<T> converterContextFactory,
            ThreadPoolTaskExecutor downloadTaskExecutor,
            MessageConverterContextFactory.Resource resource) {
        this(eventPublisher, converterContextFactory, downloadTaskExecutor, resource, null);
    }

    protected BasicSearchController(
            ApplicationEventPublisher eventPublisher,
            MessageConverterContextFactory<T> converterContextFactory,
            ThreadPoolTaskExecutor downloadTaskExecutor,
            MessageConverterContextFactory.Resource resource,
            Gatekeeper downloadGatekeeper) {
        this.eventPublisher = eventPublisher;
        this.converterContextFactory = converterContextFactory;
        this.downloadTaskExecutor = downloadTaskExecutor;
        this.resource = resource;
        this.downloadGatekeeper = downloadGatekeeper;
    }

    public static String getLocationURLForId(
            String redirectId, String fromId, MediaType contentType) {
        String path = ServletUriComponentsBuilder.fromCurrentRequest().build().getPath();
        if (path != null) {
            String suffix = "";
            if (!contentType.equals(DEFAULT_MEDIA_TYPE)) {
                suffix = "." + UniProtMediaType.getFileExtension(contentType);
            }
            path = path.substring(0, path.lastIndexOf('/') + 1) + redirectId + suffix;
        }
        return path + "?from=" + fromId;
    }

    protected ResponseEntity<MessageConverterContext<T>> getEntityResponse(
            T entity, String fields, HttpServletRequest request) {
        MediaType contentType = getAcceptHeader(request);
        MessageConverterContext<T> context = converterContextFactory.get(resource, contentType);
        context.setFields(fields);
        context.setFileType(getBestFileTypeFromRequest(request));
        context.setEntityOnly(true);
        if (contentType.equals(LIST_MEDIA_TYPE)) {
            context.setEntityIds(Stream.of(getEntityId(entity)));
        } else {
            context.setEntities(Stream.of(entity));
        }

        ResponseEntity.BodyBuilder responseBuilder =
                getEntityRedirectId(entity, request)
                        .map(
                                id ->
                                        ResponseEntity.status(HttpStatus.SEE_OTHER)
                                                .header(
                                                        HttpHeaders.LOCATION,
                                                        getLocationURLForId(
                                                                id,
                                                                getFromId(entity, request),
                                                                contentType)))
                        .orElse(ResponseEntity.ok());

        return responseBuilder.headers(createHttpSearchHeader(contentType)).body(context);
    }

    protected ResponseEntity<MessageConverterContext<T>> getEntityResponseRDF(
            String entity, MediaType contentType, HttpServletRequest request) {
        MessageConverterContext<T> context = converterContextFactory.get(resource, contentType);
        context.setFileType(getBestFileTypeFromRequest(request));
        context.setEntityIds(Stream.of(entity));
        ResponseEntity.BodyBuilder responseBuilder = ResponseEntity.ok();
        return responseBuilder.headers(createHttpSearchHeader(contentType)).body(context);
    }

    protected ResponseEntity<MessageConverterContext<T>> getSearchResponse(
            QueryResult<T> result,
            String fields,
            HttpServletRequest request,
            HttpServletResponse response) {
        return this.getSearchResponse(result, fields, false, request, response);
    }

    protected ResponseEntity<MessageConverterContext<T>> getSearchResponse(
            QueryResult<T> result,
            String fields,
            boolean isDownload,
            HttpServletRequest request,
            HttpServletResponse response) {
        return this.getSearchResponse(result, fields, isDownload, false, request, response);
    }

    protected ResponseEntity<MessageConverterContext<T>> getSearchResponse(
            QueryResult<T> result,
            String fields,
            boolean isDownload,
            boolean subSequence,
            HttpServletRequest request,
            HttpServletResponse response) {
        MediaType contentType = getAcceptHeader(request);
        MessageConverterContext<T> context = converterContextFactory.get(resource, contentType);

        context.setFields(fields);
        context.setSubsequence(subSequence);
        context.setFacets(result.getFacets());
        context.setFileType(getBestFileTypeFromRequest(request));
        context.setMatchedFields(result.getMatchedFields());
        context.setSuggestions(result.getSuggestions());
        if (contentType.equals(LIST_MEDIA_TYPE)) {
            Stream<String> accList = result.getContent().map(this::getEntityId);
            context.setEntityIds(accList);
        } else {
            context.setEntities(result.getContent());
            context.setFailedIds(result.getFailedIds());
            context.setWarnings(result.getWarnings());
        }

        HttpHeaders headers = createHttpSearchHeader(contentType);
        if (isDownload) {
            context.setDownloadContentDispositionHeader(true);
            headers = createHttpDownloadHeader(context, request);
        }

        this.eventPublisher.publishEvent(
                new PaginatedResultsEvent(this, request, response, result.getPageAndClean()));
        return ResponseEntity.ok().headers(headers).body(context);
    }

    protected DeferredResult<ResponseEntity<MessageConverterContext<T>>> stream(
            Supplier<Stream<T>> resultSupplier,
            StreamRequest streamRequest,
            MediaType contentType,
            HttpServletRequest request) {
        Supplier<MessageConverterContext<T>> contextSupplier =
                () -> {
                    MessageConverterContext<T> context =
                            createStreamContext(streamRequest, contentType, request);
                    context.setFields(streamRequest.getFields());

                    Stream<T> results = resultSupplier.get();
                    if (contentType.equals(LIST_MEDIA_TYPE)) {
                        context.setEntityIds(results.map(this::getEntityId));
                    } else {
                        context.setEntities(results);
                    }
                    return context;
                };

        return getDeferredResultResponseEntity(contextSupplier, request);
    }

    protected DeferredResult<ResponseEntity<MessageConverterContext<T>>> streamRDF(
            Supplier<Stream<String>> rdfStreamSupplier,
            StreamRequest streamRequest,
            MediaType contentType,
            HttpServletRequest request) {

        Supplier<MessageConverterContext<T>> contextSupplier =
                () -> {
                    MessageConverterContext<T> context =
                            createStreamContext(streamRequest, contentType, request);

                    context.setEntityIds(rdfStreamSupplier.get());
                    return context;
                };
        return getDeferredResultResponseEntity(contextSupplier, request);
    }

    protected DeferredResult<ResponseEntity<MessageConverterContext<T>>>
            getDeferredResultResponseEntity(
                    Supplier<MessageConverterContext<T>> contextSupplier,
                    HttpServletRequest request) {

        DeferredResult<ResponseEntity<MessageConverterContext<T>>> deferredResult =
                new DeferredResult<>();
        deferredResult.onTimeout(() -> setTooManyRequestsResponse(deferredResult));

        // create the deferred result on a thread pool to prevent blocking client
        try {
            downloadTaskExecutor.execute(
                    () -> {
                        try {
                            if (Utils.notNull(downloadGatekeeper)) {
                                runRequestIfNotBusy(contextSupplier, request, deferredResult);
                            } else {
                                runRequestNow(contextSupplier, request, deferredResult);
                            }
                        } catch (Exception e) {
                            log.error("Error occurred during processing.", e);
                            if (Utils.notNull(downloadGatekeeper)) {
                                downloadGatekeeper.exit();
                            }
                            deferredResult.setErrorResult(e);
                        }
                    });
        } catch (TaskRejectedException ex) {
            log.info(
                    "Task executor rejected stream request (space inside={})",
                    downloadGatekeeper.getSpaceInside());
            setTooManyRequestsResponse(deferredResult);
        }

        return deferredResult;
    }

    protected abstract String getEntityId(T entity);

    protected String getFromId(T entity, HttpServletRequest request) {
        return getEntityId(entity);
    }

    protected abstract Optional<String> getEntityRedirectId(T entity, HttpServletRequest request);

    protected MediaType getAcceptHeader(HttpServletRequest request) {
        String mediaType = request.getHeader(HttpHeaders.ACCEPT);
        try {
            return UniProtMediaType.valueOf(mediaType);
        } catch (InvalidMediaTypeException e) {
            throw new ImportantMessageServiceException("The mediaType is invalid: " + mediaType, e);
        }
    }

    protected FileType getBestFileTypeFromRequest(HttpServletRequest request) {
        String encoding = request.getHeader(HttpHeaders.ACCEPT_ENCODING);
        return FileType.bestFileTypeMatch(encoding);
    }

    protected boolean isRDFAccept(HttpServletRequest request) {
        MediaType contentType = getAcceptHeader(request);
        return contentType.equals(RDF_MEDIA_TYPE);
    }

    protected MessageConverterContext<T> createStreamContext(
            StreamRequest streamRequest, MediaType contentType, HttpServletRequest request) {
        MessageConverterContext<T> context = converterContextFactory.get(resource, contentType);
        context.setDownloadContentDispositionHeader(streamRequest.isDownload());
        context.setFileType(getBestFileTypeFromRequest(request));
        return context;
    }

    protected ResponseEntity<JobStatusResponse> createAsyncDownloadStatus(
            DownloadJob job, String url) {
        ResponseEntity<JobStatusResponse> response;
        switch (job.getStatus()) {
            case NEW:
            case RUNNING:
            case FINISHED:
                response = ResponseEntity.ok(new JobStatusResponse(job.getStatus()));
                break;
            default:
                ProblemPair error = new ProblemPair(EXCEPTION_CODE, job.getError());
                response =
                        ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                .body(new JobStatusResponse(List.of(error)));
                break;
        }

        return response;
    }

    protected String constructDownloadRedirectUrl(String resultFile, String url) {
        String requestBaseUrl = extractRequestBaseUrl(url);
        return requestBaseUrl + "results/" + resultFile;
    }

    protected DownloadJob getAsyncDownloadJob(DownloadJobRepository jobRepository, String jobId) {
        Optional<DownloadJob> optJob = jobRepository.findById(jobId);
        return optJob.orElseThrow(
                () -> new ResourceNotFoundException("jobId" + jobId + "doesn't exist"));
    }

    private String extractRequestBaseUrl(String url) {
        int index = url.indexOf("status");
        return index == -1
                ? url.substring(0, url.indexOf("details")).replaceFirst("http://", "https://")
                : url.substring(0, index).replaceFirst("http://", "https://");
    }

    private void runRequestNow(
            Supplier<MessageConverterContext<T>> contextSupplier,
            HttpServletRequest request,
            DeferredResult<ResponseEntity<MessageConverterContext<T>>> deferredResult) {
        MessageConverterContext<T> context = contextSupplier.get();

        ResponseEntity<MessageConverterContext<T>> okayResponse =
                ResponseEntity.ok()
                        .headers(createHttpDownloadHeader(context, request))
                        .body(context);

        deferredResult.setResult(okayResponse);
    }

    /**
     * Runs a request only if a {@link Gatekeeper} indicates it is okay to do so. Note that the
     * entities are provided via a {@link Supplier}, so that they can be computed ONLY after the
     * gatekeeper says it is okay to do so.
     *
     * @param contextSupplier provides the {@link MessageConverterContext} for response
     * @param request the request
     * @param deferredResult the deferred result whose contents should be set
     */
    private void runRequestIfNotBusy(
            Supplier<MessageConverterContext<T>> contextSupplier,
            HttpServletRequest request,
            DeferredResult<ResponseEntity<MessageConverterContext<T>>> deferredResult) {

        if (downloadGatekeeper.enter()) {
            MessageConverterContext<T> context = contextSupplier.get();
            ResponseEntity<MessageConverterContext<T>> okayResponse =
                    ResponseEntity.ok()
                            .headers(createHttpDownloadHeader(context, request))
                            .body(context);
            context.setLargeDownload(true);

            log.info("Gatekeeper let me in (space inside={})", downloadGatekeeper.getSpaceInside());
            deferredResult.setResult(okayResponse);
        } else {
            log.info(
                    "Gatekeeper did NOT let me in (space inside={})",
                    downloadGatekeeper.getSpaceInside());
            setTooManyRequestsResponse(deferredResult);
        }
    }

    private void setTooManyRequestsResponse(
            DeferredResult<ResponseEntity<MessageConverterContext<T>>> result) {
        result.setResult(ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).build());
    }
}
