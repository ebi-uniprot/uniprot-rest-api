package org.uniprot.api.statistics.mapper;

import org.springframework.stereotype.Component;
import org.uniprot.api.statistics.StatisticAttributeConfig;
import org.uniprot.api.statistics.entity.EntryType;
import org.uniprot.api.statistics.entity.UniprotkbStatisticsEntry;
import org.uniprot.api.statistics.model.StatisticsModuleStatisticAttribute;
import org.uniprot.api.statistics.model.StatisticsModuleStatisticAttributeImpl;
import org.uniprot.api.statistics.model.StatisticModuleStatisticType;

import java.util.Optional;

@Component
public class StatisticMapper {
    private final StatisticAttributeConfig statisticAttributeConfig;

    public StatisticMapper(StatisticAttributeConfig statisticAttributeConfig) {
        this.statisticAttributeConfig = statisticAttributeConfig;
    }

    public EntryType map(StatisticModuleStatisticType statisticModuleStatisticType) {
        switch (statisticModuleStatisticType) {
            case UNREVIEWED:
                return EntryType.TREMBL;
            case REVIEWED:
                return EntryType.SWISSPROT;
        }
        throw new IllegalArgumentException(
                String.format("Statistic type %s is not recognized", statisticModuleStatisticType));
    }

    public StatisticModuleStatisticType map(EntryType entryType) {
        switch (entryType) {
            case TREMBL:
                return StatisticModuleStatisticType.UNREVIEWED;
            case SWISSPROT:
                return StatisticModuleStatisticType.REVIEWED;
        }
        throw new IllegalArgumentException(
                String.format("Entry type %s is not recognized", entryType));
    }

    public StatisticsModuleStatisticAttribute map(UniprotkbStatisticsEntry entry) {
        System.out.println();
        return StatisticsModuleStatisticAttributeImpl.builder()
                .name(entry.getAttributeName())
                .count(entry.getValueCount())
                .entryCount(entry.getEntryCount())
                .description(entry.getDescription())
                .label(
                        Optional.ofNullable(
                                        statisticAttributeConfig
                                                .getAttributes()
                                                .get(entry.getStatisticsCategory().getCategory().toLowerCase()))
                                .map(
                                        facetProperty ->
                                                facetProperty
                                                        .getValue()
                                                        .get(entry.getAttributeName().toLowerCase()))
                                .orElse(null))
                .build();
    }
}
