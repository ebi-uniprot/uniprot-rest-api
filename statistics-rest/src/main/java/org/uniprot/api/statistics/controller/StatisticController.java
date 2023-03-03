package org.uniprot.api.statistics.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.uniprot.api.statistics.model.StatisticModuleStatisticCategory;
import org.uniprot.api.statistics.model.StatisticModuleStatisticResult;
import org.uniprot.api.statistics.service.StatisticService;

import java.util.Collection;

import static org.springframework.util.MimeTypeUtils.APPLICATION_JSON_VALUE;

@RestController
@Validated
@RequestMapping("/statistics/releases")
public class StatisticController {
    private final StatisticService statisticService;

    public StatisticController(StatisticService statisticService) {
        this.statisticService = statisticService;
    }

    @GetMapping(
            value = "/{release}/{statisticType}",
            produces = {APPLICATION_JSON_VALUE})
    public ResponseEntity<StatisticModuleStatisticResult<StatisticModuleStatisticCategory>> getAllByVersionAndTypeAndCategoryIn(
            @PathVariable String release,
            @PathVariable String statisticType,
            @RequestParam(required = false, defaultValue = "") Collection<String> categories) {
        return ResponseEntity.ok()
                .body(
                        new StatisticModuleStatisticResult<>(
                                statisticService.findAllByVersionAndStatisticTypeAndCategoryIn(
                                        release, statisticType, categories)));
    }
}
