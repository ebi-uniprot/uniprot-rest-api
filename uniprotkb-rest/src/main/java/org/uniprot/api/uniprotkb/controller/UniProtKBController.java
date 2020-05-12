package org.uniprot.api.uniprotkb.controller;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.MediaType.APPLICATION_XML_VALUE;
import static org.uniprot.api.rest.output.UniProtMediaType.*;
import static org.uniprot.api.rest.output.context.MessageConverterContextFactory.Resource.UNIPROTKB;
import static org.uniprot.api.uniprotkb.controller.UniProtKBController.UNIPROTKB_RESOURCE;

import java.util.Optional;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import javax.validation.constraints.Pattern;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.async.DeferredResult;
import org.uniprot.api.common.repository.search.QueryResult;
import org.uniprot.api.rest.controller.BasicSearchController;
import org.uniprot.api.rest.output.context.FileType;
import org.uniprot.api.rest.output.context.MessageConverterContext;
import org.uniprot.api.rest.output.context.MessageConverterContextFactory;
import org.uniprot.api.rest.validation.ValidReturnFields;
import org.uniprot.api.uniprotkb.controller.request.ReturnFieldMetaReaderImpl;
import org.uniprot.api.uniprotkb.controller.request.UniProtKBRequest;
import org.uniprot.api.uniprotkb.service.UniProtEntryService;
import org.uniprot.core.uniprotkb.InactiveReasonType;
import org.uniprot.core.uniprotkb.UniProtKBEntry;
import org.uniprot.core.util.Utils;
import org.uniprot.core.xml.jaxb.uniprot.Entry;
import org.uniprot.store.config.UniProtDataType;
import org.uniprot.store.search.field.validator.FieldRegexConstants;

import uk.ac.ebi.uniprot.openapi.extension.ModelFieldMeta;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * Controller for uniprot advanced search service.
 *
 * @author lgonzales
 */
@RestController
@Validated
@RequestMapping(value = UNIPROTKB_RESOURCE)
public class UniProtKBController extends BasicSearchController<UniProtKBEntry> {
    static final String UNIPROTKB_RESOURCE = "/uniprotkb";
    private static final int PREVIEW_SIZE = 10;

    private final UniProtEntryService entryService;
    private final MessageConverterContextFactory<UniProtKBEntry> converterContextFactory;

    @Autowired
    public UniProtKBController(
            ApplicationEventPublisher eventPublisher,
            UniProtEntryService entryService,
            MessageConverterContextFactory<UniProtKBEntry> converterContextFactory,
            ThreadPoolTaskExecutor downloadTaskExecutor) {
        super(eventPublisher, converterContextFactory, downloadTaskExecutor, UNIPROTKB);
        this.entryService = entryService;
        this.converterContextFactory = converterContextFactory;
    }

