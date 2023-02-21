package org.uniprot.api.statistics.mapper;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.jupiter.api.Assertions.*;
import static org.uniprot.api.statistics.TestEntityGeneratorUtil.STATISTICS_CATEGORIES;
import static org.uniprot.api.statistics.TestEntityGeneratorUtil.STATISTICS_ENTRIES;

import java.util.List;

import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;
import org.junit.jupiter.api.Test;
import org.uniprot.api.statistics.entity.EntryType;
import org.uniprot.api.statistics.entity.UniprotkbStatisticsEntry;
import org.uniprot.api.statistics.model.StatisticAttribute;
import org.uniprot.api.statistics.model.StatisticCategory;
import org.uniprot.api.statistics.model.StatisticType;

class StatisticMapperTest {
    private final StatisticMapper statisticMapper = new StatisticMapper();

    @Test
    void mapStatisticTypeToEntryType() {
        EntryType result = statisticMapper.map("reviewed");

        assertEquals(EntryType.SWISSPROT, result);
    }

    @Test
    void mapStatisticTypeToEntryType_whenCaseMixed() {
        EntryType result = statisticMapper.map("UnrEvieweD");

        assertEquals(EntryType.TREMBL, result);
    }

    @Test
    void mapStatisticTypeToEntryType_whenNoMatch() {
        assertThrows(IllegalArgumentException.class, () -> statisticMapper.map("NoMatch"));
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
        UniprotkbStatisticsEntry statisticsEntry = STATISTICS_ENTRIES[0];
        StatisticAttribute statisticAttribute = statisticMapper.map(statisticsEntry);
        assertUniprotkbStatisticsEntryToStatisticAttributeMapping(
                statisticsEntry, statisticAttribute);
    }

    private static void assertUniprotkbStatisticsEntryToStatisticAttributeMapping(
            UniprotkbStatisticsEntry expect, StatisticAttribute actual) {
        assertSame(expect.getAttributeName(), actual.getName());
        assertEquals(expect.getValueCount(), actual.getCount());
        assertEquals(expect.getEntryCount(), actual.getEntryCount());
        assertSame(expect.getDescription(), actual.getDescription());
        assertEquals(StatisticType.REVIEWED, actual.getStatisticType());
    }

    @Test
    void mapStatisticsCategoryAndUniprotkbStatisticsEntriesToStatisticCategory() {
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
                                STATISTICS_ENTRIES[0]),
                        new UniprotkbStatisticsEntryToStatisticAttributeMappingMatcher(
                                STATISTICS_ENTRIES[1]),
                        new UniprotkbStatisticsEntryToStatisticAttributeMappingMatcher(
                                STATISTICS_ENTRIES[3])));
    }

    private static class UniprotkbStatisticsEntryToStatisticAttributeMappingMatcher
            extends TypeSafeMatcher<StatisticAttribute> {
        private final UniprotkbStatisticsEntry statisticsEntry;

        public UniprotkbStatisticsEntryToStatisticAttributeMappingMatcher(
                UniprotkbStatisticsEntry statisticsEntry) {
            this.statisticsEntry = statisticsEntry;
        }

        @Override
        protected boolean matchesSafely(StatisticAttribute item) {
            assertUniprotkbStatisticsEntryToStatisticAttributeMapping(statisticsEntry, item);
            return true;
        }

        @Override
        public void describeTo(Description description) {
            description.appendText("Given entry is similar with ").appendValue(statisticsEntry);
        }
    }
}
