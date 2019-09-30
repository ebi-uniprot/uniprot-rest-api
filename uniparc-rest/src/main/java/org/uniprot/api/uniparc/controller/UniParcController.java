package org.uniprot.api.uniparc.controller;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.MediaType.APPLICATION_XML_VALUE;
import static org.uniprot.api.rest.output.UniProtMediaType.FASTA_MEDIA_TYPE_VALUE;
import static org.uniprot.api.rest.output.UniProtMediaType.LIST_MEDIA_TYPE_VALUE;
import static org.uniprot.api.rest.output.UniProtMediaType.TSV_MEDIA_TYPE_VALUE;
import static org.uniprot.api.rest.output.UniProtMediaType.XLS_MEDIA_TYPE_VALUE;
import static org.uniprot.api.rest.output.context.MessageConverterContextFactory.Resource.UNIPARC;

import java.util.Optional;
import java.util.stream.Stream;

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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyEmitter;
import org.uniprot.api.common.repository.search.QueryResult;
import org.uniprot.api.rest.controller.BasicSearchController;
import org.uniprot.api.rest.output.context.MessageConverterContext;
import org.uniprot.api.rest.output.context.MessageConverterContextFactory;
import org.uniprot.api.rest.validation.ValidReturnFields;
import org.uniprot.api.uniparc.request.UniParcRequest;
import org.uniprot.api.uniparc.service.UniParcQueryService;
import org.uniprot.core.uniparc.UniParcEntry;
import org.uniprot.store.search.field.UniParcResultFields;
import org.uniprot.store.search.field.validator.FieldValueValidator;

import io.swagger.annotations.Api;

/**
 * @author jluo
 * @date: 21 Jun 2019
 */
@RestController
@Api(tags = {"uniparc"})
@Validated
@RequestMapping("/uniparc")
public class UniParcController extends BasicSearchController<UniParcEntry> {

    private final UniParcQueryService queryService;
    private static final int PREVIEW_SIZE = 10;

    @Autowired
    public UniParcController(
            ApplicationEventPublisher eventPublisher,
            UniParcQueryService queryService,
            MessageConverterContextFactory<UniParcEntry> converterContextFactory,
            ThreadPoolTaskExecutor downloadTaskExecutor) {
        super(eventPublisher, converterContextFactory, downloadTaskExecutor, UNIPARC);
        this.queryService = queryService;
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
    public ResponseEntity<MessageConverterContext<UniParcEntry>> search(
            @Valid UniParcRequest searchRequest,
            @RequestParam(value = "preview", required = false, defaultValue = "false")
                    boolean preview,
            @RequestHeader(value = "Accept", defaultValue = APPLICATION_JSON_VALUE)
                    MediaType contentType,
            HttpServletRequest request,
            HttpServletResponse response) {
        setPreviewInfo(searchRequest, preview);
        QueryResult<UniParcEntry> results = queryService.search(searchRequest);
        return super.getSearchResponse(
                results, searchRequest.getFields(), contentType, request, response);
    }

    @RequestMapping(
            value = "/{upi}",
            method = RequestMethod.GET,
            produces = {
                TSV_MEDIA_TYPE_VALUE,
                FASTA_MEDIA_TYPE_VALUE,
                LIST_MEDIA_TYPE_VALUE,
                APPLICATION_XML_VALUE,
                APPLICATION_JSON_VALUE,
                XLS_MEDIA_TYPE_VALUE
            })
    public ResponseEntity<MessageConverterContext<UniParcEntry>> getByUpId(
            @PathVariable("upi")
                    @Pattern(
                            regexp = FieldValueValidator.UNIPARC_UPI_REX,
                            flags = {Pattern.Flag.CASE_INSENSITIVE},
                            message = "{search.invalid.upi.value}")
                    String upi,
            @ValidReturnFields(fieldValidatorClazz = UniParcResultFields.class)
                    @RequestParam(value = "fields", required = false)
                    String fields,
            @RequestHeader(value = "Accept", defaultValue = APPLICATION_JSON_VALUE)
                    MediaType contentType) {
        UniParcEntry entry = queryService.getById(upi);
        return super.getEntityResponse(entry, fields, contentType);
    }

    @RequestMapping(
            value = "/download",
            method = RequestMethod.GET,
            produces = {
                TSV_MEDIA_TYPE_VALUE,
                FASTA_MEDIA_TYPE_VALUE,
                LIST_MEDIA_TYPE_VALUE,
                APPLICATION_XML_VALUE,
                APPLICATION_JSON_VALUE,
                XLS_MEDIA_TYPE_VALUE
            })
    public ResponseEntity<ResponseBodyEmitter> download(
            @Valid UniParcRequest searchRequest,
            @RequestHeader(value = "Accept", defaultValue = APPLICATION_JSON_VALUE)
                    MediaType contentType,
            @RequestHeader(value = "Accept-Encoding", required = false) String encoding,
            HttpServletRequest request) {
        Stream<UniParcEntry> result = queryService.download(searchRequest);
        return super.download(result, searchRequest.getFields(), contentType, request, encoding);
    }

    @Override
    protected String getEntityId(UniParcEntry entity) {
        return entity.getUniParcId().getValue();
    }

    @Override
    protected Optional<String> getEntityRedirectId(UniParcEntry entity) {
        return Optional.empty();
    }

    private void setPreviewInfo(UniParcRequest searchRequest, boolean preview) {
        if (preview) {
            searchRequest.setSize(PREVIEW_SIZE);
        }
    }
}
