package org.uniprot.api.proteome.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.async.DeferredResult;
import org.uniprot.api.common.repository.search.QueryResult;
import org.uniprot.api.proteome.request.ProteomeRequest;
import org.uniprot.api.proteome.service.ProteomeQueryService;
import org.uniprot.api.rest.controller.BasicSearchController;
import org.uniprot.api.rest.output.context.MessageConverterContext;
import org.uniprot.api.rest.output.context.MessageConverterContextFactory;
import org.uniprot.api.rest.validation.ValidReturnFields;
import org.uniprot.core.proteome.ProteomeEntry;
import org.uniprot.store.search.field.ProteomeResultFields;
import org.uniprot.store.search.field.validator.FieldValueValidator;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import javax.validation.constraints.Pattern;
import java.util.Optional;
import java.util.stream.Stream;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.MediaType.APPLICATION_XML_VALUE;
import static org.uniprot.api.rest.output.UniProtMediaType.*;
import static org.uniprot.api.rest.output.context.MessageConverterContextFactory.Resource.PROTEOME;

/**
 * @author jluo
 * @date: 24 Apr 2019
 */
@RestController
@Validated
@RequestMapping("/proteome")
public class ProteomeController extends BasicSearchController<ProteomeEntry> {

    private final ProteomeQueryService queryService;

    @Autowired
    public ProteomeController(
            ApplicationEventPublisher eventPublisher,
            ProteomeQueryService queryService,
            @Qualifier("PROTEOME")
                    MessageConverterContextFactory<ProteomeEntry> converterContextFactory,
            ThreadPoolTaskExecutor downloadTaskExecutor) {
        super(eventPublisher, converterContextFactory, downloadTaskExecutor, PROTEOME);
        this.queryService = queryService;
    }

    @RequestMapping(
            value = "/search",
            method = RequestMethod.GET,
            produces = {
                TSV_MEDIA_TYPE_VALUE,
                LIST_MEDIA_TYPE_VALUE,
                APPLICATION_XML_VALUE,
                APPLICATION_JSON_VALUE,
                XLS_MEDIA_TYPE_VALUE
            })
    public ResponseEntity<MessageConverterContext<ProteomeEntry>> search(
            @Valid ProteomeRequest searchRequest,
            @RequestParam(value = "preview", required = false, defaultValue = "false")
                    boolean preview,
            HttpServletRequest request,
            HttpServletResponse response) {
        MediaType contentType = getAcceptHeader(request);
        QueryResult<ProteomeEntry> results = queryService.search(searchRequest);
        return super.getSearchResponse(
                results, searchRequest.getFields(), contentType, request, response);
    }

    @RequestMapping(
            value = "/{upid}",
            method = RequestMethod.GET,
            produces = {
                TSV_MEDIA_TYPE_VALUE,
                LIST_MEDIA_TYPE_VALUE,
                APPLICATION_XML_VALUE,
                APPLICATION_JSON_VALUE,
                XLS_MEDIA_TYPE_VALUE
            })
    public ResponseEntity<MessageConverterContext<ProteomeEntry>> getByUpId(
            @PathVariable("upid")
                    @Pattern(
                            regexp = FieldValueValidator.PROTEOME_ID_REX,
                            flags = {Pattern.Flag.CASE_INSENSITIVE},
                            message = "{search.invalid.upid.value}")
                    String upid,
            @ValidReturnFields(fieldValidatorClazz = ProteomeResultFields.class)
                    @RequestParam(value = "fields", required = false)
                    String fields,
            HttpServletRequest request) {
        MediaType contentType = getAcceptHeader(request);
        ProteomeEntry entry = queryService.findByUniqueId(upid.toUpperCase());
        return super.getEntityResponse(entry, fields, contentType, request);
    }

    @RequestMapping(
            value = "/download",
            method = RequestMethod.GET,
            produces = {
                TSV_MEDIA_TYPE_VALUE,
                LIST_MEDIA_TYPE_VALUE,
                APPLICATION_XML_VALUE,
                APPLICATION_JSON_VALUE,
                XLS_MEDIA_TYPE_VALUE
            })
    public DeferredResult<ResponseEntity<MessageConverterContext<ProteomeEntry>>> download(
            @Valid ProteomeRequest searchRequest,
            @RequestHeader(value = "Accept", defaultValue = APPLICATION_JSON_VALUE)
                    MediaType contentType,
            @RequestHeader(value = "Accept-Encoding", required = false) String encoding,
            HttpServletRequest request) {
        Stream<ProteomeEntry> result = queryService.download(searchRequest);
        return super.download(result, searchRequest.getFields(), contentType, request, encoding);
    }

    @Override
    protected String getEntityId(ProteomeEntry entity) {
        return entity.getId().getValue();
    }

    @Override
    protected Optional<String> getEntityRedirectId(ProteomeEntry entity) {
        return Optional.empty();
    }
}
