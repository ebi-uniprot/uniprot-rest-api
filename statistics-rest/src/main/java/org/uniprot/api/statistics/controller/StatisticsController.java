package org.uniprot.api.statistics.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.uniprot.api.statistics.model.StatisticAttribute;
import org.uniprot.api.statistics.model.StatisticCategory;
import org.uniprot.api.statistics.model.StatisticType;
import org.uniprot.api.statistics.service.StatisticService;

import java.util.Collection;

import static org.springframework.util.MimeTypeUtils.APPLICATION_JSON_VALUE;

@RestController
@Validated
@RequestMapping("/statistics")
public class StatisticsController {
    private final StatisticService statisticsService;

    public StatisticsController(StatisticService statisticsService) {
        this.statisticsService = statisticsService;
    }

    @GetMapping(
            value = "/{version}/{statisticType}",
            produces = {APPLICATION_JSON_VALUE})
    public ResponseEntity<Collection<StatisticCategory>> getAllByVersion(
            @PathVariable String version, @PathVariable StatisticType statisticType) {
        return ResponseEntity.ok()
                .body(statisticsService.findAllByVersionAndStatisticType(version, statisticType));
    }

    @GetMapping(
            value = "/{version}/{statisticType}/{category}",
            produces = {APPLICATION_JSON_VALUE})
    public ResponseEntity<StatisticCategory>
            getAllByVersionAndStatisticTypeAndCategory(
                    @PathVariable String version,
                    @PathVariable StatisticType statisticType,
                    @PathVariable String category) {
        return ResponseEntity.ok()
                .body(
                        statisticsService.findAllByVersionAndStatisticTypeAndCategory(
                                version, statisticType, category));
    }

    @GetMapping(
            value = "/{version}/{statisticType}/{category}/{attribute}",
            produces = {APPLICATION_JSON_VALUE})
    public ResponseEntity<StatisticAttribute>
            getAllByVersionAndStatisticTypeAndStatisticCategoryAndAttributeName(
                    @PathVariable String version,
                    @PathVariable StatisticType statisticType,
                    @PathVariable String category,
                    @PathVariable String attribute) {
        return ResponseEntity.ok()
                .body(
                        statisticsService.findAllByVersionAndStatisticTypeAndCategoryAndAttribute(
                                version, statisticType, category, attribute));
    }
}
