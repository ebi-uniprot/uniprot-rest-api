package org.uniprot.api.statistics.model;

public interface StatisticsModuleStatisticsAttribute {
    String getName();

    String getLabel();

    long getCount();

    long getEntryCount();

    String getDescription();
}
