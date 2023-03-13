package org.uniprot.api.statistics.model;

import java.util.Collection;

import lombok.Value;

@Value
public class StatisticsModuleStatisticsResult<T> {
    Collection<T> results;
}
