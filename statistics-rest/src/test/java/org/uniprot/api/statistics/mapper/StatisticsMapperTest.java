package org.uniprot.api.statistics.mapper;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.uniprot.api.statistics.entity.EntryType;
import org.uniprot.api.statistics.entity.UniprotkbStatisticsEntry;
import org.uniprot.api.statistics.model.StatisticAttribute;
import org.uniprot.api.statistics.model.StatisticType;

import static org.junit.jupiter.api.Assertions.*;
import static org.uniprot.api.statistics.TestEntityGeneratorUtil.STATISTICS_ENTRIES;

class StatisticsMapperTest {
    private final StatisticsMapper statisticsMapper = new StatisticsMapper();

    @BeforeEach
    void setUp() {
    }

    @Test
    void mapStatisticTypeToEntryType() {
        EntryType result = statisticsMapper.map("reviewed");

        assertEquals(EntryType.SWISSPROT, result);
    }

    @Test
    void mapStatisticTypeToEntryType_whenCaseMixed() {
        EntryType result = statisticsMapper.map("UnrEvieweD");

        assertEquals(EntryType.TREMBL, result);
    }

    @Test
    void mapStatisticTypeToEntryType_whenNoMatch() {
        assertThrows(IllegalArgumentException.class, () -> statisticsMapper.map("NoMatch"));
    }

    @Test
    void mapEntryTypeToStatisticType() {
        StatisticType result = statisticsMapper.map(EntryType.TREMBL);
        assertEquals(StatisticType.UNREVIEWED, result);

        result = statisticsMapper.map(EntryType.SWISSPROT);
        assertEquals(StatisticType.REVIEWED, result);
    }

    @Test
    void mapUniprotkbStatisticsEntryToStatisticAttribute() {
        UniprotkbStatisticsEntry statisticsEntry = STATISTICS_ENTRIES[0];
        StatisticAttribute statisticAttribute = statisticsMapper.map(statisticsEntry);
        assertUniprotkbStatisticsEntryToStatisticAttributeMapping(statisticsEntry, statisticAttribute);
    }

    private static void assertUniprotkbStatisticsEntryToStatisticAttributeMapping(UniprotkbStatisticsEntry expect, StatisticAttribute actual) {
        assertSame(expect.getAttributeName(), actual.getName());
        assertSame(expect.getValueCount(), actual.getCount());
        assertSame(expect.getEntryCount(), actual.getEntryCount());
        assertSame(expect.getDescription(), actual.getDescription());
        assertEquals(StatisticType.REVIEWED, actual.getStatisticType());
    }
}