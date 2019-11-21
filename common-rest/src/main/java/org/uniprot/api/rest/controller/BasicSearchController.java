package org.uniprot.api.rest.controller;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.uniprot.api.rest.output.UniProtMediaType.LIST_MEDIA_TYPE;
import static org.uniprot.api.rest.output.header.HeaderFactory.createHttpDownloadHeader;
import static org.uniprot.api.rest.output.header.HeaderFactory.createHttpSearchHeader;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyEmitter;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import org.uniprot.api.common.repository.search.QueryResult;
import org.uniprot.api.rest.output.UniProtMediaType;
import org.uniprot.api.rest.output.context.FileType;
import org.uniprot.api.rest.output.context.MessageConverterContext;
import org.uniprot.api.rest.output.context.MessageConverterContextFactory;
import org.uniprot.api.rest.output.converter.CustomResponseBodyEmitter;
import org.uniprot.api.rest.pagination.PaginatedResultsEvent;

/**
 * @param <T>
 * @author lgonzales
 */
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

    protected ResponseEntity<ResponseBodyEmitter> download(
            Stream<T> result,
            String fields,
            MediaType contentType,
            HttpServletRequest request,
            String encoding) {
        MessageConverterContext<T> context = converterContextFactory.get(resource, contentType);
        context.setFields(fields);
        context.setFileType(FileType.bestFileTypeMatch(encoding));
        if (contentType.equals(LIST_MEDIA_TYPE)) {
            context.setEntityIds(result.map(this::getEntityId));
        } else {
            context.setEntities(result);
        }

        return getResponseBodyEmitterResponseEntity(request, context);
    }

    protected ResponseEntity<ResponseBodyEmitter> getResponseBodyEmitterResponseEntity(
            HttpServletRequest request, MessageConverterContext<T> context) {
        ResponseBodyEmitter emitter = new CustomResponseBodyEmitter();
        downloadTaskExecutor.execute(
                () -> {
                    try {
                        emitter.send(context, context.getContentType());
                    } catch (IOException e) {
                        emitter.completeWithError(e);
                    }
                    emitter.complete();
                });

        return ResponseEntity.ok()
                .headers(createHttpDownloadHeader(context, request))
                .body(emitter);
    }

    protected abstract String getEntityId(T entity);

    protected abstract Optional<String> getEntityRedirectId(T entity);

    private String getLocationURLForId(String redirectId) {
        String path = ServletUriComponentsBuilder.fromCurrentRequest().build().getPath();
        if (path != null) {
            path = path.substring(0, path.lastIndexOf('/') + 1) + redirectId;
        }
        return path;
    }

    protected MediaType getAcceptHeader(HttpServletRequest request) {
        String acceptHeader = request.getHeader("Accept");
        if (StringUtils.isEmpty(acceptHeader) || "*/*".equals(acceptHeader)) {
            return UniProtMediaType.valueOf(APPLICATION_JSON_VALUE);
        } else {
            return UniProtMediaType.valueOf(acceptHeader);
        }
    }
}
