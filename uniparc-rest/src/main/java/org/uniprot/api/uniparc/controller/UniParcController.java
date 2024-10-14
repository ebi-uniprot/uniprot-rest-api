package org.uniprot.api.uniparc.controller;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.MediaType.APPLICATION_XML_VALUE;
import static org.uniprot.api.rest.openapi.OpenAPIConstants.*;
import static org.uniprot.api.rest.output.UniProtMediaType.*;
import static org.uniprot.api.rest.output.context.MessageConverterContextFactory.Resource.UNIPARC;

import java.util.Optional;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.uniprot.api.common.concurrency.Gatekeeper;
import org.uniprot.api.rest.controller.BasicSearchController;
import org.uniprot.api.rest.output.context.MessageConverterContext;
import org.uniprot.api.rest.output.context.MessageConverterContextFactory;
import org.uniprot.api.uniparc.common.service.UniParcBestGuessService;
import org.uniprot.api.uniparc.common.service.UniParcEntryService;
import org.uniprot.api.uniparc.common.service.request.UniParcBestGuessRequest;
import org.uniprot.api.uniparc.common.service.request.UniParcGetByAccessionRequest;
import org.uniprot.api.uniparc.common.service.request.UniParcGetByUniParcIdRequest;
import org.uniprot.api.uniparc.common.service.request.UniParcSequenceRequest;
import org.uniprot.core.uniparc.UniParcEntry;
import org.uniprot.core.xml.jaxb.uniparc.Entry;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * @author jluo
 * @date: 21 Jun 2019
 */
@Tag(name = TAG_UNIPARC, description = TAG_UNIPARC_DESC)
@RestController
@Validated
@RequestMapping("/uniparc")
public class UniParcController extends BasicSearchController<UniParcEntry> {
    private static final String DATA_TYPE = "uniparc";

    private final UniParcEntryService queryService;
    private final UniParcBestGuessService bestGuessService;

    @Autowired
    public UniParcController(
            ApplicationEventPublisher eventPublisher,
            UniParcEntryService queryService,
            UniParcBestGuessService bestGuessService,
            MessageConverterContextFactory<UniParcEntry> converterContextFactory,
            ThreadPoolTaskExecutor downloadTaskExecutor,
            Gatekeeper downloadGatekeeper) {
        super(
                eventPublisher,
                converterContextFactory,
                downloadTaskExecutor,
                UNIPARC,
                downloadGatekeeper);
        this.queryService = queryService;
        this.bestGuessService = bestGuessService;
    }

