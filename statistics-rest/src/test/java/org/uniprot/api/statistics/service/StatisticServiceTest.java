package org.uniprot.api.statistics.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.uniprot.api.statistics.entity.EntryType;
import org.uniprot.api.statistics.entity.StatisticsCategory;
import org.uniprot.api.statistics.mapper.StatisticMapper;
import org.uniprot.api.statistics.model.StatisticsModuleStatisticAttribute;
import org.uniprot.api.statistics.model.StatisticModuleStatisticCategory;
import org.uniprot.api.statistics.model.StatisticModuleStatisticCategoryImpl;
import org.uniprot.api.statistics.model.StatisticModuleStatisticType;
import org.uniprot.api.statistics.repository.StatisticsCategoryRepository;
import org.uniprot.api.statistics.repository.UniprotkbStatisticsEntryRepository;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.Assert.assertThrows;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;
import static org.uniprot.api.statistics.TestEntityGeneratorUtil.*;

@ExtendWith(MockitoExtension.class)
class StatisticServiceTest {
    private static final String STATISTIC_TYPE = "reviewed";
    private static final EntryType ENTRY_TYPE = EntryType.SWISSPROT;
    private static final StatisticModuleStatisticType STATISTIC_TYPE_ENUM = StatisticModuleStatisticType.REVIEWED;
    private static final String VERSION = "version";
    public static final String CATEGORY_0 = "cat0";
    public static final String CATEGORY_1 = "cat1";
    public static final String CATEGORY_2 = "cat2";
    private StatisticModuleStatisticCategory statisticModuleStatisticCategory0;
    private StatisticModuleStatisticCategory statisticModuleStatisticCategory1;
    private StatisticModuleStatisticCategory statisticModuleStatisticCategory2;
    @Mock
    private StatisticsModuleStatisticAttribute statisticsModuleStatisticAttribute0;
    @Mock
    private StatisticsModuleStatisticAttribute statisticsModuleStatisticAttribute1;
    @Mock
    private StatisticsModuleStatisticAttribute statisticsModuleStatisticAttribute3;
    @Mock
    private StatisticsModuleStatisticAttribute statisticsModuleStatisticAttribute4;
    @Mock
    private StatisticsCategory statisticsCategory0;
    @Mock
    private StatisticsCategory statisticsCategory1;
    @Mock
    private UniprotkbStatisticsEntryRepository statisticsEntryRepository;
    @Mock
    private StatisticsCategoryRepository statisticsCategoryRepository;
    @Mock
    private StatisticMapper statisticMapper;
    @InjectMocks
    private StatisticService statisticService;

    @BeforeEach
    void setUp() {
        statisticModuleStatisticCategory0 = StatisticModuleStatisticCategoryImpl.builder()
                .categoryName(CATEGORY_0)
                .totalCount(STATISTICS_ENTRIES[0].getValueCount() + STATISTICS_ENTRIES[1].getValueCount())
                .items(List.of(statisticsModuleStatisticAttribute0, statisticsModuleStatisticAttribute1))
                .label(LABELS[0])
                .searchField(SEARCH_FIELDS[0])
                .build();
        statisticModuleStatisticCategory1 = StatisticModuleStatisticCategoryImpl.builder()
                .categoryName(CATEGORY_1)
                .totalCount(STATISTICS_ENTRIES[3].getValueCount())
                .items(List.of(statisticsModuleStatisticAttribute3))
                .label(LABELS[1])
                .searchField(SEARCH_FIELDS[1])
                .build();
        statisticModuleStatisticCategory2 = StatisticModuleStatisticCategoryImpl.builder()
                .categoryName(CATEGORY_2)
                .totalCount(STATISTICS_ENTRIES[4].getValueCount())
                .items(List.of(statisticsModuleStatisticAttribute4))
                .label(LABELS[2])
                .searchField(SEARCH_FIELDS[2])
                .build();
        lenient().when(statisticMapper.map(STATISTIC_TYPE_ENUM)).thenReturn(ENTRY_TYPE);
        lenient().when(statisticMapper.map(STATISTICS_ENTRIES[0])).thenReturn(statisticsModuleStatisticAttribute0);
        lenient().when(statisticMapper.map(STATISTICS_ENTRIES[1])).thenReturn(statisticsModuleStatisticAttribute1);
        lenient().when(statisticMapper.map(STATISTICS_ENTRIES[4])).thenReturn(statisticsModuleStatisticAttribute4);
        lenient().when(statisticMapper.map(STATISTICS_ENTRIES[3])).thenReturn(statisticsModuleStatisticAttribute3);
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

        Collection<StatisticModuleStatisticCategory> results =
                statisticService.findAllByVersionAndStatisticTypeAndCategoryIn(
                        VERSION, STATISTIC_TYPE, Collections.emptyList());

        assertThat(
                results,
                containsInAnyOrder(statisticModuleStatisticCategory0, statisticModuleStatisticCategory1, statisticModuleStatisticCategory2));
    }

