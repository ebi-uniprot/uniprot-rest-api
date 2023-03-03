package org.uniprot.api.statistics.mapper;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.uniprot.api.common.repository.search.facet.FacetProperty;
import org.uniprot.api.statistics.StatisticAttributeConfig;
import org.uniprot.api.statistics.entity.EntryType;
import org.uniprot.api.statistics.entity.UniprotkbStatisticsEntry;
import org.uniprot.api.statistics.model.StatisticsModuleStatisticAttribute;
import org.uniprot.api.statistics.model.StatisticModuleStatisticType;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.when;
import static org.uniprot.api.statistics.TestEntityGeneratorUtil.STATISTICS_CATEGORIES;
import static org.uniprot.api.statistics.TestEntityGeneratorUtil.STATISTICS_ENTRIES;

@ExtendWith(MockitoExtension.class)
class StatisticMapperTest {
    private static final Map<String, FacetProperty> FACET_MAP = new HashMap<>();
    public static final String LABEL_0 = "label0";
    @Mock
    private StatisticAttributeConfig statisticAttributeConfig;
    @InjectMocks
    private StatisticMapper statisticMapper;

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
        EntryType result = statisticMapper.map(StatisticModuleStatisticType.REVIEWED);

        assertEquals(EntryType.SWISSPROT, result);
    }

    @Test
    void mapStatisticTypeToEntryType_whenCaseMixed() {
        EntryType result = statisticMapper.map(StatisticModuleStatisticType.UNREVIEWED);

        assertEquals(EntryType.TREMBL, result);
    }

    @Test
    void mapEntryTypeToStatisticType() {
        StatisticModuleStatisticType result = statisticMapper.map(EntryType.TREMBL);
        assertEquals(StatisticModuleStatisticType.UNREVIEWED, result);

        result = statisticMapper.map(EntryType.SWISSPROT);
        assertEquals(StatisticModuleStatisticType.REVIEWED, result);
    }

    @Test
    void mapUniprotkbStatisticsEntryToStatisticAttribute() {
        when(statisticAttributeConfig.getAttributes()).thenReturn(FACET_MAP);
        UniprotkbStatisticsEntry statisticsEntry = STATISTICS_ENTRIES[0];

        StatisticsModuleStatisticAttribute statisticsModuleStatisticAttribute =
                statisticMapper.map(statisticsEntry);
        assertUniprotkbStatisticsEntryToStatisticAttributeMapping(
                statisticsEntry, statisticsModuleStatisticAttribute, LABEL_0);
    }

    @Test
    void mapUniprotkbStatisticsEntryToStatisticAttributeWhenLabelNotExist() {
        UniprotkbStatisticsEntry statisticsEntry = STATISTICS_ENTRIES[0];

        StatisticsModuleStatisticAttribute statisticsModuleStatisticAttribute =
                statisticMapper.map(statisticsEntry);
        assertUniprotkbStatisticsEntryToStatisticAttributeMapping(
                statisticsEntry, statisticsModuleStatisticAttribute, null);
    }

    private static void assertUniprotkbStatisticsEntryToStatisticAttributeMapping(
            UniprotkbStatisticsEntry expect, StatisticsModuleStatisticAttribute actual, String label) {
        assertSame(expect.getAttributeName(), actual.getName());
        assertEquals(expect.getValueCount(), actual.getCount());
        assertEquals(expect.getEntryCount(), actual.getEntryCount());
        assertSame(expect.getDescription(), actual.getDescription());
        assertSame(label, actual.getLabel());
    }

}
