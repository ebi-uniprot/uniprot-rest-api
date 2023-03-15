package org.uniprot.api.support.data.statistics.model;

import lombok.Value;

import java.util.Collection;

@Value
public class StatisticsModuleStatisticsResult<T> {
    Collection<T> results;
}
