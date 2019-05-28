package uk.ac.ebi.uniprot.api.proteome.controller;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.MediaType.APPLICATION_XML_VALUE;
import static uk.ac.ebi.uniprot.api.rest.output.UniProtMediaType.FASTA_MEDIA_TYPE_VALUE;
import static uk.ac.ebi.uniprot.api.rest.output.UniProtMediaType.FF_MEDIA_TYPE_VALUE;
import static uk.ac.ebi.uniprot.api.rest.output.UniProtMediaType.GFF_MEDIA_TYPE_VALUE;
import static uk.ac.ebi.uniprot.api.rest.output.UniProtMediaType.LIST_MEDIA_TYPE;
import static uk.ac.ebi.uniprot.api.rest.output.UniProtMediaType.LIST_MEDIA_TYPE_VALUE;
import static uk.ac.ebi.uniprot.api.rest.output.UniProtMediaType.TSV_MEDIA_TYPE_VALUE;
import static uk.ac.ebi.uniprot.api.rest.output.UniProtMediaType.XLS_MEDIA_TYPE_VALUE;
import static uk.ac.ebi.uniprot.api.rest.output.context.MessageConverterContextFactory.Resource.PROTEOME;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.annotations.Api;
import uk.ac.ebi.uniprot.api.common.repository.search.QueryResult;
import uk.ac.ebi.uniprot.api.proteome.request.ProteomeRequest;
import uk.ac.ebi.uniprot.api.proteome.request.ProteomeReturnFieldsValidator;
import uk.ac.ebi.uniprot.api.proteome.service.ProteomeQueryService;
import uk.ac.ebi.uniprot.api.rest.output.context.MessageConverterContext;
import uk.ac.ebi.uniprot.api.rest.output.context.MessageConverterContextFactory;
import uk.ac.ebi.uniprot.api.rest.pagination.PaginatedResultsEvent;
import uk.ac.ebi.uniprot.api.rest.validation.ValidReturnFields;
import uk.ac.ebi.uniprot.domain.proteome.ProteomeEntry;
import uk.ac.ebi.uniprot.search.field.validator.FieldValueValidator;

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
			@Qualifier("PROTEOME")
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
		QueryResult<?> results = queryService.search(searchRequest, context);
		this.eventPublisher.publishEvent(new PaginatedResultsEvent(this, request, response, results.getPageAndClean()));
        return ResponseEntity.ok()
                .headers(createHttpSearchHeader(contentType))
                .body(context);
	}
	  @RequestMapping(value = "/{upid}", method = RequestMethod.GET,
	            produces = {TSV_MEDIA_TYPE_VALUE, FF_MEDIA_TYPE_VALUE, LIST_MEDIA_TYPE_VALUE, APPLICATION_XML_VALUE,
	                        APPLICATION_JSON_VALUE, XLS_MEDIA_TYPE_VALUE, FASTA_MEDIA_TYPE_VALUE, GFF_MEDIA_TYPE_VALUE})
	    public ResponseEntity<Object> getByUpId(@PathVariable("upid")
	                                                 @Pattern(regexp = FieldValueValidator.PROTEOME_ID_REX,
	                                                        flags = {Pattern.Flag.CASE_INSENSITIVE},
	                                                        message ="{search.invalid.upid.value}")
	                                                 String upid,
	                                                 @ValidReturnFields(fieldValidatorClazz = ProteomeReturnFieldsValidator.class)
	                                                 @RequestParam(value = "fields", required = false)
	                                                 String fields,
	                                                 @RequestHeader(value = "Accept",
	                                                         defaultValue = APPLICATION_JSON_VALUE)
	                                                 MediaType contentType
	                                                 ) {
		  MessageConverterContext<ProteomeEntry> context = converterContextFactory.get(PROTEOME, contentType);
	        context.setFields(fields);
	        context.setEntityOnly(true);
	        ProteomeEntry entry = queryService.getByUPId(upid);
	        
	        if (contentType.equals(LIST_MEDIA_TYPE)) {
                context.setEntityIds(Stream.of(upid));
            } else {
                    context.setEntities(Stream.of(entry));
            }
	        return ResponseEntity.ok()
	                .headers(createHttpSearchHeader(contentType))
	                .body(context);
	    }

}
