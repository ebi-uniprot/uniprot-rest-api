package uk.ac.ebi.uniprot.api.proteome.controller;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.MediaType.APPLICATION_XML_VALUE;
import static uk.ac.ebi.uniprot.api.rest.output.UniProtMediaType.LIST_MEDIA_TYPE;
import static uk.ac.ebi.uniprot.api.rest.output.UniProtMediaType.LIST_MEDIA_TYPE_VALUE;
import static uk.ac.ebi.uniprot.api.rest.output.UniProtMediaType.TSV_MEDIA_TYPE_VALUE;
import static uk.ac.ebi.uniprot.api.rest.output.UniProtMediaType.XLS_MEDIA_TYPE_VALUE;
import static uk.ac.ebi.uniprot.api.rest.output.context.MessageConverterContextFactory.Resource.PROTEOME;
import static uk.ac.ebi.uniprot.api.rest.output.header.HeaderFactory.createHttpSearchHeader;

import java.util.List;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.annotations.Api;
import uk.ac.ebi.uniprot.api.common.repository.search.QueryResult;
import uk.ac.ebi.uniprot.api.proteome.request.ProteomeRequest;
import uk.ac.ebi.uniprot.api.proteome.service.ProteomeQueryService;
import uk.ac.ebi.uniprot.api.rest.output.context.MessageConverterContext;
import uk.ac.ebi.uniprot.api.rest.output.context.MessageConverterContextFactory;
import uk.ac.ebi.uniprot.api.rest.pagination.PaginatedResultsEvent;
import uk.ac.ebi.uniprot.domain.proteome.ProteomeEntry;

/**
 *
 * @author jluo
 * @date: 24 Apr 2019
 *
 */
@RestController
@Api(tags = { "proteome" })
@Validated
@RequestMapping("/proteome")
public class ProteomeController {
	private final ApplicationEventPublisher eventPublisher;
	private final ProteomeQueryService queryService;
	 private final MessageConverterContextFactory<ProteomeEntry> converterContextFactory;
	@Autowired
	public ProteomeController(ApplicationEventPublisher eventPublisher, ProteomeQueryService queryService,
			MessageConverterContextFactory<ProteomeEntry> converterContextFactory
			) {
		this.eventPublisher = eventPublisher;
		this.queryService = queryService;
		this.converterContextFactory =converterContextFactory;
	}

	  @RequestMapping(value = "/search", method = RequestMethod.GET,
      produces = {TSV_MEDIA_TYPE_VALUE, LIST_MEDIA_TYPE_VALUE, APPLICATION_XML_VALUE,
                  APPLICATION_JSON_VALUE, XLS_MEDIA_TYPE_VALUE})
	public ResponseEntity<MessageConverterContext<ProteomeEntry>>  searchCursor(@Valid ProteomeRequest searchRequest,
			 @RequestParam(value = "preview", required = false, defaultValue = "false") boolean preview,
             @RequestHeader(value = "Accept", defaultValue = APPLICATION_JSON_VALUE) MediaType contentType,
			HttpServletRequest request,
			HttpServletResponse response) {
		    MessageConverterContext<ProteomeEntry> context = converterContextFactory.get(PROTEOME, contentType);
		    context.setFields(searchRequest.getFields());
		QueryResult<ProteomeEntry> results = queryService.search(searchRequest);
		this.eventPublisher.publishEvent(new PaginatedResultsEvent(this, request, response, results.getPageAndClean()));
	    if (contentType.equals(LIST_MEDIA_TYPE)) {
            List<String> accList = results.getContent().stream().map(doc -> doc.getId().getValue()).collect(Collectors.toList());
            context.setEntityIds(accList.stream());
        } else {
        	 context.setEntities(results.getContent().stream());
        }
        return ResponseEntity.ok()
                .headers(createHttpSearchHeader(contentType))
                .body(context);
	}

}
