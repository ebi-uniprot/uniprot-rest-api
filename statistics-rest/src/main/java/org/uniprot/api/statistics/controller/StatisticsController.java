package org.uniprot.api.statistics.controller;

import static org.springframework.util.MimeTypeUtils.APPLICATION_JSON_VALUE;

import java.util.Collection;

import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.uniprot.api.statistics.model.StatisticCategory;
import org.uniprot.api.statistics.model.StatisticResult;
import org.uniprot.api.statistics.service.StatisticService;

@RestController
@Validated
@RequestMapping("/statistics/releases")
public class StatisticsController {
    private final StatisticService statisticsService;

    public StatisticsController(StatisticService statisticsService) {
        this.statisticsService = statisticsService;
    }

    @GetMapping(
            value = "/{release}/{statisticType}",
            produces = {APPLICATION_JSON_VALUE})
    public ResponseEntity<StatisticResult<StatisticCategory>> getAllByVersionAndCategoryIn(
            @PathVariable String release,
            @PathVariable String statisticType,
            @RequestParam(required = false, defaultValue = "") Collection<String> categories) {
        return ResponseEntity.ok()
                .body(
                        new StatisticResult<>(
                                statisticsService.findAllByVersionAndStatisticTypeAndCategoryIn(
                                        release, statisticType, categories)));
    }
}
