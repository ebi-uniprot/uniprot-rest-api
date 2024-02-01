package org.uniprot.api.support.data.statistics.controller;

import static org.springframework.util.MimeTypeUtils.APPLICATION_JSON_VALUE;

import java.util.Set;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.uniprot.api.support.data.statistics.model.StatisticsModuleStatisticsCategory;
import org.uniprot.api.support.data.statistics.model.StatisticsModuleStatisticsResult;
import org.uniprot.api.support.data.statistics.model.StatisticsModuleStatisticsType;
import org.uniprot.api.support.data.statistics.service.StatisticsService;
import org.uniprot.core.cv.subcell.SubcellularLocationEntry;

@RestController
@Tag(name = "Release statistics", description = "UniProtKB release statistics")
@RequestMapping("/statistics/releases")
public class StatisticsController {
    private final StatisticsService statisticsService;

    public StatisticsController(StatisticsService statisticsService) {
        this.statisticsService = statisticsService;
    }

    @Operation(
            summary = "Get release statistics by UniProt release name and statistics type.",
            responses = {
                    @ApiResponse(
                            content = {
                                    @Content(
                                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                                            schema =
                                            @Schema(
                                                    implementation =
                                                            StatisticsModuleStatisticsResult.class))})})
    @GetMapping(
            value = "/{release}/{statisticType}",
            produces = {APPLICATION_JSON_VALUE})
    public ResponseEntity<StatisticsModuleStatisticsResult<StatisticsModuleStatisticsCategory>>
            getAllByVersionAndTypeAndCategoryIn(
                    @Parameter(description = "UniProt release name", example = "2023_05", required = true)
                    @PathVariable String release,
                    @Parameter(description = "Statistic type", example = "reviewed", required = true,
                            schema = @Schema(type = "enum", allowableValues = {"reviewed", "unreviewed"}))
                    @PathVariable String statisticType,
                    @Parameter(description = "List of statistics categories, separated by commas.", example = "TOTAL_ORGANISM,COMMENTS")
                    @RequestParam(required = false, defaultValue = "") Set<String> categories) {
        return ResponseEntity.ok()
                .body(
                        new StatisticsModuleStatisticsResult<>(
                                statisticsService.findAllByVersionAndStatisticTypeAndCategoryIn(
                                        release, statisticType, categories)));
    }
}
