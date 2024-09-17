package org.uniprot.api.unisave.controller;

import static org.springframework.http.HttpHeaders.*;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.uniprot.api.rest.openapi.OpenAPIConstants.*;
import static org.uniprot.api.rest.output.UniProtMediaType.*;
import static org.uniprot.api.unisave.request.UniSaveRequest.ACCESSION_PATTERN;

import java.util.stream.Stream;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import javax.validation.constraints.Pattern;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.uniprot.api.rest.output.UniProtMediaType;
import org.uniprot.api.rest.output.context.MessageConverterContext;
import org.uniprot.api.rest.output.context.MessageConverterContextFactory;
import org.uniprot.api.unisave.model.UniSaveEntry;
import org.uniprot.api.unisave.request.UniSaveRequest;
import org.uniprot.api.unisave.service.UniSaveService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;

/**
 * Created 20/03/20
 *
 * @author Edd
 */
@RestController
@RequestMapping("/unisave")
@Slf4j
@Validated
@Tag(name = TAG_UNISAVE, description = TAG_UNISAVE_DESC)
public class UniSaveController {
    private final MessageConverterContextFactory<UniSaveEntry> converterContextFactory;
    private final UniSaveService service;

    @Autowired
    public UniSaveController(
            MessageConverterContextFactory<UniSaveEntry> converterContextFactory,
            UniSaveService service) {
        this.converterContextFactory = converterContextFactory;
        this.service = service;
    }

