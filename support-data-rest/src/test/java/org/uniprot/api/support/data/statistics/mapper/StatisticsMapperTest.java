package org.uniprot.api.support.data.statistics.mapper;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.uniprot.api.common.repository.search.facet.FacetProperty;
import org.uniprot.api.support.data.statistics.StatisticsAttributeConfig;
import org.uniprot.api.support.data.statistics.entity.EntryType;
import org.uniprot.api.support.data.statistics.entity.UniProtKBStatisticsEntry;
import org.uniprot.api.support.data.statistics.model.StatisticsModuleStatisticsAttribute;
import org.uniprot.api.support.data.statistics.model.StatisticsModuleStatisticsHistory;
import org.uniprot.api.support.data.statistics.model.StatisticsModuleStatisticsType;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.when;
import static org.uniprot.api.support.data.statistics.TestEntityGeneratorUtil.*;
import static org.uniprot.api.support.data.statistics.model.StatisticsModuleStatisticsType.REVIEWED;
import static org.uniprot.api.support.data.statistics.model.StatisticsModuleStatisticsType.UNREVIEWED;

@ExtendWith(MockitoExtension.class)
class StatisticsMapperTest {
    private static final Map<String, FacetProperty> FACET_MAP = new HashMap<>();
    @Mock
    private StatisticsAttributeConfig statisticsAttributeConfig;
    @InjectMocks
    private StatisticsMapper statisticsMapper;

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
        EntryType result = statisticsMapper.map(REVIEWED);

        assertEquals(EntryType.SWISSPROT, result);
    }

    @Test
    void mapStatisticTypeToEntryType_whenCaseMixed() {
        EntryType result = statisticsMapper.map(UNREVIEWED);

        assertEquals(EntryType.TREMBL, result);
    }

    @Test
    void mapEntryTypeToStatisticType() {
        StatisticsModuleStatisticsType result = statisticsMapper.map(EntryType.TREMBL);
        assertEquals(UNREVIEWED, result);

        result = statisticsMapper.map(EntryType.SWISSPROT);
        assertEquals(REVIEWED, result);
    }

    @Test
    void mapUniProtKBStatisticsEntryToStatisticAttribute() {
        when(statisticsAttributeConfig.getAttributes()).thenReturn(FACET_MAP);
        UniProtKBStatisticsEntry statisticsEntry = STATISTICS_ENTRIES[0];

        StatisticsModuleStatisticsAttribute statisticsModuleStatisticsAttribute =
                statisticsMapper.map(statisticsEntry);
        assertUniprotkbStatisticsEntryToStatisticAttributeMapping(
                statisticsEntry, statisticsModuleStatisticsAttribute, LABEL_0);
    }

    @Test
    void mapUniProtKBStatisticsEntryToStatisticAttributeWhenLabelNotExist() {
        UniProtKBStatisticsEntry statisticsEntry = STATISTICS_ENTRIES[0];

        StatisticsModuleStatisticsAttribute statisticsModuleStatisticsAttribute =
                statisticsMapper.map(statisticsEntry);
        assertUniprotkbStatisticsEntryToStatisticAttributeMapping(
                statisticsEntry, statisticsModuleStatisticsAttribute, null);
    }

    @Test
    void mapHistory() {
        UniProtKBStatisticsEntry statisticsEntry = STATISTICS_ENTRIES[0];

        StatisticsModuleStatisticsHistory history = statisticsMapper.mapHistory(statisticsEntry);

        assertSame(REVIEWED, history.getStatisticsType());
        assertSame(REL_0, history.getReleaseName());
        assertSame(DATES[0], history.getReleaseDate());
        assertSame(ENTRY_COUNTS[0], history.getEntryCount());
        assertSame(VALUE_COUNTS[0], history.getValueCount());
    }

    private static void assertUniprotkbStatisticsEntryToStatisticAttributeMapping(
            UniProtKBStatisticsEntry expect,
            StatisticsModuleStatisticsAttribute actual,
            String label) {
        assertSame(expect.getAttributeName(), actual.getName());
        assertEquals(expect.getValueCount().longValue(), actual.getCount());
        assertEquals(expect.getEntryCount().longValue(), actual.getEntryCount());
        assertSame(expect.getDescription(), actual.getDescription());
        assertSame(REVIEWED, actual.getStatisticsType());
        assertSame(label, actual.getLabel());
    }
}
