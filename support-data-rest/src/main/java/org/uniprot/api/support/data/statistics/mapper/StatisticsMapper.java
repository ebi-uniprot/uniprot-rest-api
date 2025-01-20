package org.uniprot.api.support.data.statistics.mapper;

import java.util.Optional;

import org.springframework.stereotype.Component;
import org.uniprot.api.support.data.statistics.StatisticsAttributeConfig;
import org.uniprot.api.support.data.statistics.entity.EntryType;
import org.uniprot.api.support.data.statistics.entity.UniProtKBStatisticsEntry;
import org.uniprot.api.support.data.statistics.model.*;

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
        if (entryType == null) {
            return null;
        }
        return switch (entryType) {
            case TREMBL -> StatisticsModuleStatisticsType.UNREVIEWED;
            case SWISSPROT -> StatisticsModuleStatisticsType.REVIEWED;
        };
    }

    public StatisticsModuleStatisticsAttribute map(
            UniProtKBStatisticsEntry entry, String attributeQuery) {
        return StatisticsModuleStatisticsAttributeImpl.builder()
                .name(entry.getAttributeName())
                .count(entry.getValueCount())
                .query(attributeQuery)
                .entryCount(entry.getEntryCount())
                .statisticsType(map(entry.getEntryType()))
                .description(entry.getDescription())
                .label(getLabel(entry))
                .build();
    }

    public StatisticsModuleStatisticsHistory mapHistory(UniProtKBStatisticsEntry entry) {
        return StatisticsModuleStatisticsHistoryImpl.builder()
                .statisticsType(map(entry.getEntryType()))
                .releaseName(entry.getUniProtRelease().getName())
                .releaseDate(entry.getUniProtRelease().getDate())
                .valueCount(entry.getValueCount())
                .entryCount(entry.getEntryCount())
                .build();
    }

    private String getLabel(UniProtKBStatisticsEntry entry) {
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
