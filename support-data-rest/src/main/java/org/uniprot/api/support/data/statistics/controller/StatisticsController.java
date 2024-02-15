package org.uniprot.api.support.data.statistics.controller;

import static org.springframework.util.MimeTypeUtils.APPLICATION_JSON_VALUE;
import static org.uniprot.api.rest.openapi.OpenApiConstants.*;

import java.util.Set;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.uniprot.api.support.data.statistics.model.StatisticsModuleStatisticsCategory;
import org.uniprot.api.support.data.statistics.model.StatisticsModuleStatisticsResult;
import org.uniprot.api.support.data.statistics.service.StatisticsService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@Tag(name = TAG_RELEASE_STAT, description = TAG_RELEASE_STAT_DESC)
@RequestMapping("/statistics/releases")
public class StatisticsController {
    private final StatisticsService statisticsService;

    public StatisticsController(StatisticsService statisticsService) {
        this.statisticsService = statisticsService;
    }

    @Operation(
            hidden = true,
            summary = "Get release statistics by UniProt release name and statistics type.",
            responses = {
                @ApiResponse(
                        content = {
                            @Content(
                                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema =
                                            @Schema(
                                                    implementation =
                                                            StatisticsModuleStatisticsResult.class))
                        })
            })
    @GetMapping(
            value = "/{release}/{statisticType}",
            produces = {APPLICATION_JSON_VALUE})
    public ResponseEntity<StatisticsModuleStatisticsResult<StatisticsModuleStatisticsCategory>>
            getAllByVersionAndTypeAndCategoryIn(
                    @Parameter(
                                    description = RELEASE_NAME_STATS_DESCRIPTION,
                                    example = RELEASE_NAME_STATS_EXAMPLE,
                                    required = true)
                            @PathVariable
                            String release,
                    @Parameter(
                                    description = TYPE_STATS_DESCRIPTION,
                                    example = TYPE_STATS_EXAMPLE,
                                    required = true,
                                    schema =
                                            @Schema(
                                                    type = "enum",
                                                    allowableValues = {"reviewed", "unreviewed"}))
                            @PathVariable
                            String statisticType,
                    @Parameter(
                                    description = CATEGORY_STATS_DESCRIPTION,
                                    example = CATEGORY_STATS_EXAMPLE)
                            @RequestParam(required = false, defaultValue = "")
                            Set<String> categories) {
        return ResponseEntity.ok()
                .body(
                        new StatisticsModuleStatisticsResult<>(
                                statisticsService.findAllByVersionAndStatisticTypeAndCategoryIn(
                                        release, statisticType, categories)));
    }
}