    @GetMapping(
            value = "/{upi}",
            produces = {
                TSV_MEDIA_TYPE_VALUE,
                RDF_MEDIA_TYPE_VALUE,
                FASTA_MEDIA_TYPE_VALUE,
                APPLICATION_XML_VALUE,
                APPLICATION_JSON_VALUE,
                XLS_MEDIA_TYPE_VALUE,
                TURTLE_MEDIA_TYPE_VALUE,
                N_TRIPLES_MEDIA_TYPE_VALUE
            })
    @Operation(
            summary = ID_UNIPARC_OPERATION_DESC,
            description = ID_UNIPARC_OPERATION_DESC,
            responses = {
                @ApiResponse(
                        content = {
                            @Content(
                                    mediaType = APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = UniParcEntry.class)),
                            @Content(
                                    mediaType = APPLICATION_XML_VALUE,
                                    schema = @Schema(implementation = Entry.class)),
                            @Content(mediaType = RDF_MEDIA_TYPE_VALUE),
                            @Content(mediaType = XLS_MEDIA_TYPE_VALUE),
                            @Content(mediaType = TSV_MEDIA_TYPE_VALUE),
                            @Content(mediaType = FASTA_MEDIA_TYPE_VALUE)
                        })
            })
    public ResponseEntity<MessageConverterContext<UniParcEntry>> getByUpId(
            @Valid @ModelAttribute UniParcGetByUniParcIdRequest getByUniParcIdRequest,
            HttpServletRequest request) {

        MediaType contentType = getAcceptHeader(request);
        Optional<String> acceptedRdfContentType = getAcceptedRdfContentType(request);
        if (acceptedRdfContentType.isPresent()) {
            String result =
                    queryService.getRdf(
                            getByUniParcIdRequest.getUpi(),
                            DATA_TYPE,
                            acceptedRdfContentType.get());
            return super.getEntityResponseRdf(result, contentType, request);
        }

        UniParcEntry entry = queryService.getByUniParcId(getByUniParcIdRequest);
        return super.getEntityResponse(entry, getByUniParcIdRequest.getFields(), request);
    }

    @GetMapping(
            value = "/accession/{accession}",
            produces = {
                APPLICATION_JSON_VALUE,
                APPLICATION_XML_VALUE,
                FASTA_MEDIA_TYPE_VALUE,
                TSV_MEDIA_TYPE_VALUE,
                XLS_MEDIA_TYPE_VALUE,
                LIST_MEDIA_TYPE_VALUE
            })
    @Operation(
            hidden = true,
            summary = ACCESSION_UNIPARC_OPERATION,
            description = ACCESSION_UNIPARC_OPERATION_DESC,
            responses = {
                @ApiResponse(
                        content = {
                            @Content(
                                    mediaType = APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = UniParcEntry.class)),
                            @Content(
                                    mediaType = APPLICATION_XML_VALUE,
                                    schema = @Schema(implementation = Entry.class)),
                            @Content(mediaType = TSV_MEDIA_TYPE_VALUE),
                            @Content(mediaType = LIST_MEDIA_TYPE_VALUE),
                            @Content(mediaType = XLS_MEDIA_TYPE_VALUE),
                            @Content(mediaType = FASTA_MEDIA_TYPE_VALUE)
                        })
            })
    public ResponseEntity<MessageConverterContext<UniParcEntry>> getByAccession(
            @Valid @ModelAttribute UniParcGetByAccessionRequest getByAccessionRequest,
            HttpServletRequest request) {

        UniParcEntry entry = queryService.getByUniProtAccession(getByAccessionRequest);

        return super.getEntityResponse(entry, getByAccessionRequest.getFields(), request);
    }

    @SuppressWarnings("squid:S6856")
    @GetMapping(
            value = "/bestguess",
            produces = {APPLICATION_JSON_VALUE, APPLICATION_XML_VALUE, FASTA_MEDIA_TYPE_VALUE})
    @Operation(
            hidden = true,
            summary = BEST_GUESS_UNIPARC_OPERATION,
            description = BEST_GUESS_UNIPARC_OPERATION_DESC,
            responses = {
                @ApiResponse(
                        content = {
                            @Content(
                                    mediaType = APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = UniParcEntry.class)),
                            @Content(
                                    mediaType = APPLICATION_XML_VALUE,
                                    array =
                                            @ArraySchema(
                                                    schema =
                                                            @Schema(
                                                                    implementation = Entry.class,
                                                                    name = "entries"))),
                            @Content(mediaType = FASTA_MEDIA_TYPE_VALUE)
                        })
            })
    public ResponseEntity<MessageConverterContext<UniParcEntry>> uniParcBestGuess(
            @Valid @ModelAttribute UniParcBestGuessRequest bestGuessRequest,
            HttpServletRequest request) {

        UniParcEntry bestGuess = bestGuessService.getUniParcBestGuess(bestGuessRequest);

        return super.getEntityResponse(bestGuess, bestGuessRequest.getFields(), request);
    }

    @GetMapping(
            value = "/sequence",
            produces = {
                APPLICATION_JSON_VALUE,
                APPLICATION_XML_VALUE,
                FASTA_MEDIA_TYPE_VALUE,
                TSV_MEDIA_TYPE_VALUE,
                XLS_MEDIA_TYPE_VALUE,
                LIST_MEDIA_TYPE_VALUE
            })
    @Operation(
            hidden = true,
            summary = SEQUENCE_UNIPARC_OPERATION,
            description = SEQUENCE_UNIPARC_OPERATION_DESC,
            responses = {
                @ApiResponse(
                        content = {
                            @Content(
                                    mediaType = APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = UniParcEntry.class)),
                            @Content(
                                    mediaType = APPLICATION_XML_VALUE,
                                    schema = @Schema(implementation = Entry.class)),
                            @Content(mediaType = TSV_MEDIA_TYPE_VALUE),
                            @Content(mediaType = LIST_MEDIA_TYPE_VALUE),
                            @Content(mediaType = XLS_MEDIA_TYPE_VALUE),
                            @Content(mediaType = FASTA_MEDIA_TYPE_VALUE)
                        })
            })
    public ResponseEntity<MessageConverterContext<UniParcEntry>> uniParcSequenceFilter(
            @Valid @ModelAttribute UniParcSequenceRequest sequenceRequest,
            HttpServletRequest request) {

        UniParcEntry entry = queryService.getBySequence(sequenceRequest);

        return super.getEntityResponse(entry, sequenceRequest.getFields(), request);
    }

    @Override
    protected String getEntityId(UniParcEntry entity) {
        return entity.getUniParcId().getValue();
    }

    @Override
    protected Optional<String> getEntityRedirectId(
            UniParcEntry entity, HttpServletRequest request) {
        return Optional.empty();
    }
}
