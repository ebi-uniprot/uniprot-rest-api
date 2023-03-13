package org.uniprot.api.statistics.controller;

import static org.springframework.util.MimeTypeUtils.APPLICATION_JSON_VALUE;

import java.util.Set;

import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.uniprot.api.statistics.model.StatisticsModuleStatisticsCategory;
import org.uniprot.api.statistics.model.StatisticsModuleStatisticsResult;
import org.uniprot.api.statistics.service.StatisticsService;

@RestController
@Validated
@RequestMapping("/statistics/releases")
public class StatisticsController {
    private final StatisticsService statisticsService;

    public StatisticsController(StatisticsService statisticsService) {
        this.statisticsService = statisticsService;
    }

    @GetMapping(
            value = "/{release}/{statisticType}",
            produces = {APPLICATION_JSON_VALUE})
    public ResponseEntity<StatisticsModuleStatisticsResult<StatisticsModuleStatisticsCategory>>
            getAllByVersionAndTypeAndCategoryIn(
                    @PathVariable String release,
                    @PathVariable String statisticType,
                    @RequestParam(required = false, defaultValue = "") Set<String> categories) {
        return ResponseEntity.ok()
                .body(
                        new StatisticsModuleStatisticsResult<>(
                                statisticsService.findAllByVersionAndStatisticTypeAndCategoryIn(
                                        release, statisticType, categories)));
    }
}
