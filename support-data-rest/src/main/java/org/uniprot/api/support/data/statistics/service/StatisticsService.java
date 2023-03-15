package org.uniprot.api.support.data.statistics.service;

import org.uniprot.api.support.data.statistics.model.StatisticsModuleStatisticsCategory;

import java.util.List;
import java.util.Set;

public interface StatisticsService {
    List<StatisticsModuleStatisticsCategory> findAllByVersionAndStatisticTypeAndCategoryIn(
            String version, String statisticType, Set<String> categories);
}