    @Operation(
            summary = ID_UNISAVE_OPERATION,
            description = ID_UNISAVE_OPERATION_DESC,
            responses = {
                @ApiResponse(
                        content = {
                            @Content(
                                    mediaType = APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = UniSaveEntry.class)),
                            @Content(mediaType = FASTA_MEDIA_TYPE_VALUE),
                            @Content(mediaType = FF_MEDIA_TYPE_VALUE),
                            @Content(mediaType = TSV_MEDIA_TYPE_VALUE)
                        })
            })
    @GetMapping(
            value = "/{accession}",
            produces = {
                APPLICATION_JSON_VALUE,
                FASTA_MEDIA_TYPE_VALUE,
                FF_MEDIA_TYPE_VALUE,
                TSV_MEDIA_TYPE_VALUE
            })
    public ResponseEntity<MessageConverterContext<UniSaveEntry>> getEntries(
            @Parameter(
                            description = ACCESSION_UNIPROTKB_DESCRIPTION,
                            example = ACCESSION_UNIPROTKB_EXAMPLE)
                    @PathVariable("accession")
                    @Pattern(
                            regexp = ACCESSION_PATTERN,
                            message = "{search.invalid.accession.value}")
                    String accession,
            @Valid @ModelAttribute UniSaveRequest.Entries uniSaveRequest,
            HttpServletRequest servletRequest) {
        uniSaveRequest.setAccession(accession);
        String acceptHeader = getAcceptHeader(servletRequest);
        setContentIfRequired(uniSaveRequest, acceptHeader);
        MediaType contentType = UniProtMediaType.valueOf(acceptHeader);
        HttpHeaders httpHeaders =
                addDownloadHeaderIfRequired(uniSaveRequest, contentType, servletRequest);
        MessageConverterContext<UniSaveEntry> context =
                converterContextFactory.get(
                        MessageConverterContextFactory.Resource.UNISAVE, contentType);

        optimiseEntriesRequest(uniSaveRequest, contentType);
        context.setEntities(service.getEntries(uniSaveRequest).stream());

        return ResponseEntity.ok().headers(httpHeaders).body(context);
    }

    @Operation(
            summary = DIFF_UNISAVE_OPERATION,
            description = DIFF_UNISAVE_OPERATION_DESC,
            responses = {
                @ApiResponse(
                        content = {
                            @Content(
                                    mediaType = APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = UniSaveEntry.class))
                        })
            })
    @GetMapping(
            value = "/{accession}/diff",
            produces = {APPLICATION_JSON_VALUE})
    public ResponseEntity<MessageConverterContext<UniSaveEntry>> getDiff(
            @Parameter(
                            description = ACCESSION_UNIPROTKB_DESCRIPTION,
                            example = ACCESSION_UNIPROTKB_EXAMPLE)
                    @PathVariable("accession")
                    @Pattern(
                            regexp = ACCESSION_PATTERN,
                            message = "{search.invalid.accession.value}")
                    String accession,
            @Valid @ModelAttribute UniSaveRequest.Diff unisaveRequest,
            HttpServletRequest servletRequest) {
        MessageConverterContext<UniSaveEntry> context =
                converterContextFactory.get(
                        MessageConverterContextFactory.Resource.UNISAVE,
                        UniProtMediaType.valueOf(getAcceptHeader(servletRequest)));
        context.setEntityOnly(true);
        context.setEntities(
                Stream.of(
                        service.getDiff(
                                accession,
                                unisaveRequest.getVersion1(),
                                unisaveRequest.getVersion2())));

        return ResponseEntity.ok(context);
    }

    @Operation(
            summary = STATUS_UNISAVE_OPERATION,
            description = STATUS_UNISAVE_OPERATION_DESC,
            responses = {
                @ApiResponse(
                        content = {
                            @Content(
                                    mediaType = APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = UniSaveEntry.class))
                        })
            })
    @GetMapping(
            value = "/{accession}/status",
            produces = {APPLICATION_JSON_VALUE})
    public ResponseEntity<MessageConverterContext<UniSaveEntry>> getStatus(
            @Parameter(
                            description = ACCESSION_UNIPROTKB_DESCRIPTION,
                            example = ACCESSION_UNIPROTKB_EXAMPLE)
                    @PathVariable("accession")
                    @Pattern(
                            regexp = ACCESSION_PATTERN,
                            message = "{search.invalid.accession.value}")
                    String accession,
            HttpServletRequest servletRequest) {
        MessageConverterContext<UniSaveEntry> context =
                converterContextFactory.get(
                        MessageConverterContextFactory.Resource.UNISAVE,
                        UniProtMediaType.valueOf(getAcceptHeader(servletRequest)));
        context.setEntityOnly(true);
        context.setEntities(Stream.of(service.getAccessionStatus(accession)));

        return ResponseEntity.ok(context);
    }

    private void optimiseEntriesRequest(
            UniSaveRequest.Entries uniSaveRequest, MediaType contentType) {
        // user asks for unique sequences
        if (uniSaveRequest.isUniqueSequences()
                && !contentType.equals(UniProtMediaType.FASTA_MEDIA_TYPE)) {
            // only allow sequence aggregation for fasta format
            uniSaveRequest.setUniqueSequences(false);
        }
    }

    // responses in fasta/flatfile format will require fetching
    // content regardless of what user specifies, so set it
    private void setContentIfRequired(UniSaveRequest.Entries uniSaveRequest, String acceptHeader) {
        if (acceptHeader.equals(FASTA_MEDIA_TYPE_VALUE)
                || acceptHeader.equals(FF_MEDIA_TYPE_VALUE)) {
            uniSaveRequest.setIncludeContent(true);
        }
    }

    private String getAcceptHeader(HttpServletRequest servletRequest) {
        return servletRequest.getHeader(HttpHeaders.ACCEPT);
    }

    private HttpHeaders addDownloadHeaderIfRequired(
            UniSaveRequest.Entries request,
            MediaType contentType,
            HttpServletRequest servletRequest) {
        HttpHeaders httpHeaders = new HttpHeaders();
        if (request.isDownload()) {
            String requestQueryString = servletRequest.getQueryString();
            String queryString = requestQueryString == null ? "" : "-" + requestQueryString;
            String suffix = "." + UniProtMediaType.getFileExtension(contentType);
            httpHeaders.setContentDispositionFormData(
                    "attachment", "unisave-entries" + queryString + suffix);
            // used so that gate-way caching uses accept/accept-encoding headers as a key
            httpHeaders.add(VARY, ACCEPT);
            httpHeaders.add(VARY, ACCEPT_ENCODING);
        }
        return httpHeaders;
    }
}
