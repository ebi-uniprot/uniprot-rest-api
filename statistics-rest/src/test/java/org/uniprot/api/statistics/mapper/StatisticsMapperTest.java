package org.uniprot.api.statistics.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.when;
import static org.uniprot.api.statistics.TestEntityGeneratorUtil.*;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.uniprot.api.common.repository.search.facet.FacetProperty;
import org.uniprot.api.statistics.StatisticsAttributeConfig;
import org.uniprot.api.statistics.entity.EntryType;
import org.uniprot.api.statistics.entity.UniprotkbStatisticsEntry;
import org.uniprot.api.statistics.model.StatisticsModuleStatisticsAttribute;
import org.uniprot.api.statistics.model.StatisticsModuleStatisticsType;

@ExtendWith(MockitoExtension.class)
class StatisticsMapperTest {
    private static final Map<String, FacetProperty> FACET_MAP = new HashMap<>();
    @Mock private StatisticsAttributeConfig statisticsAttributeConfig;
    @InjectMocks private StatisticsMapper statisticsMapper;

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
        EntryType result = statisticsMapper.map(StatisticsModuleStatisticsType.REVIEWED);

        assertEquals(EntryType.SWISSPROT, result);
    }

    @Test
    void mapStatisticTypeToEntryType_whenCaseMixed() {
        EntryType result = statisticsMapper.map(StatisticsModuleStatisticsType.UNREVIEWED);

        assertEquals(EntryType.TREMBL, result);
    }

    @Test
    void mapEntryTypeToStatisticType() {
        StatisticsModuleStatisticsType result = statisticsMapper.map(EntryType.TREMBL);
        assertEquals(StatisticsModuleStatisticsType.UNREVIEWED, result);

        result = statisticsMapper.map(EntryType.SWISSPROT);
        assertEquals(StatisticsModuleStatisticsType.REVIEWED, result);
    }

    @Test
    void mapUniprotkbStatisticsEntryToStatisticAttribute() {
        when(statisticsAttributeConfig.getAttributes()).thenReturn(FACET_MAP);
        UniprotkbStatisticsEntry statisticsEntry = STATISTICS_ENTRIES[0];

        StatisticsModuleStatisticsAttribute statisticsModuleStatisticsAttribute =
                statisticsMapper.map(statisticsEntry);
        assertUniprotkbStatisticsEntryToStatisticAttributeMapping(
                statisticsEntry, statisticsModuleStatisticsAttribute, LABEL_0);
    }

    @Test
    void mapUniprotkbStatisticsEntryToStatisticAttributeWhenLabelNotExist() {
        UniprotkbStatisticsEntry statisticsEntry = STATISTICS_ENTRIES[0];

        StatisticsModuleStatisticsAttribute statisticsModuleStatisticsAttribute =
                statisticsMapper.map(statisticsEntry);
        assertUniprotkbStatisticsEntryToStatisticAttributeMapping(
                statisticsEntry, statisticsModuleStatisticsAttribute, null);
    }

    private static void assertUniprotkbStatisticsEntryToStatisticAttributeMapping(
            UniprotkbStatisticsEntry expect,
            StatisticsModuleStatisticsAttribute actual,
            String label) {
        assertSame(expect.getAttributeName(), actual.getName());
        assertEquals(expect.getValueCount(), actual.getCount());
        assertEquals(expect.getEntryCount(), actual.getEntryCount());
        assertSame(expect.getDescription(), actual.getDescription());
        assertSame(label, actual.getLabel());
    }
}
