package org.uniprot.api.rest.controller;

import static org.uniprot.api.rest.output.UniProtMediaType.*;
import static org.uniprot.api.rest.output.header.HeaderFactory.createHttpDownloadHeader;
import static org.uniprot.api.rest.output.header.HeaderFactory.createHttpSearchHeader;
import static org.uniprot.api.rest.request.HttpServletRequestContentTypeMutator.isBrowserAsFarAsWeKnow;

import java.util.*;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.http.*;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.context.request.async.DeferredResult;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import org.uniprot.api.common.concurrency.Gatekeeper;
import org.uniprot.api.common.exception.ImportantMessageServiceException;
import org.uniprot.api.common.exception.TooManyRequestsException;
import org.uniprot.api.common.repository.search.QueryResult;
import org.uniprot.api.rest.output.UniProtMediaType;
import org.uniprot.api.rest.output.context.FileType;
import org.uniprot.api.rest.output.context.MessageConverterContext;
import org.uniprot.api.rest.output.context.MessageConverterContextFactory;
import org.uniprot.api.rest.pagination.PaginatedResultsEvent;
import org.uniprot.api.rest.request.BasicRequest;
import org.uniprot.api.rest.request.IdsSearchRequest;
import org.uniprot.api.rest.request.StreamRequest;
import org.uniprot.core.util.Pair;
import org.uniprot.core.util.PairImpl;
import org.uniprot.core.util.Utils;

