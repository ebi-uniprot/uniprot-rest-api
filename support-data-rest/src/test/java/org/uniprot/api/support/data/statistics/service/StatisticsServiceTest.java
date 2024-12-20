package org.uniprot.api.support.data.statistics.service;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;
import static org.uniprot.api.support.data.statistics.TestEntityGeneratorUtil.*;
import static org.uniprot.api.support.data.statistics.entity.EntryType.SWISSPROT;
import static org.uniprot.api.support.data.statistics.entity.EntryType.TREMBL;
import static org.uniprot.api.support.data.statistics.model.StatisticsModuleStatisticsType.*;

import java.time.Instant;
import java.util.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.uniprot.api.support.data.statistics.entity.EntryType;
import org.uniprot.api.support.data.statistics.entity.StatisticsCategory;
import org.uniprot.api.support.data.statistics.entity.UniProtKBStatisticsEntry;
import org.uniprot.api.support.data.statistics.entity.UniProtRelease;
import org.uniprot.api.support.data.statistics.mapper.StatisticsMapper;
import org.uniprot.api.support.data.statistics.model.*;
import org.uniprot.api.support.data.statistics.repository.AttributeQueryRepository;
import org.uniprot.api.support.data.statistics.repository.StatisticsCategoryRepository;
import org.uniprot.api.support.data.statistics.repository.UniProtKBStatisticsEntryRepository;
import org.uniprot.api.support.data.statistics.repository.UniProtReleaseRepository;

