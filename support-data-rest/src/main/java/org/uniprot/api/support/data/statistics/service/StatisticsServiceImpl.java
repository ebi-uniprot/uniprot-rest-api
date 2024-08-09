package org.uniprot.api.support.data.statistics.service;

import java.util.*;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.uniprot.api.support.data.statistics.entity.EntryType;
import org.uniprot.api.support.data.statistics.entity.StatisticsCategory;
import org.uniprot.api.support.data.statistics.entity.UniProtKBStatisticsEntry;
import org.uniprot.api.support.data.statistics.entity.UniProtRelease;
import org.uniprot.api.support.data.statistics.mapper.StatisticsMapper;
import org.uniprot.api.support.data.statistics.model.*;
import org.uniprot.api.support.data.statistics.repository.StatisticsCategoryRepository;
import org.uniprot.api.support.data.statistics.repository.UniProtKBStatisticsEntryRepository;
import org.uniprot.api.support.data.statistics.repository.UniProtReleaseRepository;

@Service
public class StatisticsServiceImpl implements StatisticsService {

    private final UniProtKBStatisticsEntryRepository statisticsEntryRepository;
    private final StatisticsCategoryRepository statisticsCategoryRepository;
    private final UniProtReleaseRepository releaseRepository;
    private final StatisticsMapper statisticsMapper;

    public StatisticsServiceImpl(
            UniProtKBStatisticsEntryRepository uniprotkbStatisticsEntryRepository,
            StatisticsCategoryRepository statisticsCategoryRepository,
            UniProtReleaseRepository uniProtReleaseRepository,
            StatisticsMapper statisticsMapper) {
        this.statisticsEntryRepository = uniprotkbStatisticsEntryRepository;
        this.statisticsCategoryRepository = statisticsCategoryRepository;
        this.releaseRepository = uniProtReleaseRepository;
        this.statisticsMapper = statisticsMapper;
    }

    @Override
    public List<StatisticsModuleStatisticsCategory> findAllByVersionAndStatisticTypeAndCategoryIn(
            String version, String statisticType, Set<String> categories) {
        List<UniProtKBStatisticsEntry> entries;
        if (categories.isEmpty()) {
            entries =
                    statisticsEntryRepository.findAllByReleaseNameAndEntryType(
                            getRelease(version),
                            statisticsMapper.map(getStatisticType(statisticType)));
        } else {
            entries =
                    statisticsEntryRepository
                            .findAllByReleaseNameAndEntryTypeAndStatisticsCategoryIn(
                                    getRelease(version),
                                    statisticsMapper.map(getStatisticType(statisticType)),
                                    getCategories(categories));
        }
        return entries.stream()
                .collect(Collectors.groupingBy(UniProtKBStatisticsEntry::getStatisticsCategory))
                .entrySet()
                .stream()
                .map(this::buildStatisticsModuleStatisticsCategory)
                .collect(Collectors.toList());
    }

    private UniProtRelease getRelease(String version) {
        return releaseRepository
                .findById(version)
                .orElseThrow(
                        () ->
                                new IllegalArgumentException(
                                        String.format("Invalid Release Version: %s", version)));
    }

    @Override
    public List<StatisticsModuleStatisticsHistory> findAllByAttributeAndStatisticType(
            String attribute, String statisticType) {
        List<UniProtKBStatisticsEntry> results;
        if (StringUtils.isNotEmpty(statisticType)) {
            EntryType entryType = statisticsMapper.map(getStatisticType(statisticType));
            results =
                    statisticsEntryRepository.findAllByAttributeNameIgnoreCaseAndEntryType(
                            attribute, entryType);
        } else {
            results = statisticsEntryRepository.findAllByAttributeNameIgnoreCase(attribute);
        }
        return results.stream()
                .map(statisticsMapper::mapHistory)
                .sorted(Comparator.comparing(StatisticsModuleStatisticsHistory::getReleaseName))
                .collect(Collectors.toList());
    }

    @Override
    public Collection<StatisticsModuleStatisticsCategory> findAllByVersionAndCategoryIn(String version, Set<String> categories) {
        List<UniProtKBStatisticsEntry> entries;
        if (categories.isEmpty()) {
            entries =
                    statisticsEntryRepository.findAllByReleaseName(
                            getRelease(version));
        } else {
            entries =
                    statisticsEntryRepository
                            .findAllByReleaseNameAndStatisticsCategoryIn(
                                    getRelease(version),
                                    getCategories(categories));
        }
        return entries.stream()
                .collect(Collectors.groupingBy(UniProtKBStatisticsEntry::getStatisticsCategory))
                .entrySet()
                .stream()
                .map(this::buildStatisticsModuleStatisticsCategory)
                .collect(Collectors.toList());
    }

    private StatisticsModuleStatisticsCategoryImpl buildStatisticsModuleStatisticsCategory(
            Map.Entry<StatisticsCategory, List<UniProtKBStatisticsEntry>> entry) {
        return StatisticsModuleStatisticsCategoryImpl.builder()
                .categoryName(entry.getKey().getCategory())
                .totalCount(getTotalCount(entry))
                .label(entry.getKey().getLabel())
                .searchField(entry.getKey().getSearchField())
                .items(getItems(entry))
                .build();
    }

    private List<StatisticsModuleStatisticsAttribute> getItems(
            Map.Entry<StatisticsCategory, List<UniProtKBStatisticsEntry>> entry) {
        return entry.getValue().stream().map(statisticsMapper::map).collect(Collectors.toList());
    }

    private static long getTotalCount(
            Map.Entry<StatisticsCategory, List<UniProtKBStatisticsEntry>> entry) {
        return entry.getValue().stream().mapToLong(UniProtKBStatisticsEntry::getValueCount).sum();
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
