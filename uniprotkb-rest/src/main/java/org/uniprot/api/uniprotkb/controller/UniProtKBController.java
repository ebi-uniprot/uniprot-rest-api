package org.uniprot.api.uniprotkb.controller;

import static org.uniprot.api.rest.openapi.OpenAPIConstants.*;
import static org.uniprot.api.rest.output.UniProtMediaType.*;
import static org.uniprot.api.uniprotkb.controller.UniProtKBController.UNIPROTKB_RESOURCE;
import static org.uniprot.store.search.field.validator.FieldRegexConstants.UNIPROTKB_ACCESSION_SEQUENCE_RANGE_REGEX;

import java.util.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.async.DeferredResult;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import org.uniprot.api.common.concurrency.Gatekeeper;
import org.uniprot.api.common.exception.InvalidRequestException;
import org.uniprot.api.common.repository.search.QueryResult;
import org.uniprot.api.rest.controller.BasicSearchController;
import org.uniprot.api.rest.output.context.MessageConverterContext;
import org.uniprot.api.rest.output.context.MessageConverterContextFactory;
import org.uniprot.api.rest.output.header.HeaderFactory;
import org.uniprot.api.rest.request.IdsSearchRequest;
import org.uniprot.api.rest.validation.ValidContentTypes;
import org.uniprot.api.rest.validation.ValidReturnFields;
import org.uniprot.api.uniprotkb.common.service.uniprotkb.UniProtEntryService;
import org.uniprot.api.uniprotkb.common.service.uniprotkb.UniProtKBEntryVersionService;
import org.uniprot.api.uniprotkb.common.service.uniprotkb.request.UniProtKBIdsPostRequest;
import org.uniprot.api.uniprotkb.common.service.uniprotkb.request.UniProtKBIdsSearchRequest;
import org.uniprot.api.uniprotkb.common.service.uniprotkb.request.UniProtKBSearchRequest;
import org.uniprot.api.uniprotkb.common.service.uniprotkb.request.UniProtKBStreamRequest;
import org.uniprot.core.uniprotkb.InactiveReasonType;
import org.uniprot.core.uniprotkb.UniProtKBEntry;
import org.uniprot.core.util.Pair;
import org.uniprot.core.util.PairImpl;
import org.uniprot.core.util.Utils;
import org.uniprot.core.xml.jaxb.uniprot.Entry;
import org.uniprot.store.config.UniProtDataType;
import org.uniprot.store.search.field.validator.FieldRegexConstants;

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
@Tag(name = TAG_UNIPROTKB, description = TAG_UNIPROTKB_DESC)
public class UniProtKBController extends BasicSearchController<UniProtKBEntry> {
    private static final String DATA_TYPE = "uniprotkb";
    static final String UNIPROTKB_RESOURCE = "/uniprotkb";
    private static final int PREVIEW_SIZE = 10;
    private static final java.util.regex.Pattern ACCESSION_REGEX_PATTERN =
            java.util.regex.Pattern.compile(FieldRegexConstants.UNIPROTKB_ACCESSION_REGEX);

    private final UniProtEntryService entryService;

