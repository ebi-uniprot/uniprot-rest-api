package org.uniprot.api.rest.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.context.request.async.DeferredResult;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import org.uniprot.api.common.exception.InvalidRequestException;
import org.uniprot.api.common.repository.search.QueryResult;
import org.uniprot.api.rest.output.UniProtMediaType;
import org.uniprot.api.rest.output.context.FileType;
import org.uniprot.api.rest.output.context.MessageConverterContext;
import org.uniprot.api.rest.output.context.MessageConverterContextFactory;
import org.uniprot.api.rest.pagination.PaginatedResultsEvent;
import org.uniprot.api.rest.request.MutableHttpServletRequest;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.uniprot.api.rest.output.UniProtMediaType.*;
import static org.uniprot.api.rest.output.header.HeaderFactory.createHttpDownloadHeader;
import static org.uniprot.api.rest.output.header.HeaderFactory.createHttpSearchHeader;

/**
 * @param <T>
 * @author lgonzales
 */
@Slf4j
public abstract class BasicSearchController<T> {
    private final ApplicationEventPublisher eventPublisher;
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
            T entity, String fields, MediaType contentType) {
        handleUnknownMediaType(contentType, (MutableHttpServletRequest) request);
        MessageConverterContext<T> context = converterContextFactory.get(resource, contentType);
        context.setFields(fields);
        context.setEntityOnly(true);
        if (contentType.equals(LIST_MEDIA_TYPE)) {
            context.setEntityIds(Stream.of(getEntityId(entity)));
        } else {
            context.setEntities(Stream.of(entity));
        }

        ResponseEntity.BodyBuilder responseBuilder =
                getEntityRedirectId(entity)
                        .map(
                                id ->
                                        ResponseEntity.status(HttpStatus.SEE_OTHER)
                                                .header(
                                                        HttpHeaders.LOCATION,
                                                        getLocationURLForId(id)))
                        .orElse(ResponseEntity.ok());

        return responseBuilder.headers(createHttpSearchHeader(contentType)).body(context);
    }

    protected ResponseEntity<MessageConverterContext<T>> getSearchResponse(
            QueryResult<T> result,
            String fields,
            MediaType contentType,
            HttpServletRequest request,
            HttpServletResponse response) {
        handleUnknownMediaType(contentType, (MutableHttpServletRequest) request);
        MessageConverterContext<T> context = converterContextFactory.get(resource, contentType);

        context.setFields(fields);
        context.setFacets(result.getFacets());
        context.setMatchedFields(result.getMatchedFields());
        if (contentType.equals(LIST_MEDIA_TYPE)) {
            List<String> accList =
                    result.getContent().stream()
                            .map(this::getEntityId)
                            .collect(Collectors.toList());
            context.setEntityIds(accList.stream());
        } else {
            context.setEntities(result.getContent().stream());
        }
        this.eventPublisher.publishEvent(
                new PaginatedResultsEvent(this, request, response, result.getPageAndClean()));
        return ResponseEntity.ok().headers(createHttpSearchHeader(contentType)).body(context);
    }

    protected DeferredResult<ResponseEntity<MessageConverterContext<T>>> download(
            Stream<T> result,
            String fields,
            MediaType contentType,
            HttpServletRequest request,
            String encoding) {
        handleUnknownMediaType(contentType, (MutableHttpServletRequest) request);
        MessageConverterContext<T> context = converterContextFactory.get(resource, contentType);
        context.setFields(fields);
        context.setFileType(FileType.bestFileTypeMatch(encoding));
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

    protected abstract Optional<String> getEntityRedirectId(T entity);

    protected MediaType getAcceptHeader(HttpServletRequest request) {
        return UniProtMediaType.valueOf(request.getHeader(HttpHeaders.ACCEPT));
    }

    private void handleUnknownMediaType(MediaType contentType, MutableHttpServletRequest request) {
        if (contentType.getType().equals(UNKNOWN_MEDIA_TYPE_TYPE)) {
            request.addHeader(HttpHeaders.ACCEPT, DEFAULT_MEDIA_TYPE_VALUE);
            throw new InvalidRequestException(
                    "Invalid format requested: '" + contentType.getSubtype()+"'");
        }
    }

    private String getLocationURLForId(String redirectId) {
        String path = ServletUriComponentsBuilder.fromCurrentRequest().build().getPath();
        if (path != null) {
            path = path.substring(0, path.lastIndexOf('/') + 1) + redirectId;
        }
        return path;
    }
}
