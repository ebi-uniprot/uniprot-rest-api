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
import org.uniprot.api.statistics.model.StatisticAttribute;
import org.uniprot.api.statistics.model.StatisticCategory;
import org.uniprot.api.statistics.model.StatisticCategoryImpl;
import org.uniprot.api.statistics.model.StatisticType;
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
    private static final StatisticType STATISTIC_TYPE_ENUM = StatisticType.REVIEWED;
    private static final String VERSION = "version";
    public static final String CATEGORY_0 = "cat0";
    public static final String CATEGORY_1 = "cat1";
    public static final String CATEGORY_2 = "cat2";
    private StatisticCategory statisticCategory0;
    private StatisticCategory statisticCategory1;
    private StatisticCategory statisticCategory2;
    @Mock
    private StatisticAttribute statisticAttribute0;
    @Mock
    private StatisticAttribute statisticAttribute1;
    @Mock
    private StatisticAttribute statisticAttribute3;
    @Mock
    private StatisticAttribute statisticAttribute4;
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
        statisticCategory0 = StatisticCategoryImpl.builder()
                .name(CATEGORY_0)
                .totalCount(STATISTICS_ENTRIES[0].getValueCount() + STATISTICS_ENTRIES[1].getValueCount())
                .totalEntryCount(STATISTICS_ENTRIES[0].getEntryCount() + STATISTICS_ENTRIES[1].getEntryCount())
                .attributes(List.of(statisticAttribute0, statisticAttribute1))
                .label(LABELS[0])
                .searchField(SEARCH_FIELDS[0])
                .build();
        statisticCategory1 = StatisticCategoryImpl.builder()
                .name(CATEGORY_1)
                .totalCount(STATISTICS_ENTRIES[3].getValueCount())
                .totalEntryCount(STATISTICS_ENTRIES[3].getEntryCount())
                .attributes(List.of(statisticAttribute3))
                .label(LABELS[1])
                .searchField(SEARCH_FIELDS[1])
                .build();
        statisticCategory2 = StatisticCategoryImpl.builder()
                .name(CATEGORY_2)
                .totalCount(STATISTICS_ENTRIES[4].getValueCount())
                .totalEntryCount(STATISTICS_ENTRIES[4].getEntryCount())
                .attributes(List.of(statisticAttribute4))
                .label(LABELS[2])
                .searchField(SEARCH_FIELDS[2])
                .build();
        lenient().when(statisticMapper.map(STATISTIC_TYPE_ENUM)).thenReturn(ENTRY_TYPE);
        lenient().when(statisticMapper.map(STATISTICS_ENTRIES[0])).thenReturn(statisticAttribute0);
        lenient().when(statisticMapper.map(STATISTICS_ENTRIES[1])).thenReturn(statisticAttribute1);
        lenient().when(statisticMapper.map(STATISTICS_ENTRIES[4])).thenReturn(statisticAttribute4);
        lenient().when(statisticMapper.map(STATISTICS_ENTRIES[3])).thenReturn(statisticAttribute3);
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

        Collection<StatisticCategory> results =
                statisticService.findAllByVersionAndStatisticTypeAndCategoryIn(
                        VERSION, STATISTIC_TYPE, Collections.emptyList());

        assertThat(
                results,
                containsInAnyOrder(statisticCategory0, statisticCategory1, statisticCategory2));
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

        Collection<StatisticCategory> results =
                statisticService.findAllByVersionAndStatisticTypeAndCategoryIn(
                        VERSION, STATISTIC_TYPE, List.of(CATEGORY_0, CATEGORY_1));

        assertThat(results, containsInAnyOrder(statisticCategory0, statisticCategory1));
    }

    @Test
    void findAllByVersionAndStatisticTypeAndCategoryIn_whenSingleCategoryIsPassed() {
        when(statisticsCategoryRepository.findByCategoryIgnoreCase(CATEGORY_0))
                .thenReturn(Optional.of(statisticsCategory0));
        when(statisticsEntryRepository.findAllByReleaseNameAndEntryTypeAndStatisticsCategoryIn(
                VERSION, ENTRY_TYPE, List.of(statisticsCategory0)))
                .thenReturn(List.of(STATISTICS_ENTRIES[0], STATISTICS_ENTRIES[1]));

        Collection<StatisticCategory> results =
                statisticService.findAllByVersionAndStatisticTypeAndCategoryIn(
                        VERSION, STATISTIC_TYPE, List.of(CATEGORY_0));

        assertThat(results, containsInAnyOrder(statisticCategory0));
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
