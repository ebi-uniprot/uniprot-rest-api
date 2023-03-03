package org.uniprot.api.statistics.controller;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.uniprot.api.statistics.model.StatisticModuleStatisticCategory;
import org.uniprot.api.statistics.model.StatisticModuleStatisticResult;
import org.uniprot.api.statistics.service.StatisticService;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StatisticControllerTest {
    public static final String RELEASE = "rel";
    public static final String STATISTIC_TYPE = "statType";
    public static final String CATEGORY_0 = "cat0";
    public static final String CATEGORY_1 = "cat1";
    public static final List<String> CATEGORIES_0 = List.of(CATEGORY_0, CATEGORY_1);
    public static final List<String> CATEGORIES_1 = List.of(CATEGORY_0);
    public static final List<String> CATEGORIES_2 = Collections.emptyList();
    @Mock private Collection<StatisticModuleStatisticCategory> results0;
    @Mock private Collection<StatisticModuleStatisticCategory> results1;
    @Mock private Collection<StatisticModuleStatisticCategory> results2;
    @Mock private StatisticService statisticService;
    @InjectMocks private StatisticController statisticController;

    @Test
    void getAllByVersionAndCategoryIn() {
        when(statisticService.findAllByVersionAndStatisticTypeAndCategoryIn(
                        RELEASE, STATISTIC_TYPE, CATEGORIES_0))
                .thenReturn(results0);

        ResponseEntity<StatisticModuleStatisticResult<StatisticModuleStatisticCategory>> result =
                statisticController.getAllByVersionAndTypeAndCategoryIn(
                        RELEASE, STATISTIC_TYPE, CATEGORIES_0);

        assertSame(results0, result.getBody().getResults());
    }

    @Test
    void getAllByVersionAndCategoryIn_whenSingleCategoryPassed() {
        when(statisticService.findAllByVersionAndStatisticTypeAndCategoryIn(
                        RELEASE, STATISTIC_TYPE, CATEGORIES_1))
                .thenReturn(results1);

        ResponseEntity<StatisticModuleStatisticResult<StatisticModuleStatisticCategory>> result =
                statisticController.getAllByVersionAndTypeAndCategoryIn(
                        RELEASE, STATISTIC_TYPE, CATEGORIES_1);

        assertSame(results1, result.getBody().getResults());
    }

    @Test
    void getAllByVersionAndCategoryIn_whenEmptyCategoryCollectionPassed() {
        when(statisticService.findAllByVersionAndStatisticTypeAndCategoryIn(
                        RELEASE, STATISTIC_TYPE, CATEGORIES_2))
                .thenReturn(results2);

        ResponseEntity<StatisticModuleStatisticResult<StatisticModuleStatisticCategory>> result =
                statisticController.getAllByVersionAndTypeAndCategoryIn(
                        RELEASE, STATISTIC_TYPE, CATEGORIES_2);

        assertSame(results2, result.getBody().getResults());
    }
}
