package org.uniprot.api.rest.controller;

import static org.uniprot.api.rest.output.UniProtMediaType.*;
import static org.uniprot.api.rest.output.header.HeaderFactory.createHttpDownloadHeader;
import static org.uniprot.api.rest.output.header.HeaderFactory.createHttpSearchHeader;

import java.util.Optional;
import java.util.stream.Stream;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import lombok.extern.slf4j.Slf4j;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.context.request.async.DeferredResult;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import org.uniprot.api.common.repository.search.QueryResult;
import org.uniprot.api.rest.output.UniProtMediaType;
import org.uniprot.api.rest.output.context.FileType;
import org.uniprot.api.rest.output.context.MessageConverterContext;
import org.uniprot.api.rest.output.context.MessageConverterContextFactory;
import org.uniprot.api.rest.pagination.PaginatedResultsEvent;
import org.uniprot.api.rest.request.StreamRequest;

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

    protected BasicSearchController(
            ApplicationEventPublisher eventPublisher,
            MessageConverterContextFactory<T> converterContextFactory,
            ThreadPoolTaskExecutor downloadTaskExecutor,
            MessageConverterContextFactory.Resource resource) {
        this.eventPublisher = eventPublisher;
        this.converterContextFactory = converterContextFactory;
        this.downloadTaskExecutor = downloadTaskExecutor;
        this.resource = resource;
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
        MediaType contentType = getAcceptHeader(request);
        MessageConverterContext<T> context = converterContextFactory.get(resource, contentType);

        context.setFields(fields);
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
            Stream<T> result, String fields, MediaType contentType, HttpServletRequest request) {
        MessageConverterContext<T> context = converterContextFactory.get(resource, contentType);
        context.setFields(fields);
        context.setFileType(getBestFileTypeFromRequest(request));
        if (contentType.equals(LIST_MEDIA_TYPE)) {
            context.setEntityIds(result.map(this::getEntityId));
        } else {
            context.setEntities(result);
        }
        return getDeferredResultResponseEntity(request, context);
    }

    protected DeferredResult<ResponseEntity<MessageConverterContext<T>>> stream(
            Stream<T> result,
            StreamRequest streamRequest,
            MediaType contentType,
            HttpServletRequest request) {
        MessageConverterContext<T> context = converterContextFactory.get(resource, contentType);
        context.setFields(streamRequest.getFields());
        context.setDownloadContentDispositionHeader(streamRequest.isDownload());
        context.setFileType(getBestFileTypeFromRequest(request));
        if (contentType.equals(LIST_MEDIA_TYPE)) {
            context.setEntityIds(result.map(this::getEntityId));
        } else {
            context.setEntities(result);
        }
        return getDeferredResultResponseEntity(request, context);
    }

    protected DeferredResult<ResponseEntity<MessageConverterContext<T>>>
            getDeferredResultResponseEntity(
                    HttpServletRequest request, MessageConverterContext<T> context) {
        // timeout in millis
        Long timeoutInMillis = (long) downloadTaskExecutor.getKeepAliveSeconds() * 1000;

        DeferredResult<ResponseEntity<MessageConverterContext<T>>> deferredResult =
                new DeferredResult<>(timeoutInMillis);

        deferredResult.onTimeout(() -> log.error("Request timeout occurred."));

        downloadTaskExecutor.execute(
                () -> {
                    try {
                        deferredResult.setResult(
                                ResponseEntity.ok()
                                        .headers(createHttpDownloadHeader(context, request))
                                        .body(context));
                    } catch (Exception e) {
                        log.error("Error occurred during processing.");
                        deferredResult.setErrorResult(e);
                    }
                });

        return deferredResult;
    }

    protected abstract String getEntityId(T entity);

    protected String getFromId(T entity, HttpServletRequest request) {
        return getEntityId(entity);
    }

    protected abstract Optional<String> getEntityRedirectId(T entity, HttpServletRequest request);

    protected MediaType getAcceptHeader(HttpServletRequest request) {
        return UniProtMediaType.valueOf(request.getHeader(HttpHeaders.ACCEPT));
    }

    protected FileType getBestFileTypeFromRequest(HttpServletRequest request) {
        String encoding = request.getHeader(HttpHeaders.ACCEPT_ENCODING);
        return FileType.bestFileTypeMatch(encoding);
    }

    protected DeferredResult<ResponseEntity<MessageConverterContext<T>>> streamRDF(
            Stream<String> rdfStream,
            StreamRequest streamRequest,
            MediaType contentType,
            HttpServletRequest request) {
        MessageConverterContext<T> context = converterContextFactory.get(resource, contentType);
        context.setDownloadContentDispositionHeader(streamRequest.isDownload());
        context.setFileType(getBestFileTypeFromRequest(request));
        context.setEntityIds(rdfStream);
        return getDeferredResultResponseEntity(request, context);
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

    protected boolean isRDFAccept(HttpServletRequest request) {
        MediaType contentType = getAcceptHeader(request);
        return contentType.equals(RDF_MEDIA_TYPE);
    }
}