import lombok.extern.slf4j.Slf4j;

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
        return getEntityResponse(entity, fields, null, request);
    }

    protected ResponseEntity<MessageConverterContext<T>> getEntityResponse(
            T entity,
            String fields,
            Map<String, List<Pair<String, Boolean>>> accessionSequenceMap,
            HttpServletRequest request) {
        MediaType contentType = getAcceptHeader(request);
        MessageConverterContext<T> context = converterContextFactory.get(resource, contentType);
        context.setFields(fields);
        context.setFileType(getBestFileTypeFromRequest(request));
        context.setEntityOnly(true);
        context.setIdSequenceRanges(accessionSequenceMap);
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

    protected ResponseEntity<MessageConverterContext<T>> getEntityResponseRdf(
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
        return this.getSearchResponse(
                result, fields, isDownload, subSequence, null, request, response);
    }

    protected ResponseEntity<MessageConverterContext<T>> getSearchResponse(
            QueryResult<T> result,
            String fields,
            boolean isDownload,
            boolean subSequence,
            Map<String, List<Pair<String, Boolean>>> idSequenceRangesMap,
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
            context.setIdSequenceRanges(idSequenceRangesMap);
            context.setEntities(result.getContent());
            context.setExtraOptions(result.getExtraOptions());
            context.setWarnings(result.getWarnings());
        }

        HttpHeaders headers = createHttpSearchHeader(contentType);
        if (isDownload) {
            context.setDownloadContentDispositionHeader(true);
            headers = createHttpDownloadHeader(context, request);
        }

        ApplicationEvent paginatedResultEvent =
                new PaginatedResultsEvent(this, request, response, result.getPageAndClean());
        publishPaginationEvent(paginatedResultEvent);
        return ResponseEntity.ok().headers(headers).body(context);
    }

    /**
     * This is to suppress a false positive sonar failure Sonar error says: "Logging should not be
     * vulnerable to injection attacks" This line does not log anything, this is why we added
     * suppress below
     */
    @SuppressWarnings("javasecurity:S5145")
    private void publishPaginationEvent(ApplicationEvent paginatedResultEvent) {
        this.eventPublisher.publishEvent(paginatedResultEvent);
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

    protected DeferredResult<ResponseEntity<MessageConverterContext<T>>> streamRdf(
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
        deferredResult.onTimeout(
                () -> {
                    throw new TooManyRequestsException(
                            "Timeout waiting to enter in the stream queue");
                });

        // create the deferred result on a thread pool to prevent blocking client
        try {
            downloadTaskExecutor.execute(
                    () -> {
                        try {
                            String userAgent = request.getHeader(HttpHeaders.USER_AGENT);
                            if (isGatekeeperNeeded(userAgent)) {
                                runRequestIfNotBusy(contextSupplier, request, deferredResult);
                            } else {
                                runRequestNow(contextSupplier, request, deferredResult);
                            }
                        } catch (Throwable e) {
                            log.error("Error occurred during processing.", e);
                            if (Utils.notNull(downloadGatekeeper)) {
                                downloadGatekeeper.exit();
                            }
                            deferredResult.setErrorResult(e);
                        }
                    });
        } catch (TaskRejectedException ex) {
            String errorMessage =
                    String.format(
                            "Task executor rejected stream request (space inside=%d)",
                            downloadGatekeeper.getSpaceInside());
            log.info(errorMessage);
            throw new TooManyRequestsException(errorMessage);
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

    protected Optional<String> getAcceptedRdfContentType(HttpServletRequest request) {
        MediaType contentType = getAcceptHeader(request);
        if (UniProtMediaType.SUPPORTED_RDF_MEDIA_TYPES.containsKey(contentType)) {
            return Optional.of(SUPPORTED_RDF_MEDIA_TYPES.get(contentType));
        }
        return Optional.empty();
    }

    protected void setBasicRequestFormat(BasicRequest basicRequest, HttpServletRequest request) {
        MediaType mediaType = getAcceptHeader(request);
        basicRequest.setFormat(mediaType.toString());
    }

    protected MessageConverterContext<T> createStreamContext(
            StreamRequest streamRequest, MediaType contentType, HttpServletRequest request) {
        MessageConverterContext<T> context = converterContextFactory.get(resource, contentType);
        context.setDownloadContentDispositionHeader(streamRequest.isDownload());
        context.setFileType(getBestFileTypeFromRequest(request));
        return context;
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
    protected void runRequestIfNotBusy(
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
            String errorMessage =
                    String.format(
                            "Gatekeeper did NOT let me in (space inside=%d)",
                            downloadGatekeeper.getSpaceInside());
            log.info(errorMessage);
            throw new TooManyRequestsException(errorMessage);
        }
    }

    private boolean isGatekeeperNeeded(String userAgent) {
        return Utils.notNull(downloadGatekeeper) && !isBrowserAsFarAsWeKnow(userAgent);
    }

    protected Map<String, List<Pair<String, Boolean>>> getIdSequenceRangesMap(
            IdsSearchRequest idsSearchRequest, Pattern idSequencePattern) {
        Map<String, List<Pair<String, Boolean>>> idRangesMap = new HashMap<>();
        for (String passedId : idsSearchRequest.getCommaSeparatedIds().split(",")) {
            String sanitisedId = passedId.strip().toUpperCase();
            String sequenceRange = null;
            if (idSequencePattern.matcher(sanitisedId).matches()) {
                sequenceRange = getSequenceRange(sanitisedId);
                sanitisedId = extractId(sanitisedId);
            }
            Pair<String, Boolean> rangeIsProcessedPair =
                    new PairImpl<>(sequenceRange, Boolean.FALSE);
            List<Pair<String, Boolean>> rangeIsProcessedPairs;
            if (!idRangesMap.containsKey(sanitisedId)) {
                rangeIsProcessedPairs = new ArrayList<>();
                rangeIsProcessedPairs.add(rangeIsProcessedPair);
                idRangesMap.put(sanitisedId, rangeIsProcessedPairs);
            } else {
                rangeIsProcessedPairs = idRangesMap.get(sanitisedId);
                rangeIsProcessedPairs.add(rangeIsProcessedPair);
            }
        }
        return idRangesMap;
    }

    protected String extractId(String id) {
        return id.substring(0, id.indexOf('['));
    }

    protected String getSequenceRange(String id) {
        return id.substring(id.indexOf('[') + 1, id.indexOf(']'));
    }
}
