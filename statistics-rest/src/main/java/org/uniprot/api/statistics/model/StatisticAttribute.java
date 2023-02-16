package org.uniprot.api.statistics.model;

public interface StatisticAttribute {
    String getName();
    long getCount();
    long getEntryCount();
    String getDescription();
    StatisticType getStatisticType();
}
