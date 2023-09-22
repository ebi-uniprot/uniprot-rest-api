package org.uniprot.api.support.data.statistics.mapper;

import java.util.Optional;

import org.springframework.stereotype.Component;
import org.uniprot.api.support.data.statistics.StatisticsAttributeConfig;
import org.uniprot.api.support.data.statistics.entity.EntryType;
import org.uniprot.api.support.data.statistics.entity.UniprotKBStatisticsEntry;
import org.uniprot.api.support.data.statistics.model.StatisticsModuleStatisticsAttribute;
import org.uniprot.api.support.data.statistics.model.StatisticsModuleStatisticsAttributeImpl;
import org.uniprot.api.support.data.statistics.model.StatisticsModuleStatisticsType;

@Component
public class StatisticsMapper {
    private final StatisticsAttributeConfig statisticsAttributeConfig;

    public StatisticsMapper(StatisticsAttributeConfig statisticsAttributeConfig) {
        this.statisticsAttributeConfig = statisticsAttributeConfig;
    }

    public EntryType map(StatisticsModuleStatisticsType statisticsModuleStatisticsType) {
        switch (statisticsModuleStatisticsType) {
            case UNREVIEWED:
                return EntryType.TREMBL;
            case REVIEWED:
                return EntryType.SWISSPROT;
        }
        throw new IllegalArgumentException(
                String.format(
                        "Statistic type %s is not recognized", statisticsModuleStatisticsType));
    }

    public StatisticsModuleStatisticsType map(EntryType entryType) {
        switch (entryType) {
            case TREMBL:
                return StatisticsModuleStatisticsType.UNREVIEWED;
            case SWISSPROT:
                return StatisticsModuleStatisticsType.REVIEWED;
        }
        throw new IllegalArgumentException(
                String.format("Entry type %s is not recognized", entryType));
    }

    public StatisticsModuleStatisticsAttribute map(UniprotKBStatisticsEntry entry) {
        return StatisticsModuleStatisticsAttributeImpl.builder()
                .name(entry.getAttributeName())
                .count(entry.getValueCount())
                .entryCount(entry.getEntryCount())
                .description(entry.getDescription())
                .label(getLabel(entry))
                .build();
    }

    private String getLabel(UniprotKBStatisticsEntry entry) {
        return Optional.ofNullable(
                        statisticsAttributeConfig
                                .getAttributes()
                                .get(entry.getStatisticsCategory().getCategory().toLowerCase()))
                .map(
                        facetProperty ->
                                facetProperty
                                        .getValue()
                                        .get(entry.getAttributeName().toLowerCase()))
                .orElse(null);
    }
}
