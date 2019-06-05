package uk.ac.ebi.uniprot.api.uniprotkb.controller;

import io.swagger.annotations.Api;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyEmitter;
import uk.ac.ebi.uniprot.api.common.repository.search.QueryResult;
import uk.ac.ebi.uniprot.api.rest.controller.BasicSearchController;
import uk.ac.ebi.uniprot.api.rest.output.context.FileType;
import uk.ac.ebi.uniprot.api.rest.output.context.MessageConverterContext;
import uk.ac.ebi.uniprot.api.rest.output.context.MessageConverterContextFactory;
import uk.ac.ebi.uniprot.api.rest.validation.ValidReturnFields;
import uk.ac.ebi.uniprot.api.uniprotkb.controller.request.SearchRequestDTO;
import uk.ac.ebi.uniprot.api.uniprotkb.service.UniProtEntryService;
import uk.ac.ebi.uniprot.api.uniprotkb.validation.validator.impl.UniprotReturnFieldsValidator;
import uk.ac.ebi.uniprot.domain.uniprot.UniProtEntry;
import uk.ac.ebi.uniprot.search.field.validator.FieldValueValidator;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import javax.validation.constraints.Pattern;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.MediaType.APPLICATION_XML_VALUE;
import static uk.ac.ebi.uniprot.api.rest.output.UniProtMediaType.*;
import static uk.ac.ebi.uniprot.api.rest.output.context.MessageConverterContextFactory.Resource.UNIPROT;
import static uk.ac.ebi.uniprot.api.uniprotkb.controller.UniprotKBController.UNIPROTKB_RESOURCE;

/**
 * Controller for uniprot advanced search service.
 *
 * @author lgonzales
 */
@RestController
@Api(tags = {"uniprotkb"})
@Validated
@RequestMapping(UNIPROTKB_RESOURCE)
public class UniprotKBController extends BasicSearchController<UniProtEntry> {
    static final String UNIPROTKB_RESOURCE = "/uniprotkb";
    private static final int PREVIEW_SIZE = 10;

    private final UniProtEntryService entryService;
    private final MessageConverterContextFactory<UniProtEntry> converterContextFactory;

    @Autowired
    public UniprotKBController(ApplicationEventPublisher eventPublisher,
                               UniProtEntryService entryService,
                               MessageConverterContextFactory<UniProtEntry> converterContextFactory,
                               ThreadPoolTaskExecutor downloadTaskExecutor) {
        super(eventPublisher, converterContextFactory, downloadTaskExecutor, UNIPROT);
        this.entryService = entryService;
        this.converterContextFactory = converterContextFactory;
    }

    @RequestMapping(value = "/search", method = RequestMethod.GET,
            produces = {TSV_MEDIA_TYPE_VALUE, FF_MEDIA_TYPE_VALUE, LIST_MEDIA_TYPE_VALUE, APPLICATION_XML_VALUE,
                        APPLICATION_JSON_VALUE, XLS_MEDIA_TYPE_VALUE, FASTA_MEDIA_TYPE_VALUE, GFF_MEDIA_TYPE_VALUE})
    public ResponseEntity<MessageConverterContext<UniProtEntry>> searchCursor(@Valid SearchRequestDTO searchRequest,
                                                                              @RequestParam(value = "preview", required = false, defaultValue = "false") boolean preview,
                                                                              @RequestHeader(value = "Accept", defaultValue = APPLICATION_JSON_VALUE) MediaType contentType,
                                                                              HttpServletRequest request, HttpServletResponse response) {
        setPreviewInfo(searchRequest, preview);
        QueryResult<UniProtEntry> result = entryService.search(searchRequest);
        return super.getSearchResponse(result, searchRequest.getFields(), contentType, request, response);
    }
         
    @RequestMapping(value = "/accession/{accession}", method = RequestMethod.GET,
            produces = {TSV_MEDIA_TYPE_VALUE, FF_MEDIA_TYPE_VALUE, LIST_MEDIA_TYPE_VALUE, APPLICATION_XML_VALUE,
                        APPLICATION_JSON_VALUE, XLS_MEDIA_TYPE_VALUE, FASTA_MEDIA_TYPE_VALUE, GFF_MEDIA_TYPE_VALUE})
    public ResponseEntity<MessageConverterContext<UniProtEntry>> getByAccession(@PathVariable("accession")
                                                 @Pattern(regexp = FieldValueValidator.ACCESSION_REGEX,
                                                        flags = {Pattern.Flag.CASE_INSENSITIVE},
                                                        message ="{search.invalid.accession.value}")
                                                 String accession,
                                                                                @ValidReturnFields(fieldValidatorClazz = UniprotReturnFieldsValidator.class)
                                                 @RequestParam(value = "fields", required = false)
                                                 String fields,
                                                                                @RequestHeader(value = "Accept",
                                                         defaultValue = APPLICATION_JSON_VALUE)
                                                 MediaType contentType
                                                 ) {

        UniProtEntry entry = entryService.getByAccession(accession, fields);
        return super.getEntityResponse(entry, fields, contentType);
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

        MessageConverterContext<UniProtEntry> context = converterContextFactory.get(UNIPROT, contentType);
        context.setFileType(FileType.bestFileTypeMatch(encoding));
        context.setFields(searchRequest.getFields());
        if (contentType.equals(LIST_MEDIA_TYPE)) {
            context.setEntityIds(entryService.streamIds(searchRequest));
        } else {
            context.setEntities(entryService.stream(searchRequest, contentType));
        }

        return super.getResponseBodyEmitterResponseEntity(request, context);
    }

    @Override
    protected String getEntityId(UniProtEntry entity) {
        return entity.getPrimaryAccession().getValue();
    }

    private void setPreviewInfo(SearchRequestDTO searchRequest, boolean preview) {
        if (preview) {
            searchRequest.setSize(PREVIEW_SIZE);
        }
    }
}
