package org.uniprot.api.statistics.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.uniprot.api.statistics.entity.EntryType;
import org.uniprot.api.statistics.entity.StatisticsCategory;
import org.uniprot.api.statistics.entity.UniprotkbStatisticsEntry;
import org.uniprot.api.statistics.mapper.StatisticsMapper;
import org.uniprot.api.statistics.model.StatisticCategory;
import org.uniprot.api.statistics.repository.StatisticsCategoryRepository;
import org.uniprot.api.statistics.repository.UniprotkbStatisticsEntryRepository;

import java.util.*;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.uniprot.api.statistics.TestEntityGeneratorUtil.STATISTICS_CATEGORIES;
import static org.uniprot.api.statistics.TestEntityGeneratorUtil.STATISTICS_ENTRIES;

@ExtendWith(MockitoExtension.class)
class StatisticServiceTest {
    private static final String STATISTIC_TYPE = "statType";
    private static final EntryType ENTRY_TYPE = EntryType.SWISSPROT;
    private static final String VERSION = "version";
    public static final String CATEGORY_0 = "cat0";
    public static final String CATEGORY_1 = "cat1";
    @Mock private StatisticCategory statisticCategory0;
    @Mock private StatisticCategory statisticCategory1;
    @Mock private StatisticCategory statisticCategory2;
    @Mock private StatisticsCategory statisticsCategory0;
    @Mock private StatisticsCategory statisticsCategory1;
    @Mock private UniprotkbStatisticsEntryRepository statisticsEntryRepository;
    @Mock private StatisticsCategoryRepository statisticsCategoryRepository;
    @Mock private StatisticsMapper statisticsMapper;
    @InjectMocks private StatisticService statisticService;

    @BeforeEach
    void setUp() {
        when(statisticsMapper.map(STATISTIC_TYPE)).thenReturn(ENTRY_TYPE);
        when(statisticsMapper.map(
                        eq(STATISTICS_CATEGORIES[0]),
                        argThat(
                                argument ->
                                        matchWithEntries(
                                                argument,
                                                List.of(
                                                        STATISTICS_ENTRIES[0],
                                                        STATISTICS_ENTRIES[1])))))
                .thenReturn(statisticCategory0);
    }

    private static boolean matchWithEntries(
            Collection<UniprotkbStatisticsEntry> arg,
            Collection<UniprotkbStatisticsEntry> entries) {
        Set<UniprotkbStatisticsEntry> argSet = new HashSet<>(arg);
        Set<UniprotkbStatisticsEntry> entrySet = new HashSet<>(entries);
        return argSet.size() == entries.size() && argSet.containsAll(entrySet);
    }

    @Test
    void findAllByVersionAndStatisticTypeAndCategoryIn_whenEmptyListOfCategoriesPassed() {
        when(statisticsMapper.map(
                        eq(STATISTICS_CATEGORIES[2]),
                        argThat(
                                argument ->
                                        matchWithEntries(
                                                argument, List.of(STATISTICS_ENTRIES[4])))))
                .thenReturn(statisticCategory2);
        when(statisticsEntryRepository.findAllByReleaseNameAndEntryType(VERSION, ENTRY_TYPE))
                .thenReturn(
                        List.of(
                                STATISTICS_ENTRIES[0],
                                STATISTICS_ENTRIES[1],
                                STATISTICS_ENTRIES[3],
                                STATISTICS_ENTRIES[4]));
        when(statisticsMapper.map(
                        eq(STATISTICS_CATEGORIES[1]),
                        argThat(
                                argument ->
                                        matchWithEntries(
                                                argument, List.of(STATISTICS_ENTRIES[3])))))
                .thenReturn(statisticCategory1);

        Collection<StatisticCategory> results =
                statisticService.findAllByVersionAndStatisticTypeAndCategoryIn(
                        VERSION, STATISTIC_TYPE, Collections.emptyList());

        assertThat(
                results,
                containsInAnyOrder(statisticCategory0, statisticCategory1, statisticCategory2));
    }

    @Test
    void findAllByVersionAndStatisticTypeAndCategoryIn() {
        when(statisticsCategoryRepository.findByCategory(CATEGORY_0))
                .thenReturn(Optional.of(statisticsCategory0));
        when(statisticsCategoryRepository.findByCategory(CATEGORY_1))
                .thenReturn(Optional.of(statisticsCategory1));
        when(statisticsEntryRepository.findAllByReleaseNameAndEntryTypeAndStatisticsCategoryIdIn(
                        VERSION, ENTRY_TYPE, List.of(statisticsCategory0, statisticsCategory1)))
                .thenAnswer(
                        invocation ->
                                List.of(
                                        STATISTICS_ENTRIES[0],
                                        STATISTICS_ENTRIES[1],
                                        STATISTICS_ENTRIES[3]));
        when(statisticsMapper.map(
                        eq(STATISTICS_CATEGORIES[1]),
                        argThat(
                                argument ->
                                        matchWithEntries(
                                                argument, List.of(STATISTICS_ENTRIES[3])))))
                .thenReturn(statisticCategory1);

        Collection<StatisticCategory> results =
                statisticService.findAllByVersionAndStatisticTypeAndCategoryIn(
                        VERSION, STATISTIC_TYPE, List.of(CATEGORY_0, CATEGORY_1));

        assertThat(results, containsInAnyOrder(statisticCategory0, statisticCategory1));
    }

    @Test
    void findAllByVersionAndStatisticTypeAndCategoryIn_whenSingleCategoryIsPassed() {
        when(statisticsCategoryRepository.findByCategory(CATEGORY_0))
                .thenReturn(Optional.of(statisticsCategory0));
        when(statisticsEntryRepository.findAllByReleaseNameAndEntryTypeAndStatisticsCategoryIdIn(
                        VERSION, ENTRY_TYPE, List.of(statisticsCategory0)))
                .thenAnswer(invocation -> List.of(STATISTICS_ENTRIES[0], STATISTICS_ENTRIES[1]));

        Collection<StatisticCategory> results =
                statisticService.findAllByVersionAndStatisticTypeAndCategoryIn(
                        VERSION, STATISTIC_TYPE, List.of(CATEGORY_0));

        assertThat(results, containsInAnyOrder(statisticCategory0));
    }
}
