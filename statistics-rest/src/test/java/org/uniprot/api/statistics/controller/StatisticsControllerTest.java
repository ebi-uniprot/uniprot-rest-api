package org.uniprot.api.statistics.controller;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.when;
import static org.uniprot.api.statistics.TestEntityGeneratorUtil.*;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.uniprot.api.statistics.model.StatisticsModuleStatisticsCategory;
import org.uniprot.api.statistics.model.StatisticsModuleStatisticsResult;
import org.uniprot.api.statistics.service.StatisticsService;

@ExtendWith(MockitoExtension.class)
class StatisticsControllerTest {
    public static final Set<String> CATEGORIES_0 = Set.of(CATEGORY_0, CATEGORY_1);
    public static final Set<String> CATEGORIES_1 = Set.of(CATEGORY_0);
    public static final Set<String> CATEGORIES_2 = Collections.emptySet();
    @Mock private List<StatisticsModuleStatisticsCategory> results0;
    @Mock private List<StatisticsModuleStatisticsCategory> results1;
    @Mock private List<StatisticsModuleStatisticsCategory> results2;
    @Mock private StatisticsService statisticsService;
    @InjectMocks private StatisticsController statisticsController;

    @Test
    void getAllByVersionAndCategoryIn() {
        when(statisticsService.findAllByVersionAndStatisticTypeAndCategoryIn(
                        RELEASE, STATISTIC_TYPE, CATEGORIES_0))
                .thenReturn(results0);

        ResponseEntity<StatisticsModuleStatisticsResult<StatisticsModuleStatisticsCategory>>
                result =
                        statisticsController.getAllByVersionAndTypeAndCategoryIn(
                                RELEASE, STATISTIC_TYPE, CATEGORIES_0);

        assertSame(results0, result.getBody().getResults());
    }

    @Test
    void getAllByVersionAndCategoryIn_whenSingleCategoryPassed() {
        when(statisticsService.findAllByVersionAndStatisticTypeAndCategoryIn(
                        RELEASE, STATISTIC_TYPE, CATEGORIES_1))
                .thenReturn(results1);

        ResponseEntity<StatisticsModuleStatisticsResult<StatisticsModuleStatisticsCategory>>
                result =
                        statisticsController.getAllByVersionAndTypeAndCategoryIn(
                                RELEASE, STATISTIC_TYPE, CATEGORIES_1);

        assertSame(results1, result.getBody().getResults());
    }

    @Test
    void getAllByVersionAndCategoryIn_whenEmptyCategoryCollectionPassed() {
        when(statisticsService.findAllByVersionAndStatisticTypeAndCategoryIn(
                        RELEASE, STATISTIC_TYPE, CATEGORIES_2))
                .thenReturn(results2);

        ResponseEntity<StatisticsModuleStatisticsResult<StatisticsModuleStatisticsCategory>>
                result =
                        statisticsController.getAllByVersionAndTypeAndCategoryIn(
                                RELEASE, STATISTIC_TYPE, CATEGORIES_2);

        assertSame(results2, result.getBody().getResults());
    }
}