@ExtendWith(MockitoExtension.class)
class StatisticsServiceTest {
    private static final String STATISTIC_TYPE = "reviewed";
    private static final EntryType ENTRY_TYPE = EntryType.SWISSPROT;
    private static final StatisticsModuleStatisticsType STATISTIC_TYPE_ENUM =
            REVIEWED;
    private static final String VERSION = "version";
    private static final UniProtRelease UNI_PROT_RELEASE = new UniProtRelease();
    public static final String NAME_0 = "name0";
    private static final Date PREVIOUS_RELEASE_DATE =
            Date.from(Instant.ofEpochMilli(1653476723000L));
    private StatisticsModuleStatisticsCategory statisticsModuleStatisticsCategory0;
    private StatisticsModuleStatisticsCategory statisticsModuleStatisticsCategory1;
    private StatisticsModuleStatisticsCategory statisticsModuleStatisticsCategory2;
    @Mock private StatisticsModuleStatisticsAttribute statisticsModuleStatisticsAttribute0;
    @Mock private StatisticsModuleStatisticsAttribute statisticsModuleStatisticsAttribute1;
    @Mock private StatisticsModuleStatisticsAttribute statisticsModuleStatisticsAttribute3;
    @Mock private StatisticsModuleStatisticsAttribute statisticsModuleStatisticsAttribute4;
    private final StatisticsModuleStatisticsHistory statisticsModuleStatisticsHistory0 =
            StatisticsModuleStatisticsHistoryImpl.builder().releaseName("2022").build();
    private final StatisticsModuleStatisticsHistory statisticsModuleStatisticsHistory1 =
            StatisticsModuleStatisticsHistoryImpl.builder().releaseName("2021").build();
    @Mock private StatisticsCategory statisticsCategory0;
    @Mock private StatisticsCategory statisticsCategory1;
    @Mock private UniProtKBStatisticsEntryRepository statisticsEntryRepository;
    @Mock private StatisticsCategoryRepository statisticsCategoryRepository;
    @Mock private UniProtReleaseRepository releaseRepository;
    @Mock private AttributeQueryRepository attributeQueryRepository;
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
        lenient().when(statisticsMapper.map(UNREVIEWED)).thenReturn(TREMBL);
        lenient()
                .when(statisticsMapper.map(STATISTICS_ENTRIES[0], QUERIES[0]))
                .thenReturn(statisticsModuleStatisticsAttribute0);
       /* lenient()
                .when(statisticsMapper.map(argThat(e->ENTRY_NAMES[0].equals(e.getAttributeName())), same(QUERIES_COMMON[0])))
                .thenReturn(statisticsModuleStatisticsAttribute0);
        lenient()
                .when(statisticsMapper.map(argThat(e->ENTRY_NAMES[1].equals(e.getAttributeName())), same(QUERIES_COMMON[1])))
                .thenReturn(statisticsModuleStatisticsAttribute0);*/
        lenient()
                .when(statisticsMapper.map(STATISTICS_ENTRIES[1], QUERIES[1]))
                .thenReturn(statisticsModuleStatisticsAttribute1);
        lenient()
                .when(statisticsMapper.map(STATISTICS_ENTRIES[4], QUERIES[4]))
                .thenReturn(statisticsModuleStatisticsAttribute4);
        lenient()
                .when(statisticsMapper.map(STATISTICS_ENTRIES[3], QUERIES[3]))
                .thenReturn(statisticsModuleStatisticsAttribute3);
        lenient()
                .when(
                        statisticsMapper.map(
                                getCommonEntry(STATISTICS_ENTRIES[0]), QUERIES_COMMON[0]))
                .thenReturn(statisticsModuleStatisticsAttribute0);
        lenient()
                .when(
                        statisticsMapper.map(
                                getCommonEntry(STATISTICS_ENTRIES[1]), QUERIES_COMMON[1]))
                .thenReturn(statisticsModuleStatisticsAttribute1);
        lenient()
                .when(
                        statisticsMapper.map(
                                getCommonEntry(STATISTICS_ENTRIES[4]), QUERIES_COMMON[4]))
                .thenReturn(statisticsModuleStatisticsAttribute4);
        lenient()
                .when(
                        statisticsMapper.map(
                                getCommonEntry(STATISTICS_ENTRIES[3]), QUERIES_COMMON[3]))
                .thenReturn(statisticsModuleStatisticsAttribute3);
        lenient()
                .when(releaseRepository.findByNameAndEntryType(VERSION, ENTRY_TYPE))
                        .thenReturn(Optional.of(UNI_PROT_RELEASE));
        lenient()
                .when(releaseRepository.findByNameAndEntryType(VERSION, TREMBL))
                        .thenReturn(Optional.of(UNI_PROT_RELEASE));
        lenient()
                .when(attributeQueryRepository.findByStatisticsCategoryAndAttributeNameIgnoreCase(STATISTICS_CATEGORIES[1],ENTRY_NAMES[0]))
                .thenReturn(Optional.of(ATTRIBUTE_QUERIES[0]));
        lenient()
                .when(attributeQueryRepository.findByStatisticsCategoryAndAttributeNameIgnoreCase(STATISTICS_CATEGORIES[0],ENTRY_NAMES[0]))
                .thenReturn(Optional.of(ATTRIBUTE_QUERIES[0]));
        lenient()
                .when(attributeQueryRepository.findByStatisticsCategoryAndAttributeNameIgnoreCase(STATISTICS_CATEGORIES[0], ENTRY_NAMES[1]))
                .thenReturn(Optional.of(ATTRIBUTE_QUERIES[1]));
        lenient()
                .when(attributeQueryRepository.findByStatisticsCategoryAndAttributeNameIgnoreCase(STATISTICS_CATEGORIES[2], ENTRY_NAMES[1]))
                .thenReturn(Optional.of(ATTRIBUTE_QUERIES[1]));
        lenient()
                .when(attributeQueryRepository.findByStatisticsCategoryAndAttributeNameIgnoreCase(STATISTICS_CATEGORIES[0], ENTRY_NAMES[5]))
                .thenReturn(Optional.empty());
        lenient()
                .when(releaseRepository.findPreviousReleaseDate(REL_0))
                .thenReturn(Optional.of(PREVIOUS_RELEASE_DATE));
    }

    private UniProtKBStatisticsEntry getCommonEntry(UniProtKBStatisticsEntry statisticsEntry) {
        UniProtKBStatisticsEntry entry = new UniProtKBStatisticsEntry();
        entry.setAttributeName(statisticsEntry.getAttributeName());
        entry.setStatisticsCategory(statisticsEntry.getStatisticsCategory());
        entry.setValueCount(statisticsEntry.getValueCount());
        entry.setEntryCount(statisticsEntry.getEntryCount());
        entry.setDescription(statisticsEntry.getDescription());
        entry.setUniprotRelease(statisticsEntry.getUniprotRelease());
        return entry;
    }

    @Test
    void findAllByVersionAndStatisticTypeAndCategoryIn_whenEmptyListOfCategoriesPassed() {
        when(statisticsEntryRepository.findAllByUniprotRelease(UNI_PROT_RELEASE))
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
    void findAllByVersionAndCategoryIn_whenEmptyListOfCategoriesPassed() {
        when(statisticsEntryRepository.findAllByUniprotRelease(UNI_PROT_RELEASE))
                .thenReturn(
                        List.of(
                                STATISTICS_ENTRIES[0],
                                STATISTICS_ENTRIES[1],
                                STATISTICS_ENTRIES[3],
                                STATISTICS_ENTRIES[4]));

        Collection<StatisticsModuleStatisticsCategory> results =
                statisticsService.findAllByVersionAndCategoryIn(VERSION, Collections.emptySet());

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
        when(statisticsEntryRepository.findAllByUniprotReleaseAndStatisticsCategoryIn(
                        UNI_PROT_RELEASE,
                        Set.of(statisticsCategory0, statisticsCategory1)))
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
    void findAllByVersionAndCategoryIn() {
        when(statisticsCategoryRepository.findByCategoryIgnoreCase(CATEGORY_0))
                .thenReturn(Optional.of(statisticsCategory0));
        when(statisticsCategoryRepository.findByCategoryIgnoreCase(CATEGORY_1))
                .thenReturn(Optional.of(statisticsCategory1));
        when(statisticsEntryRepository.findAllByUniprotReleaseAndStatisticsCategoryIn(
                        UNI_PROT_RELEASE, Set.of(statisticsCategory0, statisticsCategory1)))
                .thenReturn(
                        List.of(
                                STATISTICS_ENTRIES[0],
                                STATISTICS_ENTRIES[1],
                                STATISTICS_ENTRIES[3]));

        Collection<StatisticsModuleStatisticsCategory> results =
                statisticsService.findAllByVersionAndCategoryIn(
                        VERSION, Set.of(CATEGORY_0, CATEGORY_1));

        assertThat(
                results,
                containsInAnyOrder(
                        statisticsModuleStatisticsCategory0, statisticsModuleStatisticsCategory1));
    }

    @Test
    void findAllByVersionAndStatisticTypeAndCategoryIn_whenSingleCategoryIsPassed() {
        when(statisticsCategoryRepository.findByCategoryIgnoreCase(CATEGORY_0))
                .thenReturn(Optional.of(statisticsCategory0));
        when(statisticsEntryRepository.findAllByUniprotReleaseAndStatisticsCategoryIn(
                        UNI_PROT_RELEASE, Set.of(statisticsCategory0)))
                .thenReturn(List.of(STATISTICS_ENTRIES[0], STATISTICS_ENTRIES[1]));

        Collection<StatisticsModuleStatisticsCategory> results =
                statisticsService.findAllByVersionAndStatisticTypeAndCategoryIn(
                        VERSION, STATISTIC_TYPE, Set.of(CATEGORY_0));

        assertThat(results, containsInAnyOrder(statisticsModuleStatisticsCategory0));
    }

    @Test
    void findAllByVersionAndCategoryIn_whenSingleCategoryIsPassed() {
        when(statisticsCategoryRepository.findByCategoryIgnoreCase(CATEGORY_0))
                .thenReturn(Optional.of(statisticsCategory0));
        when(statisticsEntryRepository.findAllByUniprotReleaseAndStatisticsCategoryIn(
                        UNI_PROT_RELEASE, Set.of(statisticsCategory0)))
                .thenReturn(List.of(STATISTICS_ENTRIES[0], STATISTICS_ENTRIES[1]));

        Collection<StatisticsModuleStatisticsCategory> results =
                statisticsService.findAllByVersionAndCategoryIn(VERSION, Set.of(CATEGORY_0));

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
    void findAllByVersionAndCategoryIn_whenWrongCategoryIsPassed() {
        when(statisticsCategoryRepository.findByCategoryIgnoreCase(CATEGORY_0))
                .thenReturn(Optional.empty());

        assertThrows(
                IllegalArgumentException.class,
                () -> statisticsService.findAllByVersionAndCategoryIn(VERSION, Set.of(CATEGORY_0)));
    }

    @Test
    void findAllByVersionAndStatisticTypeAndCategoryIn_whenWrongStatisticTypeIsPassed() {
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        statisticsService.findAllByVersionAndStatisticTypeAndCategoryIn(
                                VERSION, "noExistingType", Set.of(CATEGORY_0)));
    }

    @Test
    void findAllByAttributeAndStatisticType() {
        when(statisticsEntryRepository.findAllByAttributeNameIgnoreCaseAndEntryType(
                        NAME_0, SWISSPROT))
                .thenReturn(List.of(STATISTICS_ENTRIES[0], STATISTICS_ENTRIES[3]));
        when(statisticsMapper.mapHistory(STATISTICS_ENTRIES[0]))
                .thenReturn(statisticsModuleStatisticsHistory0);
        when(statisticsMapper.mapHistory(STATISTICS_ENTRIES[3]))
                .thenReturn(statisticsModuleStatisticsHistory1);

        List<StatisticsModuleStatisticsHistory> results =
                statisticsService.findAllByAttributeAndStatisticType(NAME_0, STATISTIC_TYPE);

        assertThat(
                results,
                contains(statisticsModuleStatisticsHistory1, statisticsModuleStatisticsHistory0));
    }

    @Test
    void findAllByAttribute() {
        when(statisticsEntryRepository.findAllByAttributeNameIgnoreCase(NAME_0))
                .thenReturn(List.of(STATISTICS_ENTRIES[0], STATISTICS_ENTRIES[3]));
        when(statisticsMapper.mapHistory(STATISTICS_ENTRIES[0]))
                .thenReturn(statisticsModuleStatisticsHistory0);
        when(statisticsMapper.mapHistory(STATISTICS_ENTRIES[3]))
                .thenReturn(statisticsModuleStatisticsHistory1);

        List<StatisticsModuleStatisticsHistory> results =
                statisticsService.findAllByAttributeAndStatisticType(NAME_0, "");

        assertThat(
                results,
                contains(statisticsModuleStatisticsHistory1, statisticsModuleStatisticsHistory0));
    }
}
