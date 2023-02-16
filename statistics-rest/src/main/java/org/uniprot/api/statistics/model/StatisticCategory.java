package org.uniprot.api.statistics.model;

import java.util.List;

public interface StatisticCategory {
    String getName();

    long getTotalCount();

    long getTotalEntryCount();

    List<StatisticAttribute> getAttributes();
}
