package uk.ac.ebi.uniprot.uuw.advanced.search.controller;

import org.apache.lucene.queryparser.flexible.core.QueryNodeException;
import org.apache.lucene.queryparser.flexible.standard.StandardQueryParser;
import org.apache.lucene.search.Query;
import org.apache.solr.parser.QueryParser;
import org.apache.solr.search.ExtendedDismaxQParser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.solr.core.QueryParsers;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyEmitter;
import uk.ac.ebi.uniprot.dataservice.restful.entry.domain.model.UPEntry;
import uk.ac.ebi.uniprot.uuw.advanced.search.event.PaginatedResultsEvent;
import uk.ac.ebi.uniprot.uuw.advanced.search.http.context.FileType;
import uk.ac.ebi.uniprot.uuw.advanced.search.http.context.MessageConverterContext;
import uk.ac.ebi.uniprot.uuw.advanced.search.http.context.MessageConverterContextFactory;
import uk.ac.ebi.uniprot.uuw.advanced.search.model.request.SearchRequestDTO;
import uk.ac.ebi.uniprot.uuw.advanced.search.model.response.QueryResult;
import uk.ac.ebi.uniprot.uuw.advanced.search.service.UniProtEntryService;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import javax.ws.rs.QueryParam;

import static org.springframework.http.HttpHeaders.*;
import static uk.ac.ebi.uniprot.uuw.advanced.search.controller.UniprotAdvancedSearchController.UNIPROTKB_RESOURCE;
import static uk.ac.ebi.uniprot.uuw.advanced.search.http.context.MessageConverterContextFactory.Resource.UNIPROT;

/**
 * Controller for uniprot advanced search service.
 *
 * @author lgonzales
 */
@RestController
@RequestMapping(UNIPROTKB_RESOURCE)
public class UniprotAdvancedSearchController {
    public static final String UNIPROTKB_RESOURCE = "/uniprotkb";
    private final ApplicationEventPublisher eventPublisher;

    private final UniProtEntryService entryService;
    private final MessageConverterContextFactory converterContextFactory;

    @Autowired
    public UniprotAdvancedSearchController(ApplicationEventPublisher eventPublisher,
                                           UniProtEntryService entryService,
                                           MessageConverterContextFactory converterContextFactory) {
        this.eventPublisher = eventPublisher;
        this.entryService = entryService;
        this.converterContextFactory = converterContextFactory;
    }


	@RequestMapping(value = "/search", method = RequestMethod.GET)
	public ResponseEntity<QueryResult<UPEntry>> searchCursor(@Valid SearchRequestDTO cursorRequest,
			HttpServletRequest request, HttpServletResponse response) {
		QueryResult<UPEntry> result = entryService.executeQuery(cursorRequest);
		eventPublisher.publishEvent(new PaginatedResultsEvent(this, request, response, result.getPageAndClean()));
		return new ResponseEntity<>(result, HttpStatus.OK);
	}

	@RequestMapping(value = "/accession/{accession}", method = RequestMethod.GET, produces= MediaType.APPLICATION_JSON_VALUE )
	public UPEntry getByAccession(@PathVariable("accession") String accession, @QueryParam("field") String field) {
		return entryService.getByAccession(accession,field);
	}


    /*
     * E.g., usage from command line:
     *
     * time curl -OJ -H "Accept:text/list" "http://localhost:8090/advancedsearch/uniprot/download?query=reviewed:true" (for just accessions)
     * time curl -OJ -H "Accept:text/flatfile" "http://localhost:8090/advancedsearch/uniprot/download?query=reviewed:true" (for entries)
     * time curl -OJ -H "Accept:application/xml" "http://localhost:8090/advancedsearch/uniprot/download?query=reviewed:true" (for XML entries)
     *
     * for GZIPPED results, add -H "Accept-Encoding:gzip"
     *
     * Note that by setting content-disposition header, we a file is downloaded (and it's not written to stdout).
     */
    @RequestMapping(value = "/download", method = RequestMethod.GET,
            produces = {"text/flatfile", "text/list", "application/xml"})
    public ResponseEntity<ResponseBodyEmitter> download(@RequestParam(value = "query", required = true) String query,
														@RequestHeader("Accept") MediaType contentType,
														@RequestHeader(value = "Accept-Encoding", required = false) String encoding,
														HttpServletRequest request) {
        ResponseBodyEmitter emitter = new ResponseBodyEmitter();

        MessageConverterContext context = converterContextFactory.get(UNIPROT, contentType);
        context.setFileType(FileType.bestFileTypeMatch(encoding));

        entryService.stream(query, context, emitter);

        return ResponseEntity.ok()
                .headers(createHttpDownloadHeader(contentType, context, request))
                .body(emitter);
    }

    private HttpHeaders createHttpDownloadHeader(MediaType mediaType, MessageConverterContext context, HttpServletRequest request) {
        String fileName = "uniprot-" + request.getQueryString() + "." + mediaType
                .getSubtype() + context.getFileType().getExtension();
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.setContentDispositionFormData("attachment", fileName);
        httpHeaders.setContentType(mediaType);

        // used so that gate-way caching uses accept/accept-encoding headers as a key
        httpHeaders.add(VARY, ACCEPT);
        httpHeaders.add(VARY, ACCEPT_ENCODING);
        return httpHeaders;
    }
}
