package uk.ac.ebi.uniprot.uuw.advanced.search.controller;

import static org.springframework.http.HttpHeaders.ACCEPT;
import static org.springframework.http.HttpHeaders.ACCEPT_ENCODING;
import static org.springframework.http.HttpHeaders.VARY;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.MediaType.APPLICATION_XML_VALUE;
import static uk.ac.ebi.uniprot.uuw.advanced.search.controller.UniprotAdvancedSearchController.UNIPROTKB_RESOURCE;
import static uk.ac.ebi.uniprot.uuw.advanced.search.http.context.MessageConverterContextFactory.Resource.UNIPROT;
import static uk.ac.ebi.uniprot.uuw.advanced.search.http.context.UniProtMediaType.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import javax.ws.rs.QueryParam;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyEmitter;

import uk.ac.ebi.uniprot.dataservice.restful.entry.domain.model.UPEntry;
import uk.ac.ebi.uniprot.uuw.advanced.search.event.PaginatedResultsEvent;
import uk.ac.ebi.uniprot.uuw.advanced.search.http.context.FileType;
import uk.ac.ebi.uniprot.uuw.advanced.search.http.context.MessageConverterContext;
import uk.ac.ebi.uniprot.uuw.advanced.search.http.context.MessageConverterContextFactory;
import uk.ac.ebi.uniprot.uuw.advanced.search.http.context.UniProtMediaType;
import uk.ac.ebi.uniprot.uuw.advanced.search.model.request.SearchRequestDTO;
import uk.ac.ebi.uniprot.uuw.advanced.search.model.response.QueryResult;
import uk.ac.ebi.uniprot.uuw.advanced.search.service.UniProtEntryService;

/**
 * Controller for uniprot advanced search service.
 *
 * @author lgonzales
 */
@RestController
@RequestMapping(UNIPROTKB_RESOURCE)
public class UniprotAdvancedSearchController {
    static final String UNIPROTKB_RESOURCE = "/uniprotkb";
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

    @RequestMapping(value = "/accession/{accession}", method = RequestMethod.GET, produces = APPLICATION_JSON_VALUE)
    public UPEntry getByAccession(@PathVariable("accession") String accession, @QueryParam("field") String field) {
        return entryService.getByAccession(accession, field);
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
            produces = {TSV_MEDIA_TYPE_VALUE, FF_MEDIA_TYPE_VALUE, LIST_MEDIA_TYPE_VALUE, APPLICATION_XML_VALUE,
            		APPLICATION_JSON_VALUE, XLS_MEDIA_TYPE_VALUE,FASTA_MEDIA_TYPE_VALUE})
    public ResponseEntity<ResponseBodyEmitter> download(
            @Valid SearchRequestDTO searchRequest,
            @RequestHeader("Accept") MediaType contentType,
            @RequestHeader(value = "Accept-Encoding", required = false) String encoding,
            HttpServletRequest request) {
        ResponseBodyEmitter emitter = new ResponseBodyEmitter();

        MessageConverterContext context = converterContextFactory.get(UNIPROT, contentType);
        context.setFileType(FileType.bestFileTypeMatch(encoding));
        context.setRequestDTO(searchRequest);

        entryService.stream(searchRequest, context, emitter);

        return ResponseEntity.ok()
                .headers(createHttpDownloadHeader(contentType, context, request))
                .body(emitter);
    }

    private HttpHeaders createHttpDownloadHeader(MediaType mediaType, MessageConverterContext context, HttpServletRequest request) {
        String suffix = "." +UniProtMediaType.getFileExtension(mediaType) + context.getFileType().getExtension();
        String queryString = request.getQueryString();
        String desiredFileName = "uniprot-" + queryString + suffix;
        String actualFileName;
        // truncate the file name if the query makes it too long -- instead use date + truncated query
        if (desiredFileName.length() > 200) {
            LocalDateTime now = LocalDateTime.now();
            DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy.MM.dd@HH:mm:ss.SS");
            String timestamp = now.format(dateTimeFormatter);
            int queryStrLength = queryString.length();
            int queryStringTruncatePoint = queryStrLength > 50 ? 50 : queryStrLength;
            actualFileName = "uniprot-" + timestamp + "-" + queryString.substring(0, queryStringTruncatePoint) + suffix;
        } else {
            actualFileName = desiredFileName;
        }
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.setContentDispositionFormData("attachment", actualFileName);
        httpHeaders.setContentType(mediaType);

        // used so that gate-way caching uses accept/accept-encoding headers as a key
        httpHeaders.add(VARY, ACCEPT);
        httpHeaders.add(VARY, ACCEPT_ENCODING);
        return httpHeaders;
    }
}
