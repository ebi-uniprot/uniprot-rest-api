package org.uniprot.api.support.data.statistics.service;

import org.springframework.stereotype.Service;
import org.uniprot.api.support.data.statistics.entity.StatisticsCategory;
import org.uniprot.api.support.data.statistics.entity.UniprotKBStatisticsEntry;
import org.uniprot.api.support.data.statistics.mapper.StatisticsMapper;
import org.uniprot.api.support.data.statistics.model.StatisticsModuleStatisticsAttribute;
import org.uniprot.api.support.data.statistics.model.StatisticsModuleStatisticsCategory;
import org.uniprot.api.support.data.statistics.model.StatisticsModuleStatisticsCategoryImpl;
import org.uniprot.api.support.data.statistics.model.StatisticsModuleStatisticsType;
import org.uniprot.api.support.data.statistics.repository.StatisticsCategoryRepository;
import org.uniprot.api.support.data.statistics.repository.UniprotKBStatisticsEntryRepository;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class StatisticsServiceImpl implements StatisticsService {

    private final UniprotKBStatisticsEntryRepository statisticsEntryRepository;
    private final StatisticsCategoryRepository statisticsCategoryRepository;
    private final StatisticsMapper statisticsMapper;

    public StatisticsServiceImpl(
            UniprotKBStatisticsEntryRepository uniprotkbStatisticsEntryRepository,
            StatisticsCategoryRepository statisticsCategoryRepository,
            StatisticsMapper statisticsMapper) {
        this.statisticsEntryRepository = uniprotkbStatisticsEntryRepository;
        this.statisticsCategoryRepository = statisticsCategoryRepository;
        this.statisticsMapper = statisticsMapper;
    }

    @Override
    public List<StatisticsModuleStatisticsCategory> findAllByVersionAndStatisticTypeAndCategoryIn(
            String version, String statisticType, Set<String> categories) {
        List<UniprotKBStatisticsEntry> entries;
        if (categories.isEmpty()) {
            entries =
                    statisticsEntryRepository.findAllByReleaseNameAndEntryType(
                            version, statisticsMapper.map(getStatisticType(statisticType)));
        } else {
            entries =
                    statisticsEntryRepository
                            .findAllByReleaseNameAndEntryTypeAndStatisticsCategoryIn(
                                    version,
                                    statisticsMapper.map(getStatisticType(statisticType)),
                                    getCategories(categories));
        }
        return entries.stream()
                .collect(Collectors.groupingBy(UniprotKBStatisticsEntry::getStatisticsCategory))
                .entrySet()
                .stream()
                .map(this::buildStatisticsModuleStatisticsCategory)
                .collect(Collectors.toList());
    }

    private StatisticsModuleStatisticsCategoryImpl buildStatisticsModuleStatisticsCategory(
            Map.Entry<StatisticsCategory, List<UniprotKBStatisticsEntry>> entry) {
        return StatisticsModuleStatisticsCategoryImpl.builder()
                .categoryName(entry.getKey().getCategory())
                .totalCount(getTotalCount(entry))
                .label(entry.getKey().getLabel())
                .searchField(entry.getKey().getSearchField())
                .items(getItems(entry))
                .build();
    }

    private List<StatisticsModuleStatisticsAttribute> getItems(
            Map.Entry<StatisticsCategory, List<UniprotKBStatisticsEntry>> entry) {
        return entry.getValue().stream().map(statisticsMapper::map).collect(Collectors.toList());
    }

    private static long getTotalCount(
            Map.Entry<StatisticsCategory, List<UniprotKBStatisticsEntry>> entry) {
        return entry.getValue().stream().mapToLong(UniprotKBStatisticsEntry::getValueCount).sum();
    }

    private Set<StatisticsCategory> getCategories(Set<String> categories) {
        return categories.stream()
                .map(
                        cat ->
                                statisticsCategoryRepository
                                        .findByCategoryIgnoreCase(cat)
                                        .orElseThrow(
                                                () ->
                                                        new IllegalArgumentException(
                                                                String.format(
                                                                        "Invalid Statistic Category: %s",
                                                                        cat))))
                .collect(Collectors.toSet());
    }

    private static StatisticsModuleStatisticsType getStatisticType(String statisticType) {
        try {
            return StatisticsModuleStatisticsType.valueOf(statisticType.toUpperCase());
        } catch (Exception e) {
            throw new IllegalArgumentException(
                    String.format("Invalid Statistic Type: %s", statisticType));
        }
    }
}
