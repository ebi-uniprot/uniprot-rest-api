package org.uniprot.api.support.data.statistics.model;

import java.util.List;

public interface StatisticsModuleStatisticsCategory {
    String getCategoryName();

    String getSearchField();

    String getLabel();

    long getTotalCount();

    List<StatisticsModuleStatisticsAttribute> getItems();
}
