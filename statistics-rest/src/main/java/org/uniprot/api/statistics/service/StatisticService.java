package org.uniprot.api.statistics.service;

import org.springframework.stereotype.Service;
import org.uniprot.api.statistics.entity.UniprotkbStatisticsEntry;
import org.uniprot.api.statistics.mapper.StatisticsMapper;
import org.uniprot.api.statistics.model.StatisticAttribute;
import org.uniprot.api.statistics.model.StatisticCategory;
import org.uniprot.api.statistics.model.StatisticType;
import org.uniprot.api.statistics.repository.StatisticsCategoryRepository;
import org.uniprot.api.statistics.repository.UniprotkbStatisticsEntryRepository;

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

    public List<StatisticCategory> findAllByVersionAndStatisticType(String version, StatisticType statisticsType) {
        return statisticsEntryRepository.findAllByReleaseNameAndEntryType(version, statisticsMapper.map(statisticsType))
                .stream()
                .collect(Collectors.groupingBy(UniprotkbStatisticsEntry::getStatisticsCategoryId))
                .entrySet().stream().map(entry -> statisticsMapper.map(entry.getKey().getCategory(), entry.getValue()))
                .collect(Collectors.toList());
    }

    public StatisticCategory findAllByVersionAndStatisticTypeAndCategory(String version, StatisticType statisticsType, String category) {
        return statisticsEntryRepository
                .findAllByReleaseNameAndEntryTypeAndStatisticsCategoryId(version,
                        statisticsMapper.map(statisticsType),
                        statisticsCategoryRepository.findByCategory(category).orElseThrow(() -> new IllegalArgumentException("Category " + category + " not found")))
                .stream()
                .collect(Collectors.collectingAndThen(Collectors.toList(), list -> statisticsMapper.map(category, list)));
    }

    public StatisticAttribute findAllByVersionAndStatisticTypeAndCategoryAndAttribute(
            String version, StatisticType statisticsType, String category, String attribute) {
        return statisticsEntryRepository
                .findByReleaseNameAndEntryTypeAndStatisticsCategoryIdAndAttributeName(
                        version,
                        statisticsMapper.map(statisticsType),
                        statisticsCategoryRepository.findByCategory(category).orElseThrow(() -> new IllegalArgumentException("Category " + category + " not found")), attribute)
                .map(statisticsMapper::map).orElseThrow(() -> new IllegalArgumentException("No matching record for Version" + version + " and Category " + category + " and attribute " + attribute));
    }
}
