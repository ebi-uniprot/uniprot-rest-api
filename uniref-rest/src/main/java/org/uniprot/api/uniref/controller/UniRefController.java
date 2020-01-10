package org.uniprot.api.uniref.controller;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.MediaType.APPLICATION_XML_VALUE;
import static org.uniprot.api.rest.output.UniProtMediaType.*;
import static org.uniprot.api.rest.output.context.MessageConverterContextFactory.Resource.UNIREF;

import java.util.Optional;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import javax.validation.constraints.Pattern;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.async.DeferredResult;
import org.uniprot.api.common.repository.search.QueryResult;
import org.uniprot.api.rest.controller.BasicSearchController;
import org.uniprot.api.rest.output.context.FileType;
import org.uniprot.api.rest.output.context.MessageConverterContext;
import org.uniprot.api.rest.output.context.MessageConverterContextFactory;
import org.uniprot.api.rest.validation.ValidReturnFields;
import org.uniprot.api.uniref.request.UniRefRequest;
import org.uniprot.api.uniref.service.UniRefQueryService;
import org.uniprot.core.uniref.UniRefEntry;
import org.uniprot.store.search.field.UniRefResultFields;
import org.uniprot.store.search.field.validator.FieldValueValidator;

/**
 * @author jluo
 * @date: 22 Aug 2019
 */
@RestController
@Validated
@RequestMapping("/uniref")
public class UniRefController extends BasicSearchController<UniRefEntry> {

    private static final int PREVIEW_SIZE = 10;

    private final UniRefQueryService queryService;
    private final MessageConverterContextFactory<UniRefEntry> converterContextFactory;

    @Autowired
    public UniRefController(
            ApplicationEventPublisher eventPublisher,
            UniRefQueryService queryService,
            MessageConverterContextFactory<UniRefEntry> converterContextFactory,
            ThreadPoolTaskExecutor downloadTaskExecutor) {
        super(eventPublisher, converterContextFactory, downloadTaskExecutor, UNIREF);
        this.queryService = queryService;
        this.converterContextFactory = converterContextFactory;
    }

    @RequestMapping(
            value = "/{id}",
            method = RequestMethod.GET,
            produces = {
                TSV_MEDIA_TYPE_VALUE,
                FASTA_MEDIA_TYPE_VALUE,
                LIST_MEDIA_TYPE_VALUE,
                APPLICATION_XML_VALUE,
                APPLICATION_JSON_VALUE,
                XLS_MEDIA_TYPE_VALUE
            })
    public ResponseEntity<MessageConverterContext<UniRefEntry>> getById(
            @PathVariable("id")
                    @Pattern(
                            regexp = FieldValueValidator.UNIREF_CLUSTER_ID_REX,
                            flags = {Pattern.Flag.CASE_INSENSITIVE},
                            message = "{search.invalid.id.value}")
                    String id,
            @ValidReturnFields(fieldValidatorClazz = UniRefResultFields.class)
                    @RequestParam(value = "fields", required = false)
                    String fields,
            HttpServletRequest request) {
        UniRefEntry entry = queryService.findByUniqueId(id);
        return super.getEntityResponse(entry, fields, request);
    }

    @RequestMapping(
            value = "/search",
            method = RequestMethod.GET,
            produces = {
                TSV_MEDIA_TYPE_VALUE,
                FASTA_MEDIA_TYPE_VALUE,
                LIST_MEDIA_TYPE_VALUE,
                APPLICATION_XML_VALUE,
                APPLICATION_JSON_VALUE,
                XLS_MEDIA_TYPE_VALUE
            })
    public ResponseEntity<MessageConverterContext<UniRefEntry>> search(
            @Valid UniRefRequest searchRequest,
            @RequestParam(value = "preview", required = false, defaultValue = "false")
                    boolean preview,
            HttpServletRequest request,
            HttpServletResponse response) {
        setPreviewInfo(searchRequest, preview);
        QueryResult<UniRefEntry> results = queryService.search(searchRequest);
        return super.getSearchResponse(results, searchRequest.getFields(), request, response);
    }

    @RequestMapping(
            value = "/download",
            method = RequestMethod.GET,
            produces = {
                TSV_MEDIA_TYPE_VALUE, LIST_MEDIA_TYPE_VALUE, APPLICATION_XML_VALUE,
                APPLICATION_JSON_VALUE, XLS_MEDIA_TYPE_VALUE, FASTA_MEDIA_TYPE_VALUE
            })
    public DeferredResult<ResponseEntity<MessageConverterContext<UniRefEntry>>> download(
            @Valid UniRefRequest searchRequest,
            @RequestHeader(value = "Accept", defaultValue = APPLICATION_XML_VALUE)
                    MediaType contentType,
            @RequestHeader(value = "Accept-Encoding", required = false) String encoding,
            HttpServletRequest request) {

        MessageConverterContext<UniRefEntry> context =
                converterContextFactory.get(UNIREF, contentType);
        context.setFileType(FileType.bestFileTypeMatch(encoding));
        context.setFields(searchRequest.getFields());
        if (contentType.equals(LIST_MEDIA_TYPE)) {
            context.setEntityIds(queryService.streamIds(searchRequest));
        } else {
            context.setEntities(queryService.stream(searchRequest));
        }

        return super.getDeferredResultResponseEntity(request, context);
    }

    @Override
    protected String getEntityId(UniRefEntry entity) {
        return entity.getId().getValue();
    }

    @Override
    protected Optional<String> getEntityRedirectId(UniRefEntry entity) {
        return Optional.empty();
    }

    private void setPreviewInfo(UniRefRequest searchRequest, boolean preview) {
        if (preview) {
            searchRequest.setSize(PREVIEW_SIZE);
        }
    }
}
