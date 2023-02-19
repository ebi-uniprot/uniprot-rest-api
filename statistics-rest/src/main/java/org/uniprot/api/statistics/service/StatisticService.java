package org.uniprot.api.statistics.service;

import org.springframework.stereotype.Service;
import org.uniprot.api.statistics.entity.UniprotkbStatisticsEntry;
import org.uniprot.api.statistics.mapper.StatisticsMapper;
import org.uniprot.api.statistics.model.StatisticCategory;
import org.uniprot.api.statistics.repository.StatisticsCategoryRepository;
import org.uniprot.api.statistics.repository.UniprotkbStatisticsEntryRepository;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class StatisticService {

    private final UniprotkbStatisticsEntryRepository statisticsEntryRepository;
    private final StatisticsCategoryRepository statisticsCategoryRepository;
    private final StatisticsMapper statisticsMapper;

    public StatisticService(
            UniprotkbStatisticsEntryRepository uniprotkbStatisticsEntryRepository,
            StatisticsCategoryRepository statisticsCategoryRepository,
            StatisticsMapper statisticsMapper) {
        this.statisticsEntryRepository = uniprotkbStatisticsEntryRepository;
        this.statisticsCategoryRepository = statisticsCategoryRepository;
        this.statisticsMapper = statisticsMapper;
    }

    public Collection<StatisticCategory> findAllByVersionAndStatisticTypeAndCategoryIn(
            String version, String statisticsType, Collection<String> categories) {
        List<UniprotkbStatisticsEntry> entries;
        if (categories.isEmpty()) {
            entries =
                    statisticsEntryRepository.findAllByReleaseNameAndEntryType(
                            version, statisticsMapper.map(statisticsType));
        } else {
            entries =
                    statisticsEntryRepository
                            .findAllByReleaseNameAndEntryTypeAndStatisticsCategoryIdIn(
                                    version,
                                    statisticsMapper.map(statisticsType),
                                    categories.stream()
                                            .map(
                                                    cat ->
                                                            statisticsCategoryRepository
                                                                    .findByCategory(cat)
                                                                    .orElseThrow(
                                                                            () ->
                                                                                    new IllegalArgumentException(
                                                                                            "Category "
                                                                                                    + categories
                                                                                                    + " not found")))
                                            .collect(Collectors.toList()));
        }
        return entries.stream()
                .collect(Collectors.collectingAndThen(Collectors.toList(), statisticsMapper::map));
    }
}