    @Tag(
            name = "uniprotkb",
            description =
                    "The UniProt Knowledgebase (UniProtKB) is the central hub for the collection of functional information on proteins, with accurate, consistent and rich annotation. In addition to capturing the core data mandatory for each UniProtKB entry (mainly, the amino acid sequence, protein name or description, taxonomic data and citation information), as much annotation information as possible is added. This includes widely accepted biological ontologies, classifications and cross-references, and clear indications of the quality of annotation in the form of evidence attribution of experimental and computational data. The UniProt Knowledgebase consists of two sections: \"UniProtKB/Swiss-Prot\" (reviewed, manually annotated) and \"UniProtKB/TrEMBL\" (unreviewed, automatically annotated), respectively.")
    @RequestMapping(
            value = "/search",
            method = RequestMethod.GET,
            produces = {
                TSV_MEDIA_TYPE_VALUE,
                FF_MEDIA_TYPE_VALUE,
                LIST_MEDIA_TYPE_VALUE,
                APPLICATION_XML_VALUE,
                APPLICATION_JSON_VALUE,
                XLS_MEDIA_TYPE_VALUE,
                FASTA_MEDIA_TYPE_VALUE,
                GFF_MEDIA_TYPE_VALUE
            })
    @Operation(
            summary = "Search for a UniProtKB protein entry (or entries) by a SOLR query.",
            responses = {
                @ApiResponse(
                        content = {
                            @Content(
                                    mediaType = APPLICATION_JSON_VALUE,
                                    array =
                                            @ArraySchema(
                                                    schema =
                                                            @Schema(
                                                                    implementation =
                                                                            UniProtKBEntry.class))),
                            @Content(
                                    mediaType = APPLICATION_XML_VALUE,
                                    array =
                                            @ArraySchema(
                                                    schema =
                                                            @Schema(
                                                                    implementation = Entry.class,
                                                                    name = "entries"))),
                            @Content(mediaType = TSV_MEDIA_TYPE_VALUE),
                            @Content(mediaType = FF_MEDIA_TYPE_VALUE),
                            @Content(mediaType = LIST_MEDIA_TYPE_VALUE),
                            @Content(mediaType = XLS_MEDIA_TYPE_VALUE),
                            @Content(mediaType = FASTA_MEDIA_TYPE_VALUE),
                            @Content(mediaType = GFF_MEDIA_TYPE_VALUE)
                        })
            })
    public ResponseEntity<MessageConverterContext<UniProtKBEntry>> searchCursor(
            @Valid @ModelAttribute UniProtKBRequest searchRequest,
            @Parameter(hidden = true)
                    @RequestParam(value = "preview", required = false, defaultValue = "false")
                    boolean preview,
            HttpServletRequest request,
            HttpServletResponse response) {
        setPreviewInfo(searchRequest, preview);
        QueryResult<UniProtKBEntry> result = entryService.search(searchRequest);
        return super.getSearchResponse(result, searchRequest.getFields(), request, response);
    }

