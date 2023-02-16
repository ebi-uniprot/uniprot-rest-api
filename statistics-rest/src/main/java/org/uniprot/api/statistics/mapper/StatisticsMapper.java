package org.uniprot.api.statistics.mapper;

import org.springframework.stereotype.Component;
import org.uniprot.api.statistics.entity.EntryType;
import org.uniprot.api.statistics.entity.UniprotkbStatisticsEntry;
import org.uniprot.api.statistics.model.StatisticAttribute;
import org.uniprot.api.statistics.model.StatisticCategory;
import org.uniprot.api.statistics.model.StatisticType;

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

    public StatisticAttribute map(UniprotkbStatisticsEntry entry) {
        return StatisticAttribute.builder()
                .name(entry.getAttributeName())
                .count(entry.getValueCount())
                .entryCount(entry.getEntryCount())
                .description(entry.getDescription())
                .statisticType(map(entry.getEntryType()))
                .build();
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

    public Collection<StatisticCategory> map(Collection<UniprotkbStatisticsEntry> entries) {
        return entries.stream()
                .collect(Collectors.groupingBy(UniprotkbStatisticsEntry::getStatisticsCategoryId))
                .entrySet()
                .stream()
                .map(entry -> map(entry.getKey().getCategory(), entry.getValue()))
                .collect(Collectors.toList());
    }

    public StatisticCategory map(String category, Collection<UniprotkbStatisticsEntry> entries) {
        return StatisticCategory.builder()
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
