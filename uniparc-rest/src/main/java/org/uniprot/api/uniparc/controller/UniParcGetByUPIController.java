package org.uniprot.api.uniparc.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.async.DeferredResult;
import org.uniprot.api.common.repository.search.QueryResult;
import org.uniprot.api.rest.controller.BasicSearchController;
import org.uniprot.api.rest.output.context.MessageConverterContext;
import org.uniprot.api.rest.output.context.MessageConverterContextFactory;
import org.uniprot.api.uniparc.model.UniParcEntryWrapper;
import org.uniprot.api.uniparc.request.UniParcBestGuessRequest;
import org.uniprot.api.uniparc.request.UniParcGetByAccessionRequest;
import org.uniprot.api.uniparc.request.UniParcGetByDBRefIdRequest;
import org.uniprot.api.uniparc.request.UniParcGetByProteomeIdRequest;
import org.uniprot.api.uniparc.request.UniParcGetByUniParcIdRequest;
import org.uniprot.api.uniparc.request.UniParcSearchRequest;
import org.uniprot.api.uniparc.request.UniParcSequenceRequest;
import org.uniprot.api.uniparc.request.UniParcStreamRequest;
import org.uniprot.api.uniparc.service.UniParcQueryService;
import org.uniprot.core.uniparc.UniParcEntry;
import org.uniprot.core.xml.jaxb.uniparc.Entry;

import java.util.Optional;
import java.util.stream.Stream;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.MediaType.APPLICATION_XML_VALUE;
import static org.uniprot.api.rest.output.UniProtMediaType.FASTA_MEDIA_TYPE_VALUE;
import static org.uniprot.api.rest.output.UniProtMediaType.LIST_MEDIA_TYPE;
import static org.uniprot.api.rest.output.UniProtMediaType.LIST_MEDIA_TYPE_VALUE;
import static org.uniprot.api.rest.output.UniProtMediaType.RDF_MEDIA_TYPE;
import static org.uniprot.api.rest.output.UniProtMediaType.RDF_MEDIA_TYPE_VALUE;
import static org.uniprot.api.rest.output.UniProtMediaType.TSV_MEDIA_TYPE;
import static org.uniprot.api.rest.output.UniProtMediaType.TSV_MEDIA_TYPE_VALUE;
import static org.uniprot.api.rest.output.UniProtMediaType.XLS_MEDIA_TYPE_VALUE;
import static org.uniprot.api.rest.output.context.MessageConverterContextFactory.Resource.UNIPARC;

/**
 * @author jluo
 * @date: 21 Jun 2019
 */
@Tag(
        name = "uniparc",
        description =
                "UniParc is a comprehensive and non-redundant database that contains most of the publicly available protein sequences in the world. Proteins may exist in different source databases and in multiple copies in the same database. UniParc avoids such redundancy by storing each unique sequence only once and giving it a stable and unique identifier (UPI).")
@RestController
@Validated
@RequestMapping("/uniparc")
public class UniParcGetByUPIController extends BasicSearchController<UniParcEntryWrapper> {

    private final UniParcQueryService queryService;
    private static final int PREVIEW_SIZE = 10;

    @Autowired
    public UniParcGetByUPIController(
            ApplicationEventPublisher eventPublisher,
            UniParcQueryService queryService,
            MessageConverterContextFactory<UniParcEntryWrapper> converterContextFactory,
            ThreadPoolTaskExecutor downloadTaskExecutor) {
        super(eventPublisher, converterContextFactory, downloadTaskExecutor, UNIPARC);
        this.queryService = queryService;
    }

    @GetMapping(
            value = "/{upi}",
            produces = {
                TSV_MEDIA_TYPE_VALUE,
                FASTA_MEDIA_TYPE_VALUE,
                APPLICATION_XML_VALUE,
                APPLICATION_JSON_VALUE,
                RDF_MEDIA_TYPE_VALUE
            })
    @Operation(
            summary = "Retrieve an UniParc entry by upi.",
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
                            @Content(mediaType = FASTA_MEDIA_TYPE_VALUE),
                            @Content(mediaType = RDF_MEDIA_TYPE_VALUE)
                        })
            })
    public ResponseEntity<MessageConverterContext<UniParcEntryWrapper>> getByUpId(
            @Valid @ModelAttribute UniParcGetByUniParcIdRequest getByUniParcIdRequest,
            HttpServletRequest request) {
        UniParcEntryWrapper entry = queryService.getByUniParcId(getByUniParcIdRequest);
        return super.getEntityResponse(entry, getByUniParcIdRequest.getFields(), request);
    }

    @Override
    protected String getEntityId(UniParcEntryWrapper entity) {
        return entity.getEntry().getUniParcId().getValue();
    }

    @Override
    protected Optional<String> getEntityRedirectId(UniParcEntryWrapper entity) {
        return Optional.empty();
    }
}
