package uk.ac.ebi.uniprot.uniprotkb.controller;

import io.swagger.annotations.Api;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyEmitter;
import uk.ac.ebi.uniprot.common.repository.search.QueryResult;
import uk.ac.ebi.uniprot.domain.uniprot.UniProtEntry;
import uk.ac.ebi.uniprot.rest.output.context.FileType;
import uk.ac.ebi.uniprot.rest.output.context.MessageConverterContext;
import uk.ac.ebi.uniprot.rest.output.context.MessageConverterContextFactory;
import uk.ac.ebi.uniprot.rest.pagination.PaginatedResultsEvent;
import uk.ac.ebi.uniprot.uniprotkb.controller.request.SearchRequestDTO;
import uk.ac.ebi.uniprot.uniprotkb.service.UniProtEntryService;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.MediaType.APPLICATION_XML_VALUE;
import static uk.ac.ebi.uniprot.rest.output.UniProtMediaType.*;
import static uk.ac.ebi.uniprot.rest.output.context.MessageConverterContextFactory.Resource.UNIPROT;
import static uk.ac.ebi.uniprot.rest.output.header.HeaderFactory.createHttpDownloadHeader;
import static uk.ac.ebi.uniprot.rest.output.header.HeaderFactory.createHttpSearchHeader;
import static uk.ac.ebi.uniprot.uniprotkb.controller.UniprotKBController.UNIPROTKB_RESOURCE;

/**
 * Controller for uniprot advanced search service.
 *
 * @author lgonzales
 */
@RestController
@Api(tags = {"uniprotkb"})
@RequestMapping(UNIPROTKB_RESOURCE)
public class UniprotKBController {
    static final String UNIPROTKB_RESOURCE = "/uniprotkb";
    private static final int PREVIEW_SIZE = 10;
    private final ApplicationEventPublisher eventPublisher;

    private final UniProtEntryService entryService;
    private final MessageConverterContextFactory<UniProtEntry> converterContextFactory;

    @Autowired
    public UniprotKBController(ApplicationEventPublisher eventPublisher,
                               UniProtEntryService entryService,
                               MessageConverterContextFactory<UniProtEntry> converterContextFactory) {
        this.eventPublisher = eventPublisher;
        this.entryService = entryService;
        this.converterContextFactory = converterContextFactory;
    }

    @RequestMapping(value = "/search", method = RequestMethod.GET,
            produces = {TSV_MEDIA_TYPE_VALUE, FF_MEDIA_TYPE_VALUE, LIST_MEDIA_TYPE_VALUE, APPLICATION_XML_VALUE,
                        APPLICATION_JSON_VALUE, XLS_MEDIA_TYPE_VALUE, FASTA_MEDIA_TYPE_VALUE, GFF_MEDIA_TYPE_VALUE})
    public ResponseEntity<MessageConverterContext> searchCursor(@Valid SearchRequestDTO searchRequest,
                                                                @RequestParam(value = "preview", required = false, defaultValue = "false") boolean preview,
                                                                @RequestHeader(value = "Accept", defaultValue = APPLICATION_JSON_VALUE) MediaType contentType,
                                                                HttpServletRequest request, HttpServletResponse response) {
        setPreviewInfo(searchRequest, preview);

        MessageConverterContext<UniProtEntry> context = converterContextFactory.get(UNIPROT, contentType);
        context.setFields(searchRequest.getFields());
        QueryResult<?> result = entryService.search(searchRequest, context);

        eventPublisher.publishEvent(new PaginatedResultsEvent(this, request, response, result.getPageAndClean()));
        return ResponseEntity.ok()
                .headers(createHttpSearchHeader(contentType))
                .body(context);
    }
         
    @RequestMapping(value = "/accession/{accession}", method = RequestMethod.GET,
            produces = {TSV_MEDIA_TYPE_VALUE, FF_MEDIA_TYPE_VALUE, LIST_MEDIA_TYPE_VALUE, APPLICATION_XML_VALUE,
                        APPLICATION_JSON_VALUE, XLS_MEDIA_TYPE_VALUE, FASTA_MEDIA_TYPE_VALUE, GFF_MEDIA_TYPE_VALUE})
    public ResponseEntity<Object> getByAccession(@PathVariable("accession") String accession,
                                                 @RequestHeader(value = "Accept", defaultValue = APPLICATION_JSON_VALUE) MediaType contentType,
                                                 @RequestParam(value = "fields", required = false) String fields) {
        MessageConverterContext<UniProtEntry> context = converterContextFactory.get(UNIPROT, contentType);
        context.setFields(fields);
        entryService.getByAccession(accession, fields, context);

        return ResponseEntity.ok()
                .headers(createHttpSearchHeader(contentType))
                .body(context);
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
            @RequestHeader(value = "Accept", defaultValue = FF_MEDIA_TYPE_VALUE) MediaType contentType,
            @RequestHeader(value = "Accept-Encoding", required = false) String encoding,
            HttpServletRequest request) {
        ResponseBodyEmitter emitter = new ResponseBodyEmitter();

        MessageConverterContext<UniProtEntry> context = converterContextFactory.get(UNIPROT, contentType);
        context.setFileType(FileType.bestFileTypeMatch(encoding));
        context.setFields(searchRequest.getFields());

        entryService.stream(searchRequest, context, emitter);

        return ResponseEntity.ok()
                .headers(createHttpDownloadHeader(context, request))
                .body(emitter);
    }

    private void setPreviewInfo(SearchRequestDTO searchRequest, boolean preview) {
        if (preview) {
            searchRequest.setSize(PREVIEW_SIZE);
        }
    }
}