    @Tag(name = "uniprotkb")
    @RequestMapping(
            value = "/accession/{accession}",
            method = RequestMethod.GET,
            produces = {
                TSV_MEDIA_TYPE_VALUE,
                FF_MEDIA_TYPE_VALUE,
                LIST_MEDIA_TYPE_VALUE,
                APPLICATION_XML_VALUE,
                APPLICATION_JSON_VALUE,
                XLS_MEDIA_TYPE_VALUE,
                FASTA_MEDIA_TYPE_VALUE,
                GFF_MEDIA_TYPE_VALUE
            })
    @Operation(
            summary = "Search for a UniProtKB protein entry by accession.",
            responses = {
                @ApiResponse(
                        content = {
                            @Content(
                                    mediaType = APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = UniProtKBEntry.class)),
                            @Content(
                                    mediaType = APPLICATION_XML_VALUE,
                                    schema = @Schema(implementation = Entry.class)),
                            @Content(mediaType = TSV_MEDIA_TYPE_VALUE),
                            @Content(mediaType = FF_MEDIA_TYPE_VALUE),
                            @Content(mediaType = LIST_MEDIA_TYPE_VALUE),
                            @Content(mediaType = XLS_MEDIA_TYPE_VALUE),
                            @Content(mediaType = FASTA_MEDIA_TYPE_VALUE),
                            @Content(mediaType = GFF_MEDIA_TYPE_VALUE)
                        })
            })
    public ResponseEntity<MessageConverterContext<UniProtKBEntry>> getByAccession(
            @Parameter(description = "Unique identifier for the UniProt entry")
                    @PathVariable("accession")
                    @Pattern(
                            regexp = FieldRegexConstants.UNIPROTKB_ACCESSION_REGEX,
                            flags = {Pattern.Flag.CASE_INSENSITIVE},
                            message = "{search.invalid.accession.value}")
                    String accession,
            @ModelFieldMeta(
                            reader = ReturnFieldMetaReaderImpl.class,
                            path = "uniprotkb-return-fields.json")
                    @ValidReturnFields(uniProtDataType = UniProtDataType.UNIPROTKB)
                    @Parameter(
                            description =
                                    "Comma separated list of fields to be returned in response")
                    @RequestParam(value = "fields", required = false)
                    String fields,
            HttpServletRequest request) {
        UniProtKBEntry entry = entryService.findByUniqueId(accession, fields);
        return super.getEntityResponse(entry, fields, request);
    }

    /*
     * E.g., usage from command line:
     * time curl -OJ -H "Accept:text/list" "http://localhost:8090/uniprot/api/uniprotkb/download?query=reviewed:true" (i.e., show accessions)
     *   - for GZIPPED results, add -H "Accept-Encoding:gzip"
     *   - omit '-OJ' option to curl, to just see it print to standard output
     */
    @Tag(name = "uniprotkb")
    @RequestMapping(
            value = "/download",
            method = RequestMethod.GET,
            produces = {
                TSV_MEDIA_TYPE_VALUE,
                FF_MEDIA_TYPE_VALUE,
                LIST_MEDIA_TYPE_VALUE,
                APPLICATION_XML_VALUE,
                APPLICATION_JSON_VALUE,
                XLS_MEDIA_TYPE_VALUE,
                FASTA_MEDIA_TYPE_VALUE,
                GFF_MEDIA_TYPE_VALUE,
                RDF_MEDIA_TYPE_VALUE
            })
    @Operation(
            summary = "Download a UniProtKB protein entry (or entries) retrieved by a SOLR query.",
            responses = {
                @ApiResponse(
                        content = {
                            @Content(
                                    mediaType = APPLICATION_JSON_VALUE,
                                    array =
                                            @ArraySchema(
                                                    schema =
                                                            @Schema(
                                                                    implementation =
                                                                            UniProtKBEntry.class))),
                            @Content(
                                    mediaType = APPLICATION_XML_VALUE,
                                    array =
                                            @ArraySchema(
                                                    schema =
                                                            @Schema(
                                                                    implementation = Entry.class,
                                                                    name = "entries"))),
                            @Content(mediaType = TSV_MEDIA_TYPE_VALUE),
                            @Content(mediaType = FF_MEDIA_TYPE_VALUE),
                            @Content(mediaType = LIST_MEDIA_TYPE_VALUE),
                            @Content(mediaType = XLS_MEDIA_TYPE_VALUE),
                            @Content(mediaType = FASTA_MEDIA_TYPE_VALUE),
                            @Content(mediaType = GFF_MEDIA_TYPE_VALUE)
                        })
            })
    public DeferredResult<ResponseEntity<MessageConverterContext<UniProtKBEntry>>> download(
            @Valid @ModelAttribute UniProtKBRequest searchRequest,
            @RequestHeader(value = "Accept-Encoding", required = false) String encoding,
            HttpServletRequest request) {

        MediaType contentType = getAcceptHeader(request);
        MessageConverterContext<UniProtKBEntry> context =
                converterContextFactory.get(UNIPROTKB, contentType);
        context.setFileType(FileType.bestFileTypeMatch(encoding));
        context.setFields(searchRequest.getFields());
        if (contentType.equals(LIST_MEDIA_TYPE)) {
            context.setEntityIds(entryService.streamIds(searchRequest));
        } else if (contentType.equals(RDF_MEDIA_TYPE)) {
            context.setEntityIds(entryService.streamRDF(searchRequest));
        } else {
            context.setEntities(entryService.stream(searchRequest));
        }

        return super.getDeferredResultResponseEntity(request, context);
    }

    @Override
    protected String getEntityId(UniProtKBEntry entity) {
        return entity.getPrimaryAccession().getValue();
    }

    @Override
    protected Optional<String> getEntityRedirectId(UniProtKBEntry entity) {
        if (isInactiveAndMergedEntry(entity)) {
            return Optional.of(
                    String.valueOf(entity.getInactiveReason().getMergeDemergeTos().get(0)));
        } else {
            return Optional.empty();
        }
    }

    private boolean isInactiveAndMergedEntry(UniProtKBEntry uniProtkbEntry) {
        return !uniProtkbEntry.isActive()
                && uniProtkbEntry.getInactiveReason() != null
                && uniProtkbEntry
                        .getInactiveReason()
                        .getInactiveReasonType()
                        .equals(InactiveReasonType.MERGED)
                && Utils.notNullNotEmpty(uniProtkbEntry.getInactiveReason().getMergeDemergeTos());
    }

    private void setPreviewInfo(UniProtKBRequest searchRequest, boolean preview) {
        if (preview) {
            searchRequest.setSize(PREVIEW_SIZE);
        }
    }
}
