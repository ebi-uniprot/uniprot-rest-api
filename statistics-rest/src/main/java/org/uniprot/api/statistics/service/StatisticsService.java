package org.uniprot.api.statistics.service;

import java.util.List;
import java.util.Set;

import org.uniprot.api.statistics.model.StatisticsModuleStatisticsCategory;

public interface StatisticsService {
    List<StatisticsModuleStatisticsCategory> findAllByVersionAndStatisticTypeAndCategoryIn(
            String version, String statisticType, Set<String> categories);
}
