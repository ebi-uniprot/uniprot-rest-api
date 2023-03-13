package org.uniprot.api.support.data.statistics.service;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.Assert.assertThrows;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;
import static org.uniprot.api.support.data.statistics.TestEntityGeneratorUtil.*;

import java.util.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.uniprot.api.support.data.statistics.entity.EntryType;
import org.uniprot.api.support.data.statistics.entity.StatisticsCategory;
import org.uniprot.api.support.data.statistics.mapper.StatisticsMapper;
import org.uniprot.api.support.data.statistics.model.StatisticsModuleStatisticsAttribute;
import org.uniprot.api.support.data.statistics.model.StatisticsModuleStatisticsCategory;
import org.uniprot.api.support.data.statistics.model.StatisticsModuleStatisticsCategoryImpl;
import org.uniprot.api.support.data.statistics.model.StatisticsModuleStatisticsType;
import org.uniprot.api.support.data.statistics.repository.StatisticsCategoryRepository;
import org.uniprot.api.support.data.statistics.repository.UniprotkbStatisticsEntryRepository;

@ExtendWith(MockitoExtension.class)
class StatisticsServiceTest {
    private static final String STATISTIC_TYPE = "reviewed";
    private static final EntryType ENTRY_TYPE = EntryType.SWISSPROT;
    private static final StatisticsModuleStatisticsType STATISTIC_TYPE_ENUM =
            StatisticsModuleStatisticsType.REVIEWED;
    private static final String VERSION = "version";
    private StatisticsModuleStatisticsCategory statisticsModuleStatisticsCategory0;
    private StatisticsModuleStatisticsCategory statisticsModuleStatisticsCategory1;
    private StatisticsModuleStatisticsCategory statisticsModuleStatisticsCategory2;
    @Mock private StatisticsModuleStatisticsAttribute statisticsModuleStatisticsAttribute0;
    @Mock private StatisticsModuleStatisticsAttribute statisticsModuleStatisticsAttribute1;
    @Mock private StatisticsModuleStatisticsAttribute statisticsModuleStatisticsAttribute3;
    @Mock private StatisticsModuleStatisticsAttribute statisticsModuleStatisticsAttribute4;
    @Mock private StatisticsCategory statisticsCategory0;
    @Mock private StatisticsCategory statisticsCategory1;
    @Mock private UniprotkbStatisticsEntryRepository statisticsEntryRepository;
    @Mock private StatisticsCategoryRepository statisticsCategoryRepository;
    @Mock private StatisticsMapper statisticsMapper;
    @InjectMocks private StatisticsServiceImpl statisticsService;

    @BeforeEach
    void setUp() {
        statisticsModuleStatisticsCategory0 =
                StatisticsModuleStatisticsCategoryImpl.builder()
                        .categoryName(CATEGORY_0)
                        .totalCount(
                                STATISTICS_ENTRIES[0].getValueCount()
                                        + STATISTICS_ENTRIES[1].getValueCount())
                        .items(
                                List.of(
                                        statisticsModuleStatisticsAttribute0,
                                        statisticsModuleStatisticsAttribute1))
                        .label(LABELS[0])
                        .searchField(SEARCH_FIELDS[0])
                        .build();
        statisticsModuleStatisticsCategory1 =
                StatisticsModuleStatisticsCategoryImpl.builder()
                        .categoryName(CATEGORY_1)
                        .totalCount(STATISTICS_ENTRIES[3].getValueCount())
                        .items(List.of(statisticsModuleStatisticsAttribute3))
                        .label(LABELS[1])
                        .searchField(SEARCH_FIELDS[1])
                        .build();
        statisticsModuleStatisticsCategory2 =
                StatisticsModuleStatisticsCategoryImpl.builder()
                        .categoryName(CATEGORY_2)
                        .totalCount(STATISTICS_ENTRIES[4].getValueCount())
                        .items(List.of(statisticsModuleStatisticsAttribute4))
                        .label(LABELS[2])
                        .searchField(SEARCH_FIELDS[2])
                        .build();
        lenient().when(statisticsMapper.map(STATISTIC_TYPE_ENUM)).thenReturn(ENTRY_TYPE);
        lenient()
                .when(statisticsMapper.map(STATISTICS_ENTRIES[0]))
                .thenReturn(statisticsModuleStatisticsAttribute0);
        lenient()
                .when(statisticsMapper.map(STATISTICS_ENTRIES[1]))
                .thenReturn(statisticsModuleStatisticsAttribute1);
        lenient()
                .when(statisticsMapper.map(STATISTICS_ENTRIES[4]))
                .thenReturn(statisticsModuleStatisticsAttribute4);
        lenient()
                .when(statisticsMapper.map(STATISTICS_ENTRIES[3]))
                .thenReturn(statisticsModuleStatisticsAttribute3);
    }

