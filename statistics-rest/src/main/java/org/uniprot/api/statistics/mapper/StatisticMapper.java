package org.uniprot.api.statistics.mapper;

import java.util.Collection;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;
import org.uniprot.api.statistics.entity.EntryType;
import org.uniprot.api.statistics.entity.StatisticsCategory;
import org.uniprot.api.statistics.entity.UniprotkbStatisticsEntry;
import org.uniprot.api.statistics.model.*;

@Component
public class StatisticMapper {

    public EntryType map(String statisticType) {
        switch (StatisticType.valueOf(statisticType.toUpperCase())) {
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
        return StatisticAttributeImpl.builder()
                .name(entry.getAttributeName())
                .count(entry.getValueCount())
                .entryCount(entry.getEntryCount())
                .description(entry.getDescription())
                .statisticType(map(entry.getEntryType()))
                .build();
    }

    public StatisticCategory map(
            StatisticsCategory category, Collection<UniprotkbStatisticsEntry> entries) {
        return StatisticCategoryImpl.builder()
                .name(category.getCategory())
                .totalCount(
                        entries.stream().mapToLong(UniprotkbStatisticsEntry::getValueCount).sum())
                .totalEntryCount(
                        entries.stream().mapToLong(UniprotkbStatisticsEntry::getEntryCount).sum())
                .attributes(entries.stream().map(this::map).collect(Collectors.toList()))
                .build();
    }
}
