package org.uniprot.api.support.data.statistics.service;

import static org.uniprot.api.support.data.statistics.entity.EntryType.SWISSPROT;
import static org.uniprot.api.support.data.statistics.entity.EntryType.TREMBL;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.uniprot.api.support.data.statistics.entity.*;
import org.uniprot.api.support.data.statistics.mapper.StatisticsMapper;
import org.uniprot.api.support.data.statistics.model.*;
import org.uniprot.api.support.data.statistics.repository.AttributeQueryRepository;
import org.uniprot.api.support.data.statistics.repository.StatisticsCategoryRepository;
import org.uniprot.api.support.data.statistics.repository.UniProtKBStatisticsEntryRepository;
import org.uniprot.api.support.data.statistics.repository.UniProtReleaseRepository;

@Service
public class StatisticsServiceImpl implements StatisticsService {

    public static final String PREVIOUS_RELEASE_DATE = ":previous_release_date";
    public static final String START_QUERY = "(";
    public static final String END_QUERY = ")";
    public static final String REVIEWED = "reviewed:";
    private final UniProtKBStatisticsEntryRepository statisticsEntryRepository;
    private final StatisticsCategoryRepository statisticsCategoryRepository;
    private final UniProtReleaseRepository releaseRepository;
    private final AttributeQueryRepository attributeQueryRepository;
    private final StatisticsMapper statisticsMapper;

    public StatisticsServiceImpl(
            UniProtKBStatisticsEntryRepository uniprotkbStatisticsEntryRepository,
            StatisticsCategoryRepository statisticsCategoryRepository,
            UniProtReleaseRepository uniProtReleaseRepository,
            AttributeQueryRepository attributeQueryRepository,
            StatisticsMapper statisticsMapper) {
        this.statisticsEntryRepository = uniprotkbStatisticsEntryRepository;
        this.statisticsCategoryRepository = statisticsCategoryRepository;
        this.releaseRepository = uniProtReleaseRepository;
        this.attributeQueryRepository = attributeQueryRepository;
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
    public Collection<StatisticsModuleStatisticsCategory> findAllByVersionAndCategoryIn(
            String version, Set<String> categories) {
        List<UniProtKBStatisticsEntry> entries;
        if (categories.isEmpty()) {
            entries = statisticsEntryRepository.findAllByReleaseName(getRelease(version));
        } else {
            entries =
                    statisticsEntryRepository.findAllByReleaseNameAndStatisticsCategoryIn(
                            getRelease(version), getCategories(categories));
        }
        return entries.stream()
                .collect(Collectors.groupingBy(UniProtKBStatisticsEntry::getStatisticsCategory))
                .entrySet()
                .stream()
                .map(this::groupEntries)
                .map(this::buildStatisticsModuleStatisticsCategory)
                .collect(Collectors.toList());
    }

    private Map.Entry<StatisticsCategory, List<UniProtKBStatisticsEntry>> groupEntries(
            Map.Entry<StatisticsCategory, List<UniProtKBStatisticsEntry>> entry) {
        List<UniProtKBStatisticsEntry> groupedEntries =
                entry.getValue().stream()
                        .collect(Collectors.groupingBy(UniProtKBStatisticsEntry::getAttributeName))
                        .values()
                        .stream()
                        .map(this::mapToSingleEntry)
                        .sorted(Comparator.comparing(UniProtKBStatisticsEntry::getAttributeName))
                        .toList();
        return new AbstractMap.SimpleEntry<>(entry.getKey(), groupedEntries);
    }

    private UniProtKBStatisticsEntry mapToSingleEntry(
            List<UniProtKBStatisticsEntry> uniProtKBStatisticsEntries) {
        UniProtKBStatisticsEntry uniProtKBStatisticsEntry = new UniProtKBStatisticsEntry();
        UniProtKBStatisticsEntry firstEntry = uniProtKBStatisticsEntries.get(0);
        uniProtKBStatisticsEntry.setAttributeName(firstEntry.getAttributeName());
        uniProtKBStatisticsEntry.setStatisticsCategory(firstEntry.getStatisticsCategory());
        uniProtKBStatisticsEntry.setValueCount(
                uniProtKBStatisticsEntries.stream()
                        .mapToLong(UniProtKBStatisticsEntry::getValueCount)
                        .sum());
        uniProtKBStatisticsEntry.setEntryCount(
                uniProtKBStatisticsEntries.stream()
                        .mapToLong(UniProtKBStatisticsEntry::getEntryCount)
                        .sum());
        uniProtKBStatisticsEntry.setDescription(firstEntry.getDescription());
        uniProtKBStatisticsEntry.setReleaseName(firstEntry.getReleaseName());
        return uniProtKBStatisticsEntry;
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
        return entry.getValue().stream()
                .map(e -> statisticsMapper.map(e, getAttributeQuery(e)))
                .collect(Collectors.toList());
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

    private String getAttributeQuery(UniProtKBStatisticsEntry entry) {
        Optional<AttributeQuery> attributeQuery =
                attributeQueryRepository.findByAttributeNameIgnoreCase(entry.getAttributeName());
        return attributeQuery.map(query -> prepareQuery(query, entry)).orElse("");
    }

    private String prepareQuery(AttributeQuery query, UniProtKBStatisticsEntry entry) {
        String result = query.getQuery();
        if (result.contains(PREVIOUS_RELEASE_DATE)) {
            SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd");
            String currentRelease = entry.getReleaseName().getId();
            Date previousReleaseDate =
                    releaseRepository
                            .findPreviousReleaseDate(currentRelease)
                            .orElseThrow(
                                    () ->
                                            new IllegalArgumentException(
                                                    "Invalid Release Name: %s"
                                                            .formatted(currentRelease)));
            result =
                    result.replace(
                            PREVIOUS_RELEASE_DATE,
                            ":" + simpleDateFormat.format(previousReleaseDate));
        }
        EntryType entryType = entry.getEntryType();
        String prepend =
                Objects.equals(entryType, SWISSPROT)
                        ? START_QUERY + REVIEWED + "true" + END_QUERY + " AND "
                        : Objects.equals(entryType, TREMBL)
                                ? START_QUERY + REVIEWED + "false" + END_QUERY + " AND "
                                : "";
        return prepend + result;
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