    @Autowired UniProtKBEntryVersionService uniProtKBEntryVersionService;

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
                MessageConverterContextFactory.Resource.UNIPROTKB,
                downloadGatekeeper);
        this.entryService = entryService;
    }

    @GetMapping(
            value = "/search",
            produces = {
                TSV_MEDIA_TYPE_VALUE,
                FF_MEDIA_TYPE_VALUE,
                LIST_MEDIA_TYPE_VALUE,
                MediaType.APPLICATION_XML_VALUE,
                MediaType.APPLICATION_JSON_VALUE,
                XLS_MEDIA_TYPE_VALUE,
                FASTA_MEDIA_TYPE_VALUE,
                GFF_MEDIA_TYPE_VALUE
            })
    @Operation(
            summary = SEARCH_UNIPROTKB_OPERATION,
            description = SEARCH_OPERATION_DESC,
            responses = {
                @ApiResponse(
                        content = {
                            @Content(
                                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    array =
                                            @ArraySchema(
                                                    schema =
                                                            @Schema(
                                                                    implementation =
                                                                            UniProtKBEntry.class))),
                            @Content(
                                    mediaType = MediaType.APPLICATION_XML_VALUE,
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
        setBasicRequestFormat(searchRequest, request);
        QueryResult<UniProtKBEntry> result = entryService.search(searchRequest);
        return super.getSearchResponse(result, searchRequest.getFields(), request, response);
    }

    @GetMapping(
            value = "/{accession}",
            produces = {
                TSV_MEDIA_TYPE_VALUE,
                FF_MEDIA_TYPE_VALUE,
                LIST_MEDIA_TYPE_VALUE,
                MediaType.APPLICATION_XML_VALUE,
                MediaType.APPLICATION_JSON_VALUE,
                XLS_MEDIA_TYPE_VALUE,
                FASTA_MEDIA_TYPE_VALUE,
                GFF_MEDIA_TYPE_VALUE,
                RDF_MEDIA_TYPE_VALUE,
                TURTLE_MEDIA_TYPE_VALUE,
                N_TRIPLES_MEDIA_TYPE_VALUE
            })
    @Operation(
            summary = ID_UNIPROTKB_OPERATION,
            description = ID_UNIPROTKB_OPERATION_DESC,
            responses = {
                @ApiResponse(
                        content = {
                            @Content(
                                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = UniProtKBEntry.class)),
                            @Content(
                                    mediaType = MediaType.APPLICATION_XML_VALUE,
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
            @Parameter(
                            description = ACCESSION_UNIPROTKB_DESCRIPTION,
                            example = ACCESSION_UNIPROTKB_EXAMPLE)
                    @PathVariable("accession")
                    @Pattern(
                            regexp = FieldRegexConstants.UNIPROTKB_ACCESSION_OR_ID,
                            flags = {Pattern.Flag.CASE_INSENSITIVE},
                            message = "{search.invalid.accession.value}")
                    String accessionOrId,
            @ValidReturnFields(uniProtDataType = UniProtDataType.UNIPROTKB)
                    @Parameter(
                            description = FIELDS_UNIPROTKB_DESCRIPTION,
                            example = FIELDS_UNIPROTKB_EXAMPLE)
                    @RequestParam(value = "fields", required = false)
                    String fields,
            @Parameter(description = VERSION_UNIPROTKB_DESCRIPTION)
                    @RequestParam(value = "version", required = false)
                    @ValidContentTypes(contentTypes = {FASTA_MEDIA_TYPE_VALUE, FF_MEDIA_TYPE_VALUE})
                    String version,
            HttpServletRequest request) {
        if (Utils.notNullNotEmpty(version)
                && ACCESSION_REGEX_PATTERN.matcher(accessionOrId).matches()) {
            String entryVersion =
                    uniProtKBEntryVersionService.getEntryVersion(version, accessionOrId);
            return redirectToUniSave(accessionOrId, request, Optional.of(entryVersion));
        }
        if (accessionOrId.contains("_")) {
            String accession = entryService.findAccessionByProteinId(accessionOrId);
            return redirectToAccession(accessionOrId, accession, request);
        } else if (accessionOrId.contains(".")) {
            return redirectToUniSave(accessionOrId, request, Optional.empty());
        } else {
            Map<String, List<Pair<String, Boolean>>> accessionRangeMap = null;
            String accessionOnly = accessionOrId;

            if (UNIPROTKB_ACCESSION_SEQUENCE_RANGE_REGEX.matcher(accessionOrId).matches()) {
                accessionRangeMap = validateAndGetAccessionRangesMap(accessionOrId, request);
                accessionOnly = extractAccession(accessionOrId);
            }

            Optional<String> acceptedRdfContentType = getAcceptedRdfContentType(request);
            if (acceptedRdfContentType.isPresent()) {
                String rdf =
                        entryService.getRdf(accessionOrId, DATA_TYPE, acceptedRdfContentType.get());
                return super.getEntityResponseRdf(rdf, getAcceptHeader(request), request);
            } else {
                UniProtKBEntry entry = entryService.findByUniqueId(accessionOnly, fields);
                return super.getEntityResponse(entry, fields, accessionRangeMap, request);
            }
        }
    }

    /*
     * E.g., usage from command line:
     * time curl -OJ -H "Accept:text/list" "http://localhost:8090/uniprot/api/uniprotkb/download?query=reviewed:true" (i.e., show accessions)
     *   - for GZIPPED results, add -H "Accept-Encoding:gzip"
     *   - omit '-OJ' option to curl, to just see it print to standard output
     */
    @GetMapping(
            value = "/stream",
            produces = {
                TSV_MEDIA_TYPE_VALUE,
                FF_MEDIA_TYPE_VALUE,
                LIST_MEDIA_TYPE_VALUE,
                MediaType.APPLICATION_XML_VALUE,
                MediaType.APPLICATION_JSON_VALUE,
                XLS_MEDIA_TYPE_VALUE,
                FASTA_MEDIA_TYPE_VALUE,
                GFF_MEDIA_TYPE_VALUE,
                RDF_MEDIA_TYPE_VALUE,
                TURTLE_MEDIA_TYPE_VALUE,
                N_TRIPLES_MEDIA_TYPE_VALUE
            })
    @Operation(
            summary = STREAM_UNIPROTKB_OPERATION,
            description = STREAM_UNIPROTKB_OPERATION_DESC,
            responses = {
                @ApiResponse(
                        content = {
                            @Content(
                                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    array =
                                            @ArraySchema(
                                                    schema =
                                                            @Schema(
                                                                    implementation =
                                                                            UniProtKBEntry.class))),
                            @Content(
                                    mediaType = MediaType.APPLICATION_XML_VALUE,
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
            HttpServletRequest request) {

        MediaType contentType = getAcceptHeader(request);
        setBasicRequestFormat(streamRequest, request);
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

    @RequestMapping(
            value = "/accessions",
            method = {RequestMethod.GET},
            produces = {
                TSV_MEDIA_TYPE_VALUE,
                FF_MEDIA_TYPE_VALUE,
                LIST_MEDIA_TYPE_VALUE,
                MediaType.APPLICATION_XML_VALUE,
                MediaType.APPLICATION_JSON_VALUE,
                XLS_MEDIA_TYPE_VALUE,
                FASTA_MEDIA_TYPE_VALUE,
                GFF_MEDIA_TYPE_VALUE
            })
    @Operation(
            hidden = true,
            summary = IDS_UNIPROTKB_OPERATION,
            description = IDS_UNIPROTKB_OPERATION_DESC,
            responses = {
                @ApiResponse(
                        content = {
                            @Content(
                                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    array =
                                            @ArraySchema(
                                                    schema =
                                                            @Schema(
                                                                    implementation =
                                                                            UniProtKBEntry.class))),
                            @Content(
                                    mediaType = MediaType.APPLICATION_XML_VALUE,
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

    @RequestMapping(
            value = "/accessions",
            method = {RequestMethod.POST},
            produces = {
                TSV_MEDIA_TYPE_VALUE,
                FF_MEDIA_TYPE_VALUE,
                LIST_MEDIA_TYPE_VALUE,
                MediaType.APPLICATION_XML_VALUE,
                MediaType.APPLICATION_JSON_VALUE,
                XLS_MEDIA_TYPE_VALUE,
                FASTA_MEDIA_TYPE_VALUE,
                GFF_MEDIA_TYPE_VALUE
            })
    @Operation(
            hidden = true,
            summary = IDS_UNIPROTKB_OPERATION,
            description = IDS_UNIPROTKB_OPERATION_DESC,
            responses = {
                @ApiResponse(
                        content = {
                            @Content(
                                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    array =
                                            @ArraySchema(
                                                    schema =
                                                            @Schema(
                                                                    implementation =
                                                                            UniProtKBEntry.class))),
                            @Content(
                                    mediaType = MediaType.APPLICATION_XML_VALUE,
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
        Map<String, List<Pair<String, Boolean>>> accessionRangesMap =
                getAccessionSequenceRangesMap(accessionsRequest);
        return super.getSearchResponse(
                result,
                accessionsRequest.getFields(),
                accessionsRequest.isDownload(),
                false,
                accessionRangesMap,
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

    private String getUniSavePath(String accession) {
        String uniProtPath = ServletUriComponentsBuilder.fromCurrentRequest().build().getPath();
        String uniSavePath = "";
        if (uniProtPath != null) {
            uniSavePath = uniProtPath.replace("uniprotkb", "unisave");
            uniSavePath = uniSavePath.substring(0, uniSavePath.lastIndexOf('/') + 1) + accession;
        }
        return uniSavePath;
    }

    private ResponseEntity<MessageConverterContext<UniProtKBEntry>> redirectToUniSave(
            String accessionOrId, HttpServletRequest request, Optional<String> entryVersion) {
        MediaType contentType = getAcceptHeader(request);
        String version = "";
        String accession = "";
        if (entryVersion.isPresent()) {
            version = entryVersion.get();
            accession = accessionOrId;
        } else {
            version = accessionOrId.substring(accessionOrId.indexOf('.') + 1);
            accession = accessionOrId.substring(0, accessionOrId.indexOf('.'));
        }
        String uniSavePath = getUniSavePath(accession);
        if (!uniSavePath.isEmpty()) {
            String format = getFileExtension(contentType);
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
        return responseBuilder.headers(HeaderFactory.createHttpSearchHeader(contentType)).build();
    }

    private ResponseEntity<MessageConverterContext<UniProtKBEntry>> redirectToAccession(
            String accessionOrId, String accession, HttpServletRequest request) {
        MediaType contentType = getAcceptHeader(request);
        ResponseEntity.BodyBuilder responseBuilder =
                ResponseEntity.status(HttpStatus.SEE_OTHER)
                        .header(
                                HttpHeaders.LOCATION,
                                BasicSearchController.getLocationURLForId(
                                        accession, accessionOrId, contentType));
        return responseBuilder.headers(HeaderFactory.createHttpSearchHeader(contentType)).build();
    }

    private String extractAccession(String accessionOrId) {
        return accessionOrId.substring(0, accessionOrId.indexOf('['));
    }

    private String getSequenceRange(String accessionOrId) {
        return accessionOrId.substring(accessionOrId.indexOf('[') + 1, accessionOrId.indexOf(']'));
    }

    private void validateSubsequence(String sequenceRange, HttpServletRequest request) {
        MediaType format = getAcceptHeader(request);
        if (!FASTA_MEDIA_TYPE.equals(format)) {
            throw new InvalidRequestException(
                    "Sequence range is only supported for type " + FASTA_MEDIA_TYPE_VALUE);
        }
        // extract the range and validate
        String[] rangeTokens = sequenceRange.split("-");

        String errorMsg = "Invalid sequence range [" + sequenceRange + "]";

        if (rangeTokens.length != 2) {
            throw new InvalidRequestException(errorMsg);
        }

        try {
            int start = Integer.parseInt(rangeTokens[0]);
            int end = Integer.parseInt(rangeTokens[1]);
            if (start <= 0 || start > end) {
                throw new InvalidRequestException(errorMsg);
            }
        } catch (NumberFormatException nfe) {
            throw new InvalidRequestException(errorMsg);
        }
    }

    private Map<String, List<Pair<String, Boolean>>> validateAndGetAccessionRangesMap(
            String accessionOrId, HttpServletRequest request) {
        Map<String, List<Pair<String, Boolean>>> accessionRange = new HashMap<>();
        if (UNIPROTKB_ACCESSION_SEQUENCE_RANGE_REGEX.matcher(accessionOrId).matches()) {
            // values between square brackets e.g. 10-20
            String range = getSequenceRange(accessionOrId);
            validateSubsequence(range, request);
            String accessionOnly = extractAccession(accessionOrId);
            Pair<String, Boolean> rangeIsProcessedPair = new PairImpl<>(range, Boolean.FALSE);
            accessionRange = new HashMap<>();
            List<Pair<String, Boolean>> rangeIsProcessedPairs = new ArrayList<>();
            rangeIsProcessedPairs.add(rangeIsProcessedPair);
            accessionRange.put(accessionOnly, rangeIsProcessedPairs);
        }
        return accessionRange;
    }

    private Map<String, List<Pair<String, Boolean>>> getAccessionSequenceRangesMap(
            IdsSearchRequest accessionsRequest) {
        Map<String, List<Pair<String, Boolean>>> accessionRangesMap = new HashMap<>();
        for (String passedId : accessionsRequest.getCommaSeparatedIds().split(",")) {
            String sanitisedId = passedId.strip().toUpperCase();
            String sequenceRange = null;
            if (UNIPROTKB_ACCESSION_SEQUENCE_RANGE_REGEX.matcher(sanitisedId).matches()) {
                sequenceRange = getSequenceRange(sanitisedId);
                sanitisedId = extractAccession(sanitisedId);
            }
            Pair<String, Boolean> rangeIsProcessedPair =
                    new PairImpl<>(sequenceRange, Boolean.FALSE);
            List<Pair<String, Boolean>> rangeIsProcessedPairs;
            if (!accessionRangesMap.containsKey(sanitisedId)) {
                rangeIsProcessedPairs = new ArrayList<>();
                rangeIsProcessedPairs.add(rangeIsProcessedPair);
                accessionRangesMap.put(sanitisedId, rangeIsProcessedPairs);
            } else {
                rangeIsProcessedPairs = accessionRangesMap.get(sanitisedId);
                rangeIsProcessedPairs.add(rangeIsProcessedPair);
            }
        }
        return accessionRangesMap;
    }
}
