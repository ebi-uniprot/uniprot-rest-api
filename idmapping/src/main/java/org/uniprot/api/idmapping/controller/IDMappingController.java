package org.uniprot.api.idmapping.controller;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.MediaType.APPLICATION_XML_VALUE;
import static org.uniprot.api.rest.output.UniProtMediaType.TSV_MEDIA_TYPE_VALUE;
import static org.uniprot.api.rest.output.UniProtMediaType.XLS_MEDIA_TYPE_VALUE;
import static org.uniprot.api.rest.output.context.MessageConverterContextFactory.Resource.IDMAPPING_PIR;

import java.util.Optional;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.uniprot.api.common.repository.search.QueryResult;
import org.uniprot.api.idmapping.controller.request.IDMappingRequest;
import org.uniprot.api.idmapping.model.IDMappingStringPair;
import org.uniprot.api.idmapping.service.IDMappingService;
import org.uniprot.api.rest.controller.BasicSearchController;
import org.uniprot.api.rest.output.context.MessageConverterContext;
import org.uniprot.api.rest.output.context.MessageConverterContextFactory;

/**
 * Created 15/02/2021
 *
 * @author Edd
 */
@RestController
@Validated
@RequestMapping(value = IDMappingController.IDMAPPING_RESOURCE)
public class IDMappingController extends BasicSearchController<IDMappingStringPair> {
    static final String IDMAPPING_RESOURCE = "/idmapping";
    private final IDMappingService idMappingService;

    @Autowired
    public IDMappingController(
            ApplicationEventPublisher eventPublisher,
            IDMappingService idMappingService,
            MessageConverterContextFactory<IDMappingStringPair> converterContextFactory,
            ThreadPoolTaskExecutor downloadTaskExecutor) {
        super(eventPublisher, converterContextFactory, downloadTaskExecutor, IDMAPPING_PIR);
        this.idMappingService = idMappingService;
    }

    @PostMapping(
            value = "/pir",
            produces = {APPLICATION_JSON_VALUE})
    public ResponseEntity<MessageConverterContext<IDMappingStringPair>> pirSearch(
            @Valid @ModelAttribute IDMappingRequest searchRequest,
            HttpServletRequest request,
            HttpServletResponse response) {
        QueryResult<IDMappingStringPair> mappings = idMappingService.fetchIDMappings(searchRequest);
        return super.getSearchResponse(mappings, null, false, request, response);
    }

    @PostMapping(
            value = "/search",
            produces = {
                TSV_MEDIA_TYPE_VALUE,
                APPLICATION_JSON_VALUE,
                XLS_MEDIA_TYPE_VALUE,
                APPLICATION_XML_VALUE
            })
    public ResponseEntity<MessageConverterContext<IDMappingStringPair>> search(
            @Valid @ModelAttribute IDMappingRequest searchRequest,
            HttpServletRequest request,
            HttpServletResponse response) {
        QueryResult<IDMappingStringPair> mappings = idMappingService.fetchIDMappings(searchRequest);
        return super.getSearchResponse(mappings, null, true, request, response);
    }

    @PostMapping(
            value = "/stream",
            produces = {
                TSV_MEDIA_TYPE_VALUE,
                APPLICATION_JSON_VALUE,
                XLS_MEDIA_TYPE_VALUE,
                APPLICATION_XML_VALUE
            })
    public ResponseEntity<MessageConverterContext<IDMappingStringPair>> stream(
            @Valid @ModelAttribute IDMappingRequest searchRequest,
            HttpServletRequest request,
            HttpServletResponse response) {
        QueryResult<IDMappingStringPair> mappings = idMappingService.fetchIDMappings(searchRequest);
        return super.getSearchResponse(mappings, null, true, request, response);
    }

    @Override
    protected String getEntityId(IDMappingStringPair entity) {
        return null;
    }

    @Override
    protected Optional<String> getEntityRedirectId(IDMappingStringPair entity) {
        return Optional.empty();
    }
}
