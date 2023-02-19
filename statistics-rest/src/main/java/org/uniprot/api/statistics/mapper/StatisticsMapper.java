package org.uniprot.api.statistics.mapper;

import org.springframework.stereotype.Component;
import org.uniprot.api.statistics.entity.EntryType;
import org.uniprot.api.statistics.entity.UniprotkbStatisticsEntry;
import org.uniprot.api.statistics.model.*;

import java.util.Collection;
import java.util.stream.Collectors;

@Component
public class StatisticsMapper {

    public EntryType map(String statisticType) {
        switch (StatisticType.valueOf(statisticType.toUpperCase())) {
            case UNREVIWED:
                return EntryType.TREMBL;
            case REVIEWED:
                return EntryType.SWISSPROT;
        }
        throw new IllegalArgumentException(
                String.format("Statistic type %s is not recognized", statisticType));
    }

    private StatisticType map(EntryType entryType) {
        switch (entryType) {
            case TREMBL:
                return StatisticType.UNREVIWED;
            case SWISSPROT:
                return StatisticType.REVIEWED;
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

    public Collection<StatisticCategory> map(Collection<UniprotkbStatisticsEntry> entries) {
        return entries.stream()
                .collect(Collectors.groupingBy(UniprotkbStatisticsEntry::getStatisticsCategoryId))
                .keySet()
                .stream()
                .map(
                        entry ->
                                StatisticCategoryImpl.builder()
                                        .name(entry.getCategory())
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
                                        .build())
                .collect(Collectors.toList());
    }
}
