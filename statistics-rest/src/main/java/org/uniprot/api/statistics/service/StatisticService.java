package org.uniprot.api.statistics.service;

import org.springframework.stereotype.Service;
import org.uniprot.api.statistics.entity.UniprotkbStatisticsEntry;
import org.uniprot.api.statistics.mapper.StatisticMapper;
import org.uniprot.api.statistics.model.StatisticCategory;
import org.uniprot.api.statistics.model.StatisticType;
import org.uniprot.api.statistics.repository.StatisticsCategoryRepository;
import org.uniprot.api.statistics.repository.UniprotkbStatisticsEntryRepository;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class StatisticService {

    private final UniprotkbStatisticsEntryRepository statisticsEntryRepository;
    private final StatisticsCategoryRepository statisticsCategoryRepository;
    private final StatisticMapper statisticMapper;

    public StatisticService(
            UniprotkbStatisticsEntryRepository uniprotkbStatisticsEntryRepository,
            StatisticsCategoryRepository statisticsCategoryRepository,
            StatisticMapper statisticMapper) {
        this.statisticsEntryRepository = uniprotkbStatisticsEntryRepository;
        this.statisticsCategoryRepository = statisticsCategoryRepository;
        this.statisticMapper = statisticMapper;
    }

    public Collection<StatisticCategory> findAllByVersionAndStatisticTypeAndCategoryIn(
            String version, String statisticType, Collection<String> categories) {
        List<UniprotkbStatisticsEntry> entries;
        if (categories.isEmpty()) {
            entries =
                    statisticsEntryRepository.findAllByReleaseNameAndEntryType(
                            version, statisticMapper.map(getStatisticType(statisticType)));
        } else {
            entries =
                    statisticsEntryRepository
                            .findAllByReleaseNameAndEntryTypeAndStatisticsCategoryIdIn(
                                    version,
                                    statisticMapper.map(getStatisticType(statisticType)),
                                    categories.stream()
                                            .map(
                                                    cat ->
                                                            statisticsCategoryRepository
                                                                    .findByCategoryIgnoreCase(cat)
                                                                    .orElseThrow(
                                                                            () ->
                                                                                    new IllegalArgumentException(
                                                                                            String
                                                                                                    .format(
                                                                                                            "Invalid Statistic Category: %s",
                                                                                                            cat))))
                                            .collect(Collectors.toList()));
        }
        return entries.stream()
                .collect(Collectors.groupingBy(UniprotkbStatisticsEntry::getStatisticsCategoryId))
                .entrySet()
                .stream()
                .map(entry -> statisticMapper.map(entry.getKey(), entry.getValue()))
                .collect(Collectors.toList());
    }

    private static StatisticType getStatisticType(String statisticType) {
        try {
            return StatisticType.valueOf(statisticType.toUpperCase());
        } catch (Exception e) {
            throw new IllegalArgumentException(
                    String.format("Invalid Statistic Type: %s", statisticType));
        }
    }
}
