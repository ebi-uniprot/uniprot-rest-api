package org.uniprot.api.uniprotkb.controller;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.MediaType.APPLICATION_XML_VALUE;
import static org.uniprot.api.rest.output.UniProtMediaType.*;
import static org.uniprot.api.rest.output.context.MessageConverterContextFactory.Resource.UNIPROTKB;
import static org.uniprot.api.rest.output.header.HeaderFactory.createHttpSearchHeader;
import static org.uniprot.api.uniprotkb.controller.UniProtKBController.UNIPROTKB_RESOURCE;

import java.util.Optional;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;

import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.*;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.async.DeferredResult;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import org.uniprot.api.common.concurrency.Gatekeeper;
import org.uniprot.api.common.repository.search.QueryResult;
import org.uniprot.api.rest.controller.BasicSearchController;
import org.uniprot.api.rest.output.UniProtMediaType;
import org.uniprot.api.rest.output.context.MessageConverterContext;
import org.uniprot.api.rest.output.context.MessageConverterContextFactory;
import org.uniprot.api.rest.request.IdsSearchRequest;
import org.uniprot.api.rest.request.ReturnFieldMetaReaderImpl;
import org.uniprot.api.rest.validation.ValidContentTypes;
import org.uniprot.api.rest.validation.ValidReturnFields;
import org.uniprot.api.uniprotkb.controller.request.UniProtKBIdsPostRequest;
import org.uniprot.api.uniprotkb.controller.request.UniProtKBIdsSearchRequest;
import org.uniprot.api.uniprotkb.controller.request.UniProtKBSearchRequest;
import org.uniprot.api.uniprotkb.controller.request.UniProtKBStreamRequest;
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
    private static final String DATA_TYPE = "uniprotkb";
    static final String UNIPROTKB_RESOURCE = "/uniprotkb";
    private static final int PREVIEW_SIZE = 10;
    private static final java.util.regex.Pattern ACCESSION_REGEX_PATTERN =
            java.util.regex.Pattern.compile(FieldRegexConstants.UNIPROTKB_ACCESSION_REGEX);

    private final UniProtEntryService entryService;

    @Autowired
    public UniProtKBController(
            ApplicationEventPublisher eventPublisher,
            UniProtEntryService entryService,
            MessageConverterContextFactory<UniProtKBEntry> converterContextFactory,
            ThreadPoolTaskExecutor downloadTaskExecutor,
            Gatekeeper downloadGatekeeper) {
        super(
                eventPublisher,
                converterContextFactory,
                downloadTaskExecutor,
                UNIPROTKB,
                downloadGatekeeper);
        this.entryService = entryService;
    }

    @Tag(
            name = "uniprotkb",
            description =
                    "The UniProt Knowledgebase (UniProtKB) is the central hub for the collection of functional information on proteins, with accurate, consistent and rich annotation. In addition to capturing the core data mandatory for each UniProtKB entry (mainly, the amino acid sequence, protein name or description, taxonomic data and citation information), as much annotation information as possible is added. This includes widely accepted biological ontologies, classifications and cross-references, and clear indications of the quality of annotation in the form of evidence attribution of experimental and computational data. The UniProt Knowledgebase consists of two sections: \"UniProtKB/Swiss-Prot\" (reviewed, manually annotated) and \"UniProtKB/TrEMBL\" (unreviewed, automatically annotated), respectively.")
    @GetMapping(
            value = "/search",
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
            @Valid @ModelAttribute UniProtKBSearchRequest searchRequest,
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
    @GetMapping(
            value = "/{accession}",
            produces = {
                TSV_MEDIA_TYPE_VALUE,
                FF_MEDIA_TYPE_VALUE,
                LIST_MEDIA_TYPE_VALUE,
                APPLICATION_XML_VALUE,
                APPLICATION_JSON_VALUE,
                XLS_MEDIA_TYPE_VALUE,
                FASTA_MEDIA_TYPE_VALUE,
                GFF_MEDIA_TYPE_VALUE,
                RDF_MEDIA_TYPE_VALUE,
                TURTLE_MEDIA_TYPE_VALUE,
                N_TRIPLES_MEDIA_TYPE_VALUE
            })
    @Operation(
            summary = "Get UniProtKB entry by an accession.",
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
                            @Content(mediaType = GFF_MEDIA_TYPE_VALUE),
                            @Content(mediaType = RDF_MEDIA_TYPE_VALUE),
                            @Content(mediaType = TURTLE_MEDIA_TYPE_VALUE),
                            @Content(mediaType = N_TRIPLES_MEDIA_TYPE_VALUE)
                        })
            })
    public ResponseEntity<MessageConverterContext<UniProtKBEntry>> getByAccession(
            @Parameter(description = "Unique identifier for the UniProt entry")
                    @PathVariable("accession")
                    @Pattern(
                            regexp = FieldRegexConstants.UNIPROTKB_ACCESSION_OR_ID,
                            flags = {Pattern.Flag.CASE_INSENSITIVE},
                            message = "{search.invalid.accession.value}")
                    String accessionOrId,
            @ModelFieldMeta(
                            reader = ReturnFieldMetaReaderImpl.class,
                            path = "uniprotkb-return-fields.json")
                    @ValidReturnFields(uniProtDataType = UniProtDataType.UNIPROTKB)
                    @Parameter(
                            description =
                                    "Comma separated list of fields to be returned in response")
                    @RequestParam(value = "fields", required = false)
                    String fields,
            @Parameter(description = "Entry version")
                    @RequestParam(value = "version", required = false)
                    @ValidContentTypes(contentTypes = {FASTA_MEDIA_TYPE_VALUE, FF_MEDIA_TYPE_VALUE})
                    String version,
            HttpServletRequest request) {
        if (Utils.notNullNotEmpty(version)
                && ACCESSION_REGEX_PATTERN.matcher(accessionOrId).matches()) {
            return redirectToEntryVersion(accessionOrId, request);
        }
        if (accessionOrId.contains("_")) {
            String accession = entryService.findAccessionByProteinId(accessionOrId);
            return redirectToAccession(accessionOrId, accession, request);
        } else if (accessionOrId.contains(".")) {
            return redirectToUniSave(accessionOrId, request, Optional.ofNullable(null));
        } else {
            Optional<String> acceptedRdfContentType = getAcceptedRdfContentType(request);
            if (acceptedRdfContentType.isPresent()) {
                String rdf =
                        entryService.getRdf(accessionOrId, DATA_TYPE, acceptedRdfContentType.get());
                return super.getEntityResponseRdf(rdf, getAcceptHeader(request), request);
            } else {
                UniProtKBEntry entry = entryService.findByUniqueId(accessionOrId, fields);
                return super.getEntityResponse(entry, fields, request);
            }
        }
    }

    private ResponseEntity<MessageConverterContext<UniProtKBEntry>> redirectToEntryVersion(
            String accessionOrId, HttpServletRequest request) {
        String version = null;
        if (request.getParameter("version").equals("last")) {
            try {
                String entryVersionHistoryURI = "https://rest.uniprot.org/unisave/" + accessionOrId;
                String response = Utils.httpGetRequest(entryVersionHistoryURI);
                JSONObject jsonObject = new JSONObject(response);
                version =
                        jsonObject
                                .getJSONArray("results")
                                .getJSONObject(0)
                                .get("entryVersion")
                                .toString();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        } else {
            version = request.getParameter("version");
        }

        return redirectToUniSave(accessionOrId, request, Optional.of(version));
    }

    /*
     * E.g., usage from command line:
     * time curl -OJ -H "Accept:text/list" "http://localhost:8090/uniprot/api/uniprotkb/download?query=reviewed:true" (i.e., show accessions)
     *   - for GZIPPED results, add -H "Accept-Encoding:gzip"
     *   - omit '-OJ' option to curl, to just see it print to standard output
     */
    @Tag(name = "uniprotkb")
    @GetMapping(
            value = "/stream",
            produces = {
                TSV_MEDIA_TYPE_VALUE,
                FF_MEDIA_TYPE_VALUE,
                LIST_MEDIA_TYPE_VALUE,
                APPLICATION_XML_VALUE,
                APPLICATION_JSON_VALUE,
                XLS_MEDIA_TYPE_VALUE,
                FASTA_MEDIA_TYPE_VALUE,
                GFF_MEDIA_TYPE_VALUE,
                RDF_MEDIA_TYPE_VALUE,
                TURTLE_MEDIA_TYPE_VALUE,
                N_TRIPLES_MEDIA_TYPE_VALUE
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
    public DeferredResult<ResponseEntity<MessageConverterContext<UniProtKBEntry>>> stream(
            @Valid @ModelAttribute UniProtKBStreamRequest streamRequest,
            @RequestHeader(value = "Accept-Encoding", required = false) String encoding,
            HttpServletRequest request) {

        MediaType contentType = getAcceptHeader(request);

        Optional<String> acceptedRdfContentType = getAcceptedRdfContentType(request);
        if (acceptedRdfContentType.isPresent()) {
            return super.streamRdf(
                    () ->
                            entryService.streamRdf(
                                    streamRequest, DATA_TYPE, acceptedRdfContentType.get()),
                    streamRequest,
                    contentType,
                    request);
        } else {
            return super.stream(
                    () -> entryService.stream(streamRequest), streamRequest, contentType, request);
        }
    }

    @Tag(name = "uniprotkb")
    @RequestMapping(
            value = "/accessions",
            method = {RequestMethod.GET},
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
            summary = "Get UniProtKB entries by a list of accessions.",
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
    public ResponseEntity<MessageConverterContext<UniProtKBEntry>> getByAccessionsGet(
            @Valid @ModelAttribute UniProtKBIdsSearchRequest accessionsRequest,
            HttpServletRequest request,
            HttpServletResponse response) {
        return getByAccessions(accessionsRequest, request, response);
    }

    @Tag(name = "uniprotkb")
    @RequestMapping(
            value = "/accessions",
            method = {RequestMethod.POST},
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
            summary = "Get UniProtKB entries by a list of accessions.",
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
    public ResponseEntity<MessageConverterContext<UniProtKBEntry>> getByAccessionsPost(
            @Valid @NotNull(message = "{download.required}") @RequestBody(required = false)
                    UniProtKBIdsPostRequest accessionsRequest,
            HttpServletRequest request,
            HttpServletResponse response) {
        return getByAccessions(accessionsRequest, request, response);
    }

    private ResponseEntity<MessageConverterContext<UniProtKBEntry>> getByAccessions(
            IdsSearchRequest accessionsRequest,
            HttpServletRequest request,
            HttpServletResponse response) {
        QueryResult<UniProtKBEntry> result = entryService.getByIds(accessionsRequest);
        return super.getSearchResponse(
                result,
                accessionsRequest.getFields(),
                accessionsRequest.isDownload(),
                request,
                response);
    }

    @Override
    protected String getEntityId(UniProtKBEntry entity) {
        return entity.getPrimaryAccession().getValue();
    }

    @Override
    protected Optional<String> getEntityRedirectId(
            UniProtKBEntry entity, HttpServletRequest request) {
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

    private void setPreviewInfo(UniProtKBSearchRequest searchRequest, boolean preview) {
        if (preview) {
            searchRequest.setSize(PREVIEW_SIZE);
        }
    }

    private ResponseEntity<MessageConverterContext<UniProtKBEntry>> redirectToUniSave(
            String accessionOrId, HttpServletRequest request, Optional<String> entryVersion) {
        MediaType contentType = getAcceptHeader(request);

        String uniProtPath = ServletUriComponentsBuilder.fromCurrentRequest().build().getPath();
        String uniSavePath = "";
        String version = "";
        String accession = "";
        if (uniProtPath != null) {
            if (entryVersion.isPresent()) {
                version = entryVersion.get();
                accession = accessionOrId;
            } else {
                version = accessionOrId.substring(accessionOrId.indexOf('.') + 1);
                accession = accessionOrId.substring(0, accessionOrId.indexOf('.'));
            }
            String format = UniProtMediaType.getFileExtension(contentType);
            uniSavePath = uniProtPath.replace("uniprotkb", "unisave");
            uniSavePath = uniSavePath.substring(0, uniSavePath.lastIndexOf('/') + 1) + accession;
            uniSavePath =
                    uniSavePath
                            + "?from="
                            + accessionOrId
                            + "&versions="
                            + version
                            + "&format="
                            + format;
        }

        ResponseEntity.BodyBuilder responseBuilder =
                ResponseEntity.status(HttpStatus.SEE_OTHER)
                        .header(HttpHeaders.LOCATION, uniSavePath);
        return responseBuilder.headers(createHttpSearchHeader(contentType)).build();
    }

    private ResponseEntity<MessageConverterContext<UniProtKBEntry>> redirectToAccession(
            String accessionOrId, String accession, HttpServletRequest request) {
        MediaType contentType = getAcceptHeader(request);
        ResponseEntity.BodyBuilder responseBuilder =
                ResponseEntity.status(HttpStatus.SEE_OTHER)
                        .header(
                                HttpHeaders.LOCATION,
                                getLocationURLForId(accession, accessionOrId, contentType));
        return responseBuilder.headers(createHttpSearchHeader(contentType)).build();
    }
}
