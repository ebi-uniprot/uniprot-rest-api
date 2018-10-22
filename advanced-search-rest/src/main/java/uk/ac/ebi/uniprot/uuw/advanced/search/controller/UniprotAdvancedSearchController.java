package uk.ac.ebi.uniprot.uuw.advanced.search.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyEmitter;
import uk.ac.ebi.uniprot.uuw.advanced.search.event.PaginatedResultsEvent;
import uk.ac.ebi.uniprot.uuw.advanced.search.http.context.FileType;
import uk.ac.ebi.uniprot.uuw.advanced.search.http.context.MessageConverterContext;
import uk.ac.ebi.uniprot.uuw.advanced.search.http.context.MessageConverterContextFactory;
import uk.ac.ebi.uniprot.uuw.advanced.search.http.context.UniProtMediaType;
import uk.ac.ebi.uniprot.uuw.advanced.search.model.request.SearchRequestDTO;
import uk.ac.ebi.uniprot.uuw.advanced.search.model.response.QueryResult;
import uk.ac.ebi.uniprot.uuw.advanced.search.service.UniProtEntryService;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import static org.springframework.http.HttpHeaders.*;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.MediaType.APPLICATION_XML_VALUE;
import static uk.ac.ebi.uniprot.uuw.advanced.search.controller.UniprotAdvancedSearchController.UNIPROTKB_RESOURCE;
import static uk.ac.ebi.uniprot.uuw.advanced.search.http.context.MessageConverterContextFactory.Resource.UNIPROT;
import static uk.ac.ebi.uniprot.uuw.advanced.search.http.context.UniProtMediaType.*;

/**
 * Controller for uniprot advanced search service.
 *
 * @author lgonzales
 */
@RestController
@RequestMapping(UNIPROTKB_RESOURCE)
public class UniprotAdvancedSearchController {
    static final String UNIPROTKB_RESOURCE = "/uniprotkb";
    private static final int PREVIEW_SIZE = 10;
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

    @RequestMapping(value = "/search", method = RequestMethod.GET,
            produces = {TSV_MEDIA_TYPE_VALUE, FF_MEDIA_TYPE_VALUE, LIST_MEDIA_TYPE_VALUE, APPLICATION_XML_VALUE,
                        APPLICATION_JSON_VALUE})
    public ResponseEntity<MessageConverterContext> searchCursor(@Valid SearchRequestDTO searchRequest,
                                                                @RequestParam(value = "preview", required = false, defaultValue = "false") boolean preview,
                                                                @RequestHeader("Accept") MediaType contentType,
                                                                HttpServletRequest request, HttpServletResponse response) {
        setPreviewInfo(searchRequest, preview);

        MessageConverterContext context = converterContextFactory.get(UNIPROT, contentType);
        context.setRequestDTO(searchRequest);
        QueryResult<?> result = entryService.search(searchRequest, context, contentType);

        eventPublisher.publishEvent(new PaginatedResultsEvent(this, request, response, result.getPageAndClean()));
        return new ResponseEntity<>(context, HttpStatus.OK);
    }

    @RequestMapping(value = "/accession/{accession}", method = RequestMethod.GET,
            produces = {TSV_MEDIA_TYPE_VALUE, FF_MEDIA_TYPE_VALUE, LIST_MEDIA_TYPE_VALUE, APPLICATION_XML_VALUE,
                        APPLICATION_JSON_VALUE})
    public ResponseEntity<Object> getByAccession(@PathVariable("accession") String accession,
                                                 @RequestHeader("Accept") MediaType contentType,
                                                 @RequestParam("fields") String fields) {
        MessageConverterContext context = converterContextFactory.get(UNIPROT, contentType);
        SearchRequestDTO requestDTO = new SearchRequestDTO();
        requestDTO.setFields(fields);
        context.setRequestDTO(requestDTO);
        context.setEntities(entryService.getByAccession(accession, fields, contentType));

        return new ResponseEntity<>(context, HttpStatus.OK);
    }

    /*
     * E.g., usage from command line:
     * time curl -OJ -H "Accept:text/list" "http://localhost:8090/uniprot/api/uniprotkb/download?query=reviewed:true" (i.e., show accessions)
     *   - for GZIPPED results, add -H "Accept-Encoding:gzip"
     *   - omit '-OJ' option to curl, to just see it print to standard output
     */
    @RequestMapping(value = "/download", method = RequestMethod.GET,
            produces = {TSV_MEDIA_TYPE_VALUE, FF_MEDIA_TYPE_VALUE, LIST_MEDIA_TYPE_VALUE, APPLICATION_XML_VALUE,
                        APPLICATION_JSON_VALUE, XLS_MEDIA_TYPE_VALUE, FASTA_MEDIA_TYPE_VALUE, GFF_MEDIA_TYPE_VALUE})
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

    private void setPreviewInfo(SearchRequestDTO searchRequest, boolean preview) {
        if (preview) {
            searchRequest.setSize(PREVIEW_SIZE);
        }
    }

    private HttpHeaders createHttpDownloadHeader(MediaType mediaType, MessageConverterContext context, HttpServletRequest request) {
        String suffix = "." + UniProtMediaType.getFileExtension(mediaType) + context.getFileType().getExtension();
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
