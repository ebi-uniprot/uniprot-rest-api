package org.uniprot.api.statistics.mapper;

import org.springframework.stereotype.Component;
import org.uniprot.api.statistics.entity.EntryType;
import org.uniprot.api.statistics.entity.UniprotkbStatisticsEntry;
import org.uniprot.api.statistics.model.*;

import java.util.Collection;
import java.util.stream.Collectors;

@Component
public class StatisticsMapper {

    public EntryType map(StatisticType statisticsEntryType) {
        switch (statisticsEntryType) {
            case TREMBL:
                return EntryType.TREMBL;
            case SWISSPROT:
                return EntryType.SWISSPROT;
        }
        throw new IllegalArgumentException(
                String.format("Statistic type %s is not recognized", statisticsEntryType));
    }

    private StatisticType map(EntryType entryType) {
        switch (entryType) {
            case TREMBL:
                return StatisticType.TREMBL;
            case SWISSPROT:
                return StatisticType.SWISSPROT;
        }
        throw new IllegalArgumentException(
                String.format("Entry type %s is not recognized", entryType));
    }

    public StatisticAttribute map(UniprotkbStatisticsEntry entry) {
        return StatisticAttributeImpl.builder()
                .name(entry.getAttributeName())
                .count(entry.getValueCount())
                .entryCount(entry.getEntryCount())
                .description(entry.getDescription())
                .statisticType(map(entry.getEntryType()))
                .build();
    }

    public StatisticCategory map(String category, Collection<UniprotkbStatisticsEntry> entries) {
        return StatisticCategoryImpl.builder()
                .name(category)
                .totalCount(
                        entries.stream()
                                .mapToLong(
                                        UniprotkbStatisticsEntry
                                                ::getValueCount)
                                .sum())
                .totalEntryCount(
                        entries.stream()
                                .mapToLong(
                                        UniprotkbStatisticsEntry
                                                ::getEntryCount)
                                .sum())
                .attributes(
                        entries.stream()
                                .map(this::map)
                                .collect(Collectors.toList()))
                .build();
    }
}
