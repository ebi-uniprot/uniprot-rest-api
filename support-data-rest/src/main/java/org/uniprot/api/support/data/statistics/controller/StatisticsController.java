package org.uniprot.api.support.data.statistics.controller;

import static org.springframework.util.MimeTypeUtils.APPLICATION_JSON_VALUE;
import static org.uniprot.api.rest.openapi.OpenAPIConstants.*;

import java.util.Set;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.uniprot.api.support.data.statistics.model.StatisticsModuleStatisticsCategory;
import org.uniprot.api.support.data.statistics.model.StatisticsModuleStatisticsHistory;
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
@RequestMapping("/statistics")
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
            value = "/releases/{release}/{statisticType}",
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

    @Operation(
            hidden = true,
            summary = "Get release statistics by UniProt release name.",
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
            value = "/releases/{release}",
            produces = {APPLICATION_JSON_VALUE})
    public ResponseEntity<StatisticsModuleStatisticsResult<StatisticsModuleStatisticsCategory>>
            getAllByVersionAndCategoryIn(
                    @Parameter(
                                    description = RELEASE_NAME_STATS_DESCRIPTION,
                                    example = RELEASE_NAME_STATS_EXAMPLE,
                                    required = true)
                            @PathVariable
                            String release,
                    @Parameter(
                                    description = CATEGORY_STATS_DESCRIPTION,
                                    example = CATEGORY_STATS_EXAMPLE)
                            @RequestParam(required = false, defaultValue = "")
                            Set<String> categories) {
        return ResponseEntity.ok()
                .body(
                        new StatisticsModuleStatisticsResult<>(
                                statisticsService.findAllByVersionAndCategoryIn(
                                        release, categories)));
    }

    @Operation(
            hidden = true,
            summary = "Get history by attribute and statistics type.",
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
            value = "/history/{attribute}",
            produces = {APPLICATION_JSON_VALUE})
    public ResponseEntity<StatisticsModuleStatisticsResult<StatisticsModuleStatisticsHistory>>
            getAllByAttributeAndTypeAndCategory(
                    @Parameter(
                                    description = RELEASE_NAME_ATTRIBUTE_DESCRIPTION,
                                    example = RELEASE_NAME_ATTRIBUTE_EXAMPLE,
                                    required = true,
                                    schema =
                                            @Schema(
                                                    type = "enum",
                                                    allowableValues = {"entry"}))
                            @PathVariable
                            String attribute,
                    @Parameter(
                                    description = TYPE_STATS_DESCRIPTION,
                                    example = TYPE_STATS_EXAMPLE,
                                    schema =
                                            @Schema(
                                                    type = "enum",
                                                    allowableValues = {"reviewed", "unreviewed"}))
                            @RequestParam(required = false, defaultValue = "")
                            String statisticType) {
        return ResponseEntity.ok()
                .body(
                        new StatisticsModuleStatisticsResult<>(
                                statisticsService.findAllByAttributeAndStatisticType(
                                        attribute, statisticType)));
    }
}
