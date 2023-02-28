package org.uniprot.api.statistics.mapper;

import org.springframework.stereotype.Component;
import org.uniprot.api.statistics.StatisticAttributeFacetConfig;
import org.uniprot.api.statistics.entity.EntryType;
import org.uniprot.api.statistics.entity.UniprotkbStatisticsEntry;
import org.uniprot.api.statistics.model.StatisticAttribute;
import org.uniprot.api.statistics.model.StatisticAttributeImpl;
import org.uniprot.api.statistics.model.StatisticType;

import java.util.Optional;

@Component
public class StatisticMapper {
    private final StatisticAttributeFacetConfig statisticAttributeFacetConfig;

    public StatisticMapper(StatisticAttributeFacetConfig statisticAttributeFacetConfig) {
        this.statisticAttributeFacetConfig = statisticAttributeFacetConfig;
    }

    public EntryType map(StatisticType statisticType) {
        switch (statisticType) {
            case UNREVIEWED:
                return EntryType.TREMBL;
            case REVIEWED:
                return EntryType.SWISSPROT;
        }
        throw new IllegalArgumentException(
                String.format("Statistic type %s is not recognized", statisticType));
    }

    public StatisticType map(EntryType entryType) {
        switch (entryType) {
            case TREMBL:
                return StatisticType.UNREVIEWED;
            case SWISSPROT:
                return StatisticType.REVIEWED;
        }
        throw new IllegalArgumentException(
                String.format("Entry type %s is not recognized", entryType));
    }

    public StatisticAttribute map(UniprotkbStatisticsEntry entry) {
        System.out.println();
        return StatisticAttributeImpl.builder()
                .name(entry.getAttributeName())
                .count(entry.getValueCount())
                .entryCount(entry.getEntryCount())
                .description(entry.getDescription())
                .statisticType(map(entry.getEntryType()))
                .label(
                        Optional.ofNullable(
                                        statisticAttributeFacetConfig
                                                .getAttributes()
                                                .get(entry.getStatisticsCategoryId().getCategory().toLowerCase()))
                                .map(
                                        facetProperty ->
                                                facetProperty
                                                        .getValue()
                                                        .get(entry.getAttributeName().toLowerCase()))
                                .orElse(null))
                .build();
    }
}
