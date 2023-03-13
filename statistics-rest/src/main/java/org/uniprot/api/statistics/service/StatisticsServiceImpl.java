package org.uniprot.api.statistics.service;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.uniprot.api.statistics.entity.StatisticsCategory;
import org.uniprot.api.statistics.entity.UniprotkbStatisticsEntry;
import org.uniprot.api.statistics.mapper.StatisticsMapper;
import org.uniprot.api.statistics.model.StatisticsModuleStatisticsCategory;
import org.uniprot.api.statistics.model.StatisticsModuleStatisticsCategoryImpl;
import org.uniprot.api.statistics.model.StatisticsModuleStatisticsType;
import org.uniprot.api.statistics.repository.StatisticsCategoryRepository;
import org.uniprot.api.statistics.repository.UniprotkbStatisticsEntryRepository;

@Service
public class StatisticsServiceImpl implements StatisticsService {

    private final UniprotkbStatisticsEntryRepository statisticsEntryRepository;
    private final StatisticsCategoryRepository statisticsCategoryRepository;
    private final StatisticsMapper statisticsMapper;

    public StatisticsServiceImpl(
            UniprotkbStatisticsEntryRepository uniprotkbStatisticsEntryRepository,
            StatisticsCategoryRepository statisticsCategoryRepository,
            StatisticsMapper statisticsMapper) {
        this.statisticsEntryRepository = uniprotkbStatisticsEntryRepository;
        this.statisticsCategoryRepository = statisticsCategoryRepository;
        this.statisticsMapper = statisticsMapper;
    }

    @Override
    public List<StatisticsModuleStatisticsCategory> findAllByVersionAndStatisticTypeAndCategoryIn(
            String version, String statisticType, Set<String> categories) {
        List<UniprotkbStatisticsEntry> entries;
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
                .collect(Collectors.groupingBy(UniprotkbStatisticsEntry::getStatisticsCategory))
                .entrySet()
                .stream()
                .map(this::buildStatisticsModuleStatisticsCategory)
                .collect(Collectors.toList());
    }

    private StatisticsModuleStatisticsCategoryImpl buildStatisticsModuleStatisticsCategory(
            Map.Entry<StatisticsCategory, List<UniprotkbStatisticsEntry>> entry) {
        return StatisticsModuleStatisticsCategoryImpl.builder()
                .categoryName(entry.getKey().getCategory())
                .totalCount(
                        entry.getValue().stream()
                                .mapToLong(UniprotkbStatisticsEntry::getValueCount)
                                .sum())
                .label(entry.getKey().getLabel())
                .searchField(entry.getKey().getSearchField())
                .items(
                        entry.getValue().stream()
                                .map(statisticsMapper::map)
                                .collect(Collectors.toList()))
                .build();
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
