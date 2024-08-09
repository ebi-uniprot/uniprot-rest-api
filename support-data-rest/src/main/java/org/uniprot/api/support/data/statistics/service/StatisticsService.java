package org.uniprot.api.support.data.statistics.service;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.uniprot.api.support.data.statistics.model.StatisticsModuleStatisticsCategory;
import org.uniprot.api.support.data.statistics.model.StatisticsModuleStatisticsHistory;

public interface StatisticsService {
    List<StatisticsModuleStatisticsCategory> findAllByVersionAndStatisticTypeAndCategoryIn(
            String version, String statisticType, Set<String> categories);

    List<StatisticsModuleStatisticsHistory> findAllByAttributeAndStatisticType(
            String attribute, String statisticType);

    Collection<StatisticsModuleStatisticsCategory> findAllByVersionAndCategoryIn(String release, Set<String> categories);
}
