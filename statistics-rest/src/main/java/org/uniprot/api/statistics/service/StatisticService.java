package org.uniprot.api.statistics.service;

import org.springframework.stereotype.Service;
import org.uniprot.api.statistics.mapper.StatisticsMapper;
import org.uniprot.api.statistics.model.StatisticAttribute;
import org.uniprot.api.statistics.model.StatisticCategory;
import org.uniprot.api.statistics.model.StatisticType;
import org.uniprot.api.statistics.repository.StatisticsCategoryRepository;
import org.uniprot.api.statistics.repository.UniprotkbStatisticsEntryRepository;

import java.util.Collection;

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

    public Collection<StatisticCategory> findAllByVersionAndStatisticType(
            String version, StatisticType statisticsType) {
        return statisticsMapper.map(
                statisticsEntryRepository.findAllByReleaseNameAndEntryType(
                        version, statisticsMapper.map(statisticsType)));
    }

    public StatisticCategory findAllByVersionAndStatisticTypeAndCategory(
            String version, StatisticType statisticsType, String category) {
        return statisticsMapper.map(category, statisticsEntryRepository
                .findAllByReleaseNameAndEntryTypeAndStatisticsCategoryId(
                        version,
                        statisticsMapper.map(statisticsType),
                        statisticsCategoryRepository
                                .findByCategory(category)
                                .orElseThrow(
                                        () ->
                                                new IllegalArgumentException(
                                                        "Category " + category + " not found"))));
    }

    public StatisticAttribute findAllByVersionAndStatisticTypeAndCategoryAndAttribute(
            String version, StatisticType statisticsType, String category, String attribute) {
        return statisticsEntryRepository
                .findByReleaseNameAndEntryTypeAndStatisticsCategoryIdAndAttributeName(
                        version,
                        statisticsMapper.map(statisticsType),
                        statisticsCategoryRepository
                                .findByCategory(category)
                                .orElseThrow(
                                        () ->
                                                new IllegalArgumentException(
                                                        "Category " + category + " not found")),
                        attribute)
                .map(statisticsMapper::map).orElseThrow(() -> new IllegalArgumentException(
                        "No matching record for Version" + version + " and Category " + category + " and attribute " + attribute));
    }
}
