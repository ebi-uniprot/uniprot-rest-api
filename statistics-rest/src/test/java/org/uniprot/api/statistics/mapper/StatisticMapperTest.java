package org.uniprot.api.statistics.mapper;

import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.uniprot.api.common.repository.search.facet.FacetProperty;
import org.uniprot.api.statistics.StatisticAttributeFacetConfig;
import org.uniprot.api.statistics.entity.EntryType;
import org.uniprot.api.statistics.entity.UniprotkbStatisticsEntry;
import org.uniprot.api.statistics.model.StatisticAttribute;
import org.uniprot.api.statistics.model.StatisticCategory;
import org.uniprot.api.statistics.model.StatisticType;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.when;
import static org.uniprot.api.statistics.TestEntityGeneratorUtil.STATISTICS_CATEGORIES;
import static org.uniprot.api.statistics.TestEntityGeneratorUtil.STATISTICS_ENTRIES;

@ExtendWith(MockitoExtension.class)
class StatisticMapperTest {
    private static final Map<String, FacetProperty> FACET_MAP = new HashMap<>();
    public static final String LABEL_0 = "label0";
    @Mock private StatisticAttributeFacetConfig statisticAttributeFacetConfig;
    @InjectMocks private StatisticMapper statisticMapper;

    @BeforeEach
    void setUp() {
        FacetProperty facetProperty = new FacetProperty();
        Map<String, String> valueMap = new HashMap<>();
        valueMap.put(STATISTICS_ENTRIES[0].getAttributeName(), LABEL_0);
        facetProperty.setValue(valueMap);
        FACET_MAP.put(STATISTICS_CATEGORIES[0].getCategory(), facetProperty);
    }

    @Test
    void mapStatisticTypeToEntryType() {
        EntryType result = statisticMapper.map(StatisticType.REVIEWED);

        assertEquals(EntryType.SWISSPROT, result);
    }

    @Test
    void mapStatisticTypeToEntryType_whenCaseMixed() {
        EntryType result = statisticMapper.map(StatisticType.UNREVIEWED);

        assertEquals(EntryType.TREMBL, result);
    }

    @Test
    void mapEntryTypeToStatisticType() {
        StatisticType result = statisticMapper.map(EntryType.TREMBL);
        assertEquals(StatisticType.UNREVIEWED, result);

        result = statisticMapper.map(EntryType.SWISSPROT);
        assertEquals(StatisticType.REVIEWED, result);
    }

    @Test
    void mapUniprotkbStatisticsEntryToStatisticAttribute() {
        when(statisticAttributeFacetConfig.getAttributes()).thenReturn(FACET_MAP);
        UniprotkbStatisticsEntry statisticsEntry = STATISTICS_ENTRIES[0];

        StatisticAttribute statisticAttribute =
                statisticMapper.map(statisticsEntry, STATISTICS_CATEGORIES[0].getCategory());
        assertUniprotkbStatisticsEntryToStatisticAttributeMapping(
                statisticsEntry, statisticAttribute, LABEL_0);
    }

    @Test
    void mapUniprotkbStatisticsEntryToStatisticAttributeWhenLabelNotExist() {
        UniprotkbStatisticsEntry statisticsEntry = STATISTICS_ENTRIES[0];

        StatisticAttribute statisticAttribute =
                statisticMapper.map(statisticsEntry, STATISTICS_CATEGORIES[0].getCategory());
        assertUniprotkbStatisticsEntryToStatisticAttributeMapping(
                statisticsEntry, statisticAttribute, null);
    }

    private static void assertUniprotkbStatisticsEntryToStatisticAttributeMapping(
            UniprotkbStatisticsEntry expect, StatisticAttribute actual, String label) {
        assertSame(expect.getAttributeName(), actual.getName());
        assertEquals(expect.getValueCount(), actual.getCount());
        assertEquals(expect.getEntryCount(), actual.getEntryCount());
        assertSame(expect.getDescription(), actual.getDescription());
        assertEquals(StatisticType.REVIEWED, actual.getStatisticType());
        assertSame(label, actual.getLabel());
    }

    @Test
    void mapStatisticsCategoryAndUniprotkbStatisticsEntriesToStatisticCategory() {
        when(statisticAttributeFacetConfig.getAttributes()).thenReturn(FACET_MAP);
        StatisticCategory result =
                statisticMapper.map(
                        STATISTICS_CATEGORIES[0],
                        List.of(
                                STATISTICS_ENTRIES[0],
                                STATISTICS_ENTRIES[1],
                                STATISTICS_ENTRIES[3]));
        assertSame(STATISTICS_CATEGORIES[0].getCategory(), result.getName());
        assertEquals(
                STATISTICS_ENTRIES[0].getValueCount()
                        + STATISTICS_ENTRIES[1].getValueCount()
                        + STATISTICS_ENTRIES[3].getValueCount(),
                result.getTotalCount());
        assertEquals(
                STATISTICS_ENTRIES[0].getEntryCount()
                        + STATISTICS_ENTRIES[1].getEntryCount()
                        + STATISTICS_ENTRIES[3].getEntryCount(),
                result.getTotalEntryCount());
        assertThat(
                result.getAttributes(),
                containsInAnyOrder(
                        new UniprotkbStatisticsEntryToStatisticAttributeMappingMatcher(
                                STATISTICS_ENTRIES[0], LABEL_0),
                        new UniprotkbStatisticsEntryToStatisticAttributeMappingMatcher(
                                STATISTICS_ENTRIES[1], null),
                        new UniprotkbStatisticsEntryToStatisticAttributeMappingMatcher(
                                STATISTICS_ENTRIES[3], null)));
    }

    private static class UniprotkbStatisticsEntryToStatisticAttributeMappingMatcher
            extends TypeSafeMatcher<StatisticAttribute> {
        private final UniprotkbStatisticsEntry statisticsEntry;
        private final String label;

        public UniprotkbStatisticsEntryToStatisticAttributeMappingMatcher(
                UniprotkbStatisticsEntry statisticsEntry, String label) {
            this.statisticsEntry = statisticsEntry;
            this.label = label;
        }

        @Override
        protected boolean matchesSafely(StatisticAttribute item) {
            assertUniprotkbStatisticsEntryToStatisticAttributeMapping(statisticsEntry, item, label);
            return true;
        }

        @Override
        public void describeTo(Description description) {
            description.appendText("Given entry is similar with ").appendValue(statisticsEntry);
        }
    }
}
