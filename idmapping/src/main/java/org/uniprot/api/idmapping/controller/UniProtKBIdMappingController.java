package org.uniprot.api.idmapping.controller;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.uniprot.api.rest.output.context.MessageConverterContextFactory.Resource.UNIPROTKB;

import java.util.Optional;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.uniprot.api.common.repository.search.QueryResult;
import org.uniprot.api.idmapping.controller.request.IdMappingSearchRequest;
import org.uniprot.api.idmapping.model.StringUniProtKBEntryPair;
import org.uniprot.api.idmapping.service.UniProtKBIdService;
import org.uniprot.api.rest.controller.BasicSearchController;
import org.uniprot.api.rest.output.context.MessageConverterContext;
import org.uniprot.api.rest.output.context.MessageConverterContextFactory;

/**
 * @author sahmad
 * @created 17/02/2021
 */
@RestController
@RequestMapping(value = "/uniprotkb/idmapping/")
public class UniProtKBIdMappingController extends BasicSearchController<StringUniProtKBEntryPair> {

    private final UniProtKBIdService idService;

    @Autowired
    public UniProtKBIdMappingController(
            ApplicationEventPublisher eventPublisher,
            UniProtKBIdService idService,
            MessageConverterContextFactory<StringUniProtKBEntryPair> converterContextFactory,
            ThreadPoolTaskExecutor downloadTaskExecutor) {
        super(eventPublisher, converterContextFactory, downloadTaskExecutor, UNIPROTKB);
        this.idService = idService;
    }

    @GetMapping(
            value = "search",
            produces = {APPLICATION_JSON_VALUE})
    public ResponseEntity<MessageConverterContext<StringUniProtKBEntryPair>> getMappedEntries(
            @Valid IdMappingSearchRequest searchRequest,
            HttpServletRequest request,
            HttpServletResponse response) {

        QueryResult<StringUniProtKBEntryPair> result =
                this.idService.getMappedEntries(searchRequest);

        return super.getSearchResponse(result, searchRequest.getFields(), request, response);
    }

    @Override
    protected String getEntityId(StringUniProtKBEntryPair entity) {
        return entity.getEntry().getPrimaryAccession().getValue();
    }

    @Override
    protected Optional<String> getEntityRedirectId(StringUniProtKBEntryPair entity) {
        return Optional.empty();
    }
}
