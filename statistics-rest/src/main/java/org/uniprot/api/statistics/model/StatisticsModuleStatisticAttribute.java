package org.uniprot.api.statistics.model;

public interface StatisticsModuleStatisticAttribute {
    String getName();

    String getLabel();

    long getCount();

    long getEntryCount();

    String getDescription();
}
