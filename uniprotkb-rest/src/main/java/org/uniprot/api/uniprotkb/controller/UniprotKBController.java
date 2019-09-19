package org.uniprot.api.uniprotkb.controller;

import ebi.ac.uk.uniprot.openapi.extension.ModelFieldMeta;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyEmitter;
import org.uniprot.api.common.repository.search.QueryResult;
import org.uniprot.api.rest.controller.BasicSearchController;
import org.uniprot.api.rest.output.context.FileType;
import org.uniprot.api.rest.output.context.MessageConverterContext;
import org.uniprot.api.rest.output.context.MessageConverterContextFactory;
import org.uniprot.api.rest.validation.ValidReturnFields;
import org.uniprot.api.uniprotkb.controller.request.SearchRequestDTO;
import org.uniprot.api.uniprotkb.service.UniProtEntryService;
import org.uniprot.core.uniprot.InactiveReasonType;
import org.uniprot.core.uniprot.UniProtEntry;
import org.uniprot.core.util.Utils;
import org.uniprot.core.xml.jaxb.uniprot.Entry;
import org.uniprot.store.search.domain.impl.UniProtResultFields;
import org.uniprot.store.search.field.validator.FieldValueValidator;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import javax.validation.constraints.Pattern;
import java.util.Optional;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.MediaType.APPLICATION_XML_VALUE;
import static org.uniprot.api.rest.output.UniProtMediaType.*;
import static org.uniprot.api.rest.output.context.MessageConverterContextFactory.Resource.UNIPROT;
import static org.uniprot.api.uniprotkb.controller.UniprotKBController.UNIPROTKB_RESOURCE;

/**
 * Controller for uniprot advanced search service.
 *
 * @author lgonzales
 */
@RestController
@Validated
@RequestMapping(value = UNIPROTKB_RESOURCE)
public class UniprotKBController extends BasicSearchController<UniProtEntry> {
    static final String UNIPROTKB_RESOURCE = "/uniprotkb";
    private static final int PREVIEW_SIZE = 10;

    private final UniProtEntryService entryService;
    private final MessageConverterContextFactory<UniProtEntry> converterContextFactory;

    @Autowired
    private HttpServletRequest request;

    @Autowired
    public UniprotKBController(ApplicationEventPublisher eventPublisher,
                               UniProtEntryService entryService,
                               MessageConverterContextFactory<UniProtEntry> converterContextFactory,
                               ThreadPoolTaskExecutor downloadTaskExecutor) {
        super(eventPublisher, converterContextFactory, downloadTaskExecutor, UNIPROT);
        this.entryService = entryService;
        this.converterContextFactory = converterContextFactory;
    }


    @Tag(name = "uniprotkb", description = "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Dolor sed viverra ipsum nunc aliquet bibendum enim. In massa tempor nec feugiat. Nunc aliquet bibendum enim facilisis gravida. Nisl nunc mi ipsum faucibus vitae aliquet nec ullamcorper. Amet luctus venenatis lectus magna fringilla. Volutpat maecenas volutpat blandit aliquam etiam erat velit scelerisque in. Egestas egestas fringilla phasellus faucibus scelerisque eleifend. Sagittis orci a scelerisque purus semper eget duis. Nulla pharetra diam sit amet nisl suscipit. Sed adipiscing diam donec adipiscing tristique risus nec feugiat in. Fusce ut placerat orci nulla. Pharetra vel turpis nunc eget lorem dolor. Tristique senectus et netus et malesuada.")
    @RequestMapping(value = "/search", method = RequestMethod.GET)
    @Operation(summary = "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua", responses = {
            @ApiResponse(content = {
                    @Content(mediaType = APPLICATION_JSON_VALUE, array = @ArraySchema(schema = @Schema(implementation = UniProtEntry.class))),
                    @Content(mediaType = APPLICATION_XML_VALUE, array = @ArraySchema(schema = @Schema(implementation = Entry.class, name = "entries"))),
                    @Content(mediaType = TSV_MEDIA_TYPE_VALUE),
                    @Content(mediaType = FF_MEDIA_TYPE_VALUE),
                    @Content(mediaType = LIST_MEDIA_TYPE_VALUE),
                    @Content(mediaType = XLS_MEDIA_TYPE_VALUE),
                    @Content(mediaType = FASTA_MEDIA_TYPE_VALUE),
                    @Content(mediaType = GFF_MEDIA_TYPE_VALUE)
            }
            )
    })
    public ResponseEntity<MessageConverterContext<UniProtEntry>> searchCursor(
            @Valid @ModelAttribute SearchRequestDTO searchRequest,
            @Parameter(hidden = true) @RequestParam(value = "preview", required = false, defaultValue = "false") boolean preview,
            HttpServletRequest request, HttpServletResponse response) {

        setPreviewInfo(searchRequest, preview);
        MediaType contentType = getAcceptHeader(this.request);
        QueryResult<UniProtEntry> result = entryService.search(searchRequest);
        return super.getSearchResponse(result, searchRequest.getFields(), contentType, request, response);
    }

