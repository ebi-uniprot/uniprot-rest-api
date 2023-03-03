package org.uniprot.api.statistics.model;

import java.util.List;

public interface StatisticModuleStatisticCategory {
    String getCategoryName();

    String getSearchField();

    String getLabel();

    long getTotalCount();

    List<StatisticsModuleStatisticAttribute> getItems();
}
