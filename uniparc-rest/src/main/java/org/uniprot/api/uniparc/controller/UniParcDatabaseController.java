package org.uniprot.api.uniparc.controller;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.uniprot.api.rest.output.UniProtMediaType.TSV_MEDIA_TYPE_VALUE;
import static org.uniprot.api.rest.output.context.MessageConverterContextFactory.Resource.UNIPARC;

import java.util.Optional;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.uniprot.api.common.repository.search.QueryResult;
import org.uniprot.api.rest.controller.BasicSearchController;
import org.uniprot.api.rest.output.context.MessageConverterContext;
import org.uniprot.api.rest.output.context.MessageConverterContextFactory;
import org.uniprot.api.uniparc.request.UniParcDatabasesRequest;
import org.uniprot.api.uniparc.service.UniParcQueryService;
import org.uniprot.core.uniparc.UniParcCrossReference;
import org.uniprot.store.search.field.validator.FieldRegexConstants;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * @author sahmad
 * @created 29/03/2021
 */
@Tag(
        name = "uniparc",
        description =
                "UniParc is a comprehensive and non-redundant database that contains most of the publicly available protein sequences in the world. Proteins may exist in different source databases and in multiple copies in the same database. UniParc avoids such redundancy by storing each unique sequence only once and giving it a stable and unique identifier (UPI).")
@RestController
@Validated
@RequestMapping("/uniparc")
public class UniParcDatabaseController extends BasicSearchController<UniParcCrossReference> {

    private final UniParcQueryService queryService;

    @Autowired
    public UniParcDatabaseController(
            ApplicationEventPublisher eventPublisher,
            UniParcQueryService queryService,
            MessageConverterContextFactory<UniParcCrossReference> converterContextFactory,
            ThreadPoolTaskExecutor downloadTaskExecutor) {
        super(eventPublisher, converterContextFactory, downloadTaskExecutor, UNIPARC);
        this.queryService = queryService;
    }

    @GetMapping(
            value = "/{upi}/databases",
            produces = {TSV_MEDIA_TYPE_VALUE, APPLICATION_JSON_VALUE})
    @Operation(
            summary = "Retrieve UniParc databases by a upi.",
            responses = {
                @ApiResponse(
                        content = {
                            @Content(
                                    mediaType = APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = UniParcCrossReference.class)),
                            @Content(mediaType = TSV_MEDIA_TYPE_VALUE)
                        })
            })
    public ResponseEntity<MessageConverterContext<UniParcCrossReference>> getDatabasesByUpi(
            @PathVariable
                    @Pattern(
                            regexp = FieldRegexConstants.UNIPARC_UPI_REGEX,
                            flags = {Pattern.Flag.CASE_INSENSITIVE},
                            message = "{search.invalid.upi.value}")
                    @NotNull(message = "{search.required}")
                    @Parameter(description = "UniParc ID (UPI)")
                    String upi,
            @Valid @ModelAttribute UniParcDatabasesRequest databasesRequest,
            HttpServletRequest request,
            HttpServletResponse response) {
        QueryResult<UniParcCrossReference> results =
                queryService.getDatabasesByUniParcId(upi, databasesRequest);
        return super.getSearchResponse(results, databasesRequest.getFields(), request, response);
    }

    @Override
    protected String getEntityId(UniParcCrossReference entity) {
        return entity.getId();
    }

    @Override
    protected Optional<String> getEntityRedirectId(
            UniParcCrossReference entity, HttpServletRequest request) {
        return Optional.empty();
    }
}