    @Tag(name = "uniprotkb")
    @RequestMapping(value = "/accession/{accession}", method = RequestMethod.GET)
    @Operation(summary = "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua",responses = {
            @ApiResponse(content = {
                            @Content(mediaType = APPLICATION_JSON_VALUE, schema = @Schema(implementation = UniProtEntry.class)),
                            @Content(mediaType = APPLICATION_XML_VALUE, schema = @Schema(implementation = Entry.class)),
                            @Content(mediaType = TSV_MEDIA_TYPE_VALUE),
                            @Content(mediaType = FF_MEDIA_TYPE_VALUE),
                            @Content(mediaType = LIST_MEDIA_TYPE_VALUE),
                            @Content(mediaType = XLS_MEDIA_TYPE_VALUE),
                            @Content(mediaType = FASTA_MEDIA_TYPE_VALUE),
                            @Content(mediaType = GFF_MEDIA_TYPE_VALUE)
                            }
                         )
    })
    public ResponseEntity<MessageConverterContext<UniProtEntry>> getByAccession(
            @Parameter(description = "Unique identifier for the UniProt entry")
            @PathVariable("accession")
            @Pattern(regexp = FieldValueValidator.ACCESSION_REGEX, flags = {Pattern.Flag.CASE_INSENSITIVE},
                    message = "{search.invalid.accession.value}") String accession,
            @ModelFieldMeta(path = "uniprotkb-rest/src/main/resources/uniprotkb_return_field_meta.json")
            @ValidReturnFields(fieldValidatorClazz = UniProtResultFields.class)
            @Parameter(description = "Comma separated list of fields to be returned in response")
            @RequestParam(value = "fields", required = false) String fields) {

        UniProtEntry entry = entryService.getByAccession(accession, fields);

        MediaType contentType = getAcceptHeader(this.request);

        return super.getEntityResponse(entry, fields, contentType);
    }

    /*
     * E.g., usage from command line:
     * time curl -OJ -H "Accept:text/list" "http://localhost:8090/uniprot/api/uniprotkb/download?query=reviewed:true" (i.e., show accessions)
     *   - for GZIPPED results, add -H "Accept-Encoding:gzip"
     *   - omit '-OJ' option to curl, to just see it print to standard output
     */
    @Tag(name = "uniprotkb")
    @RequestMapping(value = "/download", method = RequestMethod.GET)
    @Operation(summary = "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua", responses = {
            @ApiResponse(content = {
                    @Content(mediaType = APPLICATION_JSON_VALUE, array = @ArraySchema(schema = @Schema(implementation = UniProtEntry.class))),
                    @Content(mediaType = APPLICATION_XML_VALUE, array = @ArraySchema(schema = @Schema(implementation = Entry.class, name = "entries"))),
                    @Content(mediaType = TSV_MEDIA_TYPE_VALUE),
                    @Content(mediaType = FF_MEDIA_TYPE_VALUE),
                    @Content(mediaType = LIST_MEDIA_TYPE_VALUE),
                    @Content(mediaType = XLS_MEDIA_TYPE_VALUE),
                    @Content(mediaType = FASTA_MEDIA_TYPE_VALUE),
                    @Content(mediaType = GFF_MEDIA_TYPE_VALUE)
            }
            )
    })
    public ResponseEntity<ResponseBodyEmitter> download(
            @Valid @ModelAttribute SearchRequestDTO searchRequest,
            @RequestHeader(value = "Accept-Encoding", required = false) String encoding,
            HttpServletRequest request) {

        MediaType contentType = getAcceptHeader(this.request);
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

    @Override
    protected Optional<String> getEntityRedirectId(UniProtEntry entity) {
        if (isInactiveAndMergedEntry(entity)) {
            return Optional.of(String.valueOf(entity.getInactiveReason().getMergeDemergeTo().get(0)));
        } else {
            return Optional.empty();
        }
    }

    private boolean isInactiveAndMergedEntry(UniProtEntry uniProtEntry) {
        return !uniProtEntry.isActive() &&
                uniProtEntry.getInactiveReason() != null &&
                uniProtEntry.getInactiveReason().getInactiveReasonType().equals(InactiveReasonType.MERGED) &&
                Utils.notEmpty(uniProtEntry.getInactiveReason().getMergeDemergeTo());
    }

    private void setPreviewInfo(SearchRequestDTO searchRequest, boolean preview) {
        if (preview) {
            searchRequest.setSize(PREVIEW_SIZE);
        }
    }
}
