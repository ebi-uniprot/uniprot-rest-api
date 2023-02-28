package org.uniprot.api.statistics.model;

public interface StatisticAttribute {
    String getName();

    String getLabel();

    long getCount();

    long getEntryCount();

    String getDescription();

    StatisticType getStatisticType();
}
