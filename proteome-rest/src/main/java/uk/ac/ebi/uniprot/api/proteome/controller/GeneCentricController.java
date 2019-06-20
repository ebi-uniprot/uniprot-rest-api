package uk.ac.ebi.uniprot.api.proteome.controller;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.MediaType.APPLICATION_XML_VALUE;
import static uk.ac.ebi.uniprot.api.rest.output.UniProtMediaType.LIST_MEDIA_TYPE_VALUE;
import static uk.ac.ebi.uniprot.api.rest.output.UniProtMediaType.TSV_MEDIA_TYPE_VALUE;
import static uk.ac.ebi.uniprot.api.rest.output.UniProtMediaType.XLS_MEDIA_TYPE_VALUE;
import static uk.ac.ebi.uniprot.api.rest.output.context.MessageConverterContextFactory.Resource.GENECENTRIC;

import java.util.Optional;
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
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyEmitter;

import io.swagger.annotations.Api;
import uk.ac.ebi.uniprot.api.common.repository.search.QueryResult;
import uk.ac.ebi.uniprot.api.proteome.request.GeneCentricRequest;
import uk.ac.ebi.uniprot.api.proteome.request.GeneCentricReturnFieldsValidator;
import uk.ac.ebi.uniprot.api.proteome.service.GeneCentricService;
import uk.ac.ebi.uniprot.api.rest.controller.BasicSearchController;
import uk.ac.ebi.uniprot.api.rest.output.context.MessageConverterContext;
import uk.ac.ebi.uniprot.api.rest.output.context.MessageConverterContextFactory;
import uk.ac.ebi.uniprot.api.rest.validation.ValidReturnFields;
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
public class GeneCentricController extends BasicSearchController<CanonicalProtein> {
	
	private final GeneCentricService service;
	@Autowired
    public GeneCentricController(ApplicationEventPublisher eventPublisher, GeneCentricService service,
			@Qualifier("GENECENTRIC")
			MessageConverterContextFactory<CanonicalProtein> converterContextFactory,
                              ThreadPoolTaskExecutor downloadTaskExecutor) {
        super(eventPublisher, converterContextFactory, downloadTaskExecutor, GENECENTRIC);
        this.service = service;
    }

	  @RequestMapping(value = "/search", method = RequestMethod.GET,
		      produces = {APPLICATION_JSON_VALUE, APPLICATION_XML_VALUE, LIST_MEDIA_TYPE_VALUE})
			public ResponseEntity<MessageConverterContext<CanonicalProtein>>  searchCursor(@Valid GeneCentricRequest searchRequest,
		             @RequestHeader(value = "Accept", defaultValue = APPLICATION_JSON_VALUE) MediaType contentType,
					HttpServletRequest request,
					HttpServletResponse response) {
		  QueryResult<CanonicalProtein> results = service.search(searchRequest);
	     return super.getSearchResponse(results, searchRequest.getFields(), contentType, request, response);
			}
	
	@RequestMapping(value = "/upid/{upid}", produces = { APPLICATION_JSON_VALUE, APPLICATION_XML_VALUE, LIST_MEDIA_TYPE_VALUE })
	public ResponseEntity<MessageConverterContext<CanonicalProtein>> getByUpId(
			@PathVariable("upid") @Pattern(regexp = FieldValueValidator.PROTEOME_ID_REX, flags = {
					Pattern.Flag.CASE_INSENSITIVE }, message = "{search.invalid.upid.value}") String upid,
			@RequestHeader(value = "Accept", defaultValue = APPLICATION_JSON_VALUE) MediaType contentType,
			HttpServletRequest request,
			HttpServletResponse response) {
		GeneCentricRequest searchRequest = new GeneCentricRequest();
		searchRequest.setQuery(GeneCentricField.Search.upid.name()+":" + upid);
		 QueryResult<CanonicalProtein> results = service.search(searchRequest);
	     return super.getSearchResponse(results, searchRequest.getFields(), contentType, request, response);
	}

	@RequestMapping(value = "/{accession}", produces = { APPLICATION_JSON_VALUE, APPLICATION_XML_VALUE,LIST_MEDIA_TYPE_VALUE })
	public  ResponseEntity<MessageConverterContext<CanonicalProtein>> getByAccession(
			@PathVariable("accession") @Pattern(regexp = FieldValueValidator.ACCESSION_REGEX, flags = {
					Pattern.Flag.CASE_INSENSITIVE }, message = "{search.invalid.accession.value}") String accession,
			@ValidReturnFields(fieldValidatorClazz = GeneCentricReturnFieldsValidator.class) 
			@RequestParam(value = "fields", required = false) String fields,
			@RequestHeader(value = "Accept", defaultValue = APPLICATION_JSON_VALUE) MediaType contentType) {
		CanonicalProtein entry = service.getByAccession(accession);
		return super.getEntityResponse(entry, fields, contentType);
	}
	
    @RequestMapping(value = "/download", method = RequestMethod.GET,
            produces = { LIST_MEDIA_TYPE_VALUE, APPLICATION_JSON_VALUE, XLS_MEDIA_TYPE_VALUE})
    public ResponseEntity<ResponseBodyEmitter> download(@Valid
    		GeneCentricRequest searchRequest,
                                                        @RequestHeader(value = "Accept", defaultValue = APPLICATION_JSON_VALUE)
                                                        MediaType contentType,
                                                        @RequestHeader(value = "Accept-Encoding", required = false)
                                                        String encoding,
                                                        HttpServletRequest request) {
        Stream<CanonicalProtein> result = service.download(searchRequest);
        return super.download(result, searchRequest.getFields(), contentType, request,encoding);
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