    @Test
    void findAllByVersionAndStatisticTypeAndCategoryIn() {
        when(statisticsCategoryRepository.findByCategoryIgnoreCase(CATEGORY_0))
                .thenReturn(Optional.of(statisticsCategory0));
        when(statisticsCategoryRepository.findByCategoryIgnoreCase(CATEGORY_1))
                .thenReturn(Optional.of(statisticsCategory1));
        when(statisticsEntryRepository.findAllByReleaseNameAndEntryTypeAndStatisticsCategoryIn(
                VERSION, ENTRY_TYPE, List.of(statisticsCategory0, statisticsCategory1)))
                .thenReturn(
                        List.of(
                                STATISTICS_ENTRIES[0],
                                STATISTICS_ENTRIES[1],
                                STATISTICS_ENTRIES[3]));

        Collection<StatisticModuleStatisticCategory> results =
                statisticService.findAllByVersionAndStatisticTypeAndCategoryIn(
                        VERSION, STATISTIC_TYPE, List.of(CATEGORY_0, CATEGORY_1));

        assertThat(results, containsInAnyOrder(statisticModuleStatisticCategory0, statisticModuleStatisticCategory1));
    }

    @Test
    void findAllByVersionAndStatisticTypeAndCategoryIn_whenSingleCategoryIsPassed() {
        when(statisticsCategoryRepository.findByCategoryIgnoreCase(CATEGORY_0))
                .thenReturn(Optional.of(statisticsCategory0));
        when(statisticsEntryRepository.findAllByReleaseNameAndEntryTypeAndStatisticsCategoryIn(
                VERSION, ENTRY_TYPE, List.of(statisticsCategory0)))
                .thenReturn(List.of(STATISTICS_ENTRIES[0], STATISTICS_ENTRIES[1]));

        Collection<StatisticModuleStatisticCategory> results =
                statisticService.findAllByVersionAndStatisticTypeAndCategoryIn(
                        VERSION, STATISTIC_TYPE, List.of(CATEGORY_0));

        assertThat(results, containsInAnyOrder(statisticModuleStatisticCategory0));
    }

    @Test
    void findAllByVersionAndStatisticTypeAndCategoryIn_whenWrongCategoryIsPassed() {
        when(statisticsCategoryRepository.findByCategoryIgnoreCase(CATEGORY_0))
                .thenReturn(Optional.empty());

        assertThrows(
                IllegalArgumentException.class,
                () ->
                        statisticService.findAllByVersionAndStatisticTypeAndCategoryIn(
                                VERSION, STATISTIC_TYPE, List.of(CATEGORY_0)));
    }

    @Test
    void findAllByVersionAndStatisticTypeAndCategoryIn_whenWrongStatisticTypeIsPassed() {
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        statisticService.findAllByVersionAndStatisticTypeAndCategoryIn(
                                VERSION, "noExistingType", List.of(CATEGORY_0)));
    }
}
