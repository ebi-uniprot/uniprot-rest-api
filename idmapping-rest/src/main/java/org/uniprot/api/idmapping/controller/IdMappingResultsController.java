package org.uniprot.api.idmapping.controller;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.uniprot.api.idmapping.common.service.IdMappingJobService.IDMAPPING_PATH;
import static org.uniprot.api.rest.openapi.OpenAPIConstants.*;
import static org.uniprot.api.rest.output.PredefinedAPIStatus.LIMIT_EXCEED_ERROR;
import static org.uniprot.api.rest.output.UniProtMediaType.*;
import static org.uniprot.api.rest.output.context.MessageConverterContextFactory.Resource.IDMAPPING_PIR;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.async.DeferredResult;
import org.uniprot.api.common.concurrency.Gatekeeper;
import org.uniprot.api.common.exception.InvalidRequestException;
import org.uniprot.api.common.repository.search.QueryResult;
import org.uniprot.api.idmapping.common.model.IdMappingJob;
import org.uniprot.api.idmapping.common.model.IdMappingResult;
import org.uniprot.api.idmapping.common.request.IdMappingPageRequest;
import org.uniprot.api.idmapping.common.request.IdMappingStreamRequest;
import org.uniprot.api.idmapping.common.response.model.IdMappingStringPair;
import org.uniprot.api.idmapping.common.service.IdMappingJobCacheService;
import org.uniprot.api.idmapping.common.service.IdMappingPIRService;
import org.uniprot.api.rest.controller.BasicSearchController;
import org.uniprot.api.rest.output.context.MessageConverterContext;
import org.uniprot.api.rest.output.context.MessageConverterContextFactory;
import org.uniprot.core.util.Utils;

import com.google.common.base.Stopwatch;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;

/**
 * Created 15/02/2021
 *
 * @author Edd
 */
@Tag(name = TAG_IDMAPPING_RESULT, description = TAG_IDMAPPING_RESULT_DESC)
@RestController
@Validated
@Slf4j
@RequestMapping(value = IDMAPPING_PATH)
public class IdMappingResultsController extends BasicSearchController<IdMappingStringPair> {

    private final IdMappingPIRService idMappingService;
    private final IdMappingJobCacheService cacheService;

    @Value("${id.mapping.max.to.ids.count:#{null}}") // value to 500k
    private Integer maxIdMappingToIdsCount;

    @Autowired
    public IdMappingResultsController(
            ApplicationEventPublisher eventPublisher,
            IdMappingPIRService idMappingService,
            IdMappingJobCacheService cacheService,
            MessageConverterContextFactory<IdMappingStringPair> converterContextFactory,
            ThreadPoolTaskExecutor downloadTaskExecutor,
            Gatekeeper downloadGatekeeper) {
        super(
                eventPublisher,
                converterContextFactory,
                downloadTaskExecutor,
                IDMAPPING_PIR,
                downloadGatekeeper);
        this.idMappingService = idMappingService;
        this.cacheService = cacheService;
    }

    @GetMapping(
            value = "/results/{jobId}",
            produces = {TSV_MEDIA_TYPE_VALUE, APPLICATION_JSON_VALUE, XLS_MEDIA_TYPE_VALUE})
    @Operation(
            summary = ID_MAPPING_RESULT_OPERATION,
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
                                                                            IdMappingStringPair
                                                                                    .class))),
                            @Content(mediaType = TSV_MEDIA_TYPE_VALUE),
                            @Content(mediaType = LIST_MEDIA_TYPE_VALUE),
                            @Content(mediaType = XLS_MEDIA_TYPE_VALUE)
                        })
            })
    public ResponseEntity<MessageConverterContext<IdMappingStringPair>> results(
            @Parameter(description = JOB_ID_IDMAPPING_DESCRIPTION) @PathVariable String jobId,
            @Valid @ModelAttribute IdMappingPageRequest pageRequest,
            HttpServletRequest request,
            HttpServletResponse response) {
        IdMappingJob completedJob = cacheService.getCompletedJobAsResource(jobId);
        // validate the mapped ids size
        validatedMappedIdsLimit(completedJob.getIdMappingResult());

        Stopwatch stopwatch = Stopwatch.createStarted();
        QueryResult<IdMappingStringPair> queryResult =
                idMappingService.queryResultPage(pageRequest, completedJob.getIdMappingResult());
        stopwatch.stop();

        log.debug(
                "[idmapping/results/{}] response took {} seconds (id count={})",
                jobId,
                stopwatch.elapsed(TimeUnit.SECONDS),
                completedJob.getIdMappingRequest().getIds().split(",").length);
        return super.getSearchResponse(queryResult, null, request, response);
    }

    @GetMapping(
            value = "/stream/{jobId}",
            produces = {
                TSV_MEDIA_TYPE_VALUE,
                APPLICATION_JSON_VALUE,
                XLS_MEDIA_TYPE_VALUE,
                LIST_MEDIA_TYPE_VALUE
            })
    @Operation(
            summary = ID_MAPPING_STREAM_OPERATION,
            description = STREAM_OPERATION_DESC,
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
                                                                            IdMappingStringPair
                                                                                    .class))),
                            @Content(mediaType = TSV_MEDIA_TYPE_VALUE),
                            @Content(mediaType = LIST_MEDIA_TYPE_VALUE),
                            @Content(mediaType = XLS_MEDIA_TYPE_VALUE)
                        })
            })
    public DeferredResult<ResponseEntity<MessageConverterContext<IdMappingStringPair>>>
            streamResults(
                    @Parameter(description = JOB_ID_IDMAPPING_DESCRIPTION) @PathVariable
                            String jobId,
                    @Valid @ModelAttribute IdMappingStreamRequest streamRequest,
                    HttpServletRequest request) {

        IdMappingJob completedJob = cacheService.getCompletedJobAsResource(jobId);
        // validate the mapped ids size
        validatedMappedIdsLimit(completedJob.getIdMappingResult());
        return super.stream(
                () -> completedJob.getIdMappingResult().getMappedIds().stream(),
                streamRequest,
                getAcceptHeader(request),
                request);
    }

    @Override
    protected String getEntityId(IdMappingStringPair entity) {

        return entity.getTo();
    }

    @Override
    protected Optional<String> getEntityRedirectId(
            IdMappingStringPair entity, HttpServletRequest request) {
        return Optional.empty();
    }

    private void validatedMappedIdsLimit(IdMappingResult result) {
        if (Utils.notNullNotEmpty(result.getMappedIds())
                && result.getMappedIds().size() > this.maxIdMappingToIdsCount) {
            throw new InvalidRequestException(
                    LIMIT_EXCEED_ERROR.getErrorMessage(this.maxIdMappingToIdsCount));
        }
    }
}
