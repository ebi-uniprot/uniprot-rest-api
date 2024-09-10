package org.uniprot.api.support.data.statistics.model;

import java.util.Date;

public interface StatisticsModuleStatisticsHistory {
    StatisticsModuleStatisticsType getStatisticsType();

    String getReleaseName();

    Date getReleaseDate();

    long getValueCount();

    long getEntryCount();
}
