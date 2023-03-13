package org.uniprot.api.statistics.model;

import java.util.List;

public interface StatisticsModuleStatisticsCategory {
    String getCategoryName();

    String getSearchField();

    String getLabel();

    long getTotalCount();

    List<StatisticsModuleStatisticsAttribute> getItems();
}