    @Test
    void findAllByVersionAndStatisticTypeAndCategoryIn_whenEmptyListOfCategoriesPassed() {
        when(statisticsEntryRepository.findAllByReleaseNameAndEntryType(VERSION, ENTRY_TYPE))
                .thenReturn(
                        List.of(
                                STATISTICS_ENTRIES[0],
                                STATISTICS_ENTRIES[1],
                                STATISTICS_ENTRIES[3],
                                STATISTICS_ENTRIES[4]));

        Collection<StatisticsModuleStatisticsCategory> results =
                statisticsService.findAllByVersionAndStatisticTypeAndCategoryIn(
                        VERSION, STATISTIC_TYPE, Collections.emptySet());

        assertThat(
                results,
                containsInAnyOrder(
                        statisticsModuleStatisticsCategory0,
                        statisticsModuleStatisticsCategory1,
                        statisticsModuleStatisticsCategory2));
    }

    @Test
    void findAllByVersionAndStatisticTypeAndCategoryIn() {
        when(statisticsCategoryRepository.findByCategoryIgnoreCase(CATEGORY_0))
                .thenReturn(Optional.of(statisticsCategory0));
        when(statisticsCategoryRepository.findByCategoryIgnoreCase(CATEGORY_1))
                .thenReturn(Optional.of(statisticsCategory1));
        when(statisticsEntryRepository.findAllByReleaseNameAndEntryTypeAndStatisticsCategoryIn(
                        VERSION, ENTRY_TYPE, Set.of(statisticsCategory0, statisticsCategory1)))
                .thenReturn(
                        List.of(
                                STATISTICS_ENTRIES[0],
                                STATISTICS_ENTRIES[1],
                                STATISTICS_ENTRIES[3]));

        Collection<StatisticsModuleStatisticsCategory> results =
                statisticsService.findAllByVersionAndStatisticTypeAndCategoryIn(
                        VERSION, STATISTIC_TYPE, Set.of(CATEGORY_0, CATEGORY_1));

        assertThat(
                results,
                containsInAnyOrder(
                        statisticsModuleStatisticsCategory0, statisticsModuleStatisticsCategory1));
    }

    @Test
    void findAllByVersionAndStatisticTypeAndCategoryIn_whenSingleCategoryIsPassed() {
        when(statisticsCategoryRepository.findByCategoryIgnoreCase(CATEGORY_0))
                .thenReturn(Optional.of(statisticsCategory0));
        when(statisticsEntryRepository.findAllByReleaseNameAndEntryTypeAndStatisticsCategoryIn(
                        VERSION, ENTRY_TYPE, Set.of(statisticsCategory0)))
                .thenReturn(List.of(STATISTICS_ENTRIES[0], STATISTICS_ENTRIES[1]));

        Collection<StatisticsModuleStatisticsCategory> results =
                statisticsService.findAllByVersionAndStatisticTypeAndCategoryIn(
                        VERSION, STATISTIC_TYPE, Set.of(CATEGORY_0));

        assertThat(results, containsInAnyOrder(statisticsModuleStatisticsCategory0));
    }

    @Test
    void findAllByVersionAndStatisticTypeAndCategoryIn_whenWrongCategoryIsPassed() {
        when(statisticsCategoryRepository.findByCategoryIgnoreCase(CATEGORY_0))
                .thenReturn(Optional.empty());

        assertThrows(
                IllegalArgumentException.class,
                () ->
                        statisticsService.findAllByVersionAndStatisticTypeAndCategoryIn(
                                VERSION, STATISTIC_TYPE, Set.of(CATEGORY_0)));
    }

    @Test
    void findAllByVersionAndStatisticTypeAndCategoryIn_whenWrongStatisticTypeIsPassed() {
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        statisticsService.findAllByVersionAndStatisticTypeAndCategoryIn(
                                VERSION, "noExistingType", Set.of(CATEGORY_0)));
    }
}
