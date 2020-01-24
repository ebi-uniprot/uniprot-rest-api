package org.uniprot.api.proteome.controller;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.MediaType.APPLICATION_XML_VALUE;
import static org.uniprot.api.rest.output.UniProtMediaType.LIST_MEDIA_TYPE_VALUE;
import static org.uniprot.api.rest.output.UniProtMediaType.XLS_MEDIA_TYPE_VALUE;
import static org.uniprot.api.rest.output.context.MessageConverterContextFactory.Resource.GENECENTRIC;

import java.util.Optional;
import java.util.stream.Stream;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import javax.validation.constraints.Pattern;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.async.DeferredResult;
import org.uniprot.api.common.repository.search.QueryResult;
import org.uniprot.api.proteome.request.GeneCentricRequest;
import org.uniprot.api.proteome.service.GeneCentricService;
import org.uniprot.api.rest.controller.BasicSearchController;
import org.uniprot.api.rest.output.context.MessageConverterContext;
import org.uniprot.api.rest.output.context.MessageConverterContextFactory;
import org.uniprot.api.rest.validation.ValidReturnFields;
import org.uniprot.core.proteome.CanonicalProtein;
import org.uniprot.store.search.field.GeneCentricField;
import org.uniprot.store.search.field.UniProtSearchFields;
import org.uniprot.store.search.field.validator.FieldValueValidator;

/**
 * @author jluo
 * @date: 30 Apr 2019
 */
@RestController
@Validated
@RequestMapping("/genecentric")
public class GeneCentricController extends BasicSearchController<CanonicalProtein> {

    private final GeneCentricService service;

    @Autowired
    public GeneCentricController(
            ApplicationEventPublisher eventPublisher,
            GeneCentricService service,
            @Qualifier("GENECENTRIC")
                    MessageConverterContextFactory<CanonicalProtein> converterContextFactory,
            ThreadPoolTaskExecutor downloadTaskExecutor) {
        super(eventPublisher, converterContextFactory, downloadTaskExecutor, GENECENTRIC);
        this.service = service;
    }

    @RequestMapping(
            value = "/search",
            method = RequestMethod.GET,
            produces = {APPLICATION_JSON_VALUE, APPLICATION_XML_VALUE, LIST_MEDIA_TYPE_VALUE})
    public ResponseEntity<MessageConverterContext<CanonicalProtein>> searchCursor(
            @Valid GeneCentricRequest searchRequest,
            HttpServletRequest request,
            HttpServletResponse response) {
        QueryResult<CanonicalProtein> results = service.search(searchRequest);
        return super.getSearchResponse(results, searchRequest.getFields(), request, response);
    }

    @RequestMapping(
            value = "/upid/{upid}",
            produces = {APPLICATION_JSON_VALUE, APPLICATION_XML_VALUE, LIST_MEDIA_TYPE_VALUE})
    public ResponseEntity<MessageConverterContext<CanonicalProtein>> getByUpId(
            @PathVariable("upid")
                    @Pattern(
                            regexp = FieldValueValidator.PROTEOME_ID_REX,
                            flags = {Pattern.Flag.CASE_INSENSITIVE},
                            message = "{search.invalid.upid.value}")
                    String upid,
            HttpServletRequest request,
            HttpServletResponse response) {
        GeneCentricRequest searchRequest = new GeneCentricRequest();
        searchRequest.setQuery(
                UniProtSearchFields.GENECENTRIC.getField("upid").getName() + ":" + upid);
        QueryResult<CanonicalProtein> results = service.search(searchRequest);
        return super.getSearchResponse(results, searchRequest.getFields(), request, response);
    }

    @RequestMapping(
            value = "/{accession}",
            produces = {APPLICATION_JSON_VALUE, APPLICATION_XML_VALUE, LIST_MEDIA_TYPE_VALUE})
    public ResponseEntity<MessageConverterContext<CanonicalProtein>> getByAccession(
            @PathVariable("accession")
                    @Pattern(
                            regexp = FieldValueValidator.ACCESSION_REGEX,
                            flags = {Pattern.Flag.CASE_INSENSITIVE},
                            message = "{search.invalid.accession.value}")
                    String accession,
            @ValidReturnFields(fieldValidatorClazz = GeneCentricField.ResultFields.class)
                    @RequestParam(value = "fields", required = false)
                    String fields,
            HttpServletRequest request) {
        CanonicalProtein entry = service.findByUniqueId(accession.toUpperCase());
        return super.getEntityResponse(entry, fields, request);
    }

    @RequestMapping(
            value = "/download",
            method = RequestMethod.GET,
            produces = {LIST_MEDIA_TYPE_VALUE, APPLICATION_JSON_VALUE, XLS_MEDIA_TYPE_VALUE})
    public DeferredResult<ResponseEntity<MessageConverterContext<CanonicalProtein>>> download(
            @Valid GeneCentricRequest searchRequest,
            @RequestHeader(value = "Accept-Encoding", required = false) String encoding,
            HttpServletRequest request) {
        Stream<CanonicalProtein> result = service.download(searchRequest);
        return super.download(
                result, searchRequest.getFields(), getAcceptHeader(request), request, encoding);
    }

    @Override
    protected String getEntityId(CanonicalProtein entity) {
        return entity.getCanonicalProtein().getAccession().getValue();
    }

    @Override
    protected Optional<String> getEntityRedirectId(CanonicalProtein entity) {
        return Optional.empty();
    }
}
