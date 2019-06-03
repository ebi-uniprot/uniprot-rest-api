package uk.ac.ebi.uniprot.api.proteome.controller;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.MediaType.APPLICATION_XML_VALUE;
import static uk.ac.ebi.uniprot.api.rest.output.UniProtMediaType.LIST_MEDIA_TYPE;
import static uk.ac.ebi.uniprot.api.rest.output.UniProtMediaType.LIST_MEDIA_TYPE_VALUE;
import static uk.ac.ebi.uniprot.api.rest.output.context.MessageConverterContextFactory.Resource.GENECENTRIC;
import static uk.ac.ebi.uniprot.api.rest.output.header.HeaderFactory.createHttpSearchHeader;

import java.util.stream.Stream;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import javax.validation.constraints.Pattern;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.annotations.Api;
import uk.ac.ebi.uniprot.api.common.repository.search.QueryResult;
import uk.ac.ebi.uniprot.api.proteome.request.GeneCentricRequest;
import uk.ac.ebi.uniprot.api.proteome.service.GeneCentricService;
import uk.ac.ebi.uniprot.api.rest.output.context.MessageConverterContext;
import uk.ac.ebi.uniprot.api.rest.output.context.MessageConverterContextFactory;
import uk.ac.ebi.uniprot.api.rest.pagination.PaginatedResultsEvent;
import uk.ac.ebi.uniprot.domain.proteome.CanonicalProtein;
import uk.ac.ebi.uniprot.search.field.GeneCentricField;
import uk.ac.ebi.uniprot.search.field.validator.FieldValueValidator;

/**
 *
 * @author jluo
 * @date: 30 Apr 2019
 *
 */
@RestController
@Api(tags = { "genecentric" })
@Validated
@RequestMapping("/genecentric")
public class GeneCentricController {
	private final ApplicationEventPublisher eventPublisher;
	private final GeneCentricService service;
	private final MessageConverterContextFactory<CanonicalProtein> converterContextFactory;

	@Autowired
	public GeneCentricController(
			ApplicationEventPublisher eventPublisher,
			GeneCentricService service,
			@Qualifier("GENECENTRIC")
			MessageConverterContextFactory<CanonicalProtein> converterContextFactory) {
		this.eventPublisher = eventPublisher;
		this.service = service;
		this.converterContextFactory = converterContextFactory;
	}

	  @RequestMapping(value = "/search", method = RequestMethod.GET,
		      produces = {APPLICATION_JSON_VALUE, APPLICATION_XML_VALUE, LIST_MEDIA_TYPE_VALUE})
			public ResponseEntity<MessageConverterContext<CanonicalProtein>>  searchCursor(@Valid GeneCentricRequest searchRequest,
		             @RequestHeader(value = "Accept", defaultValue = APPLICATION_JSON_VALUE) MediaType contentType,
					HttpServletRequest request,
					HttpServletResponse response) {
				    MessageConverterContext<CanonicalProtein> context = converterContextFactory.get(GENECENTRIC, contentType);
				    context.setFields(searchRequest.getFields());
				QueryResult<?> results =service.search(searchRequest, context);
				this.eventPublisher.publishEvent(new PaginatedResultsEvent(this, request, response, results.getPageAndClean()));
			   
		        return ResponseEntity.ok()
		                .headers(createHttpSearchHeader(contentType))
		                .body(context);
			}
	
	@RequestMapping(value = "/upid/{upid}", produces = { APPLICATION_JSON_VALUE, APPLICATION_XML_VALUE, LIST_MEDIA_TYPE_VALUE })
	public ResponseEntity<MessageConverterContext<CanonicalProtein>> getByUpId(
			@PathVariable("upid") @Pattern(regexp = FieldValueValidator.PROTEOME_ID_REX, flags = {
					Pattern.Flag.CASE_INSENSITIVE }, message = "{search.invalid.upid.value}") String upid,
			@RequestHeader(value = "Accept", defaultValue = APPLICATION_JSON_VALUE) MediaType contentType,
			HttpServletRequest request,
			HttpServletResponse response) {
		MessageConverterContext<CanonicalProtein> context = converterContextFactory.get(GENECENTRIC, contentType);
		GeneCentricRequest searchRequest = new GeneCentricRequest();
		searchRequest.setQuery(GeneCentricField.Search.upid.name()+":" + upid);
		QueryResult<?> results =service.search(searchRequest, context);
		this.eventPublisher.publishEvent(new PaginatedResultsEvent(this, request, response, results.getPageAndClean()));
	   
        return ResponseEntity.ok()
                .headers(createHttpSearchHeader(contentType))
                .body(context);
	}

	@RequestMapping(value = "/{accession}", produces = { APPLICATION_JSON_VALUE, APPLICATION_XML_VALUE })
	public ResponseEntity<Object> getByAccession(
			@PathVariable("accession") @Pattern(regexp = FieldValueValidator.ACCESSION_REGEX, flags = {
					Pattern.Flag.CASE_INSENSITIVE }, message = "{search.invalid.accession.value}") String accession,
			@RequestHeader(value = "Accept", defaultValue = APPLICATION_JSON_VALUE) MediaType contentType) {

		MessageConverterContext<CanonicalProtein> context = converterContextFactory.get(GENECENTRIC, contentType);
		context.setEntityOnly(true);
		CanonicalProtein entry = service.getByAccession(accession);

		if (contentType.equals(LIST_MEDIA_TYPE)) {

			context.setEntityIds(Stream.of(entry.getCanonicalProtein().getAccession().getValue()));
		} else {
			context.setEntities(Stream.of(entry));
		}

		return ResponseEntity.ok().headers(createHttpSearchHeader(contentType)).body(context);
	}
}
