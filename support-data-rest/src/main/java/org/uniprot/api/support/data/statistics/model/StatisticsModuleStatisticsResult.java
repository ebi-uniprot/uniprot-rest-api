package org.uniprot.api.support.data.statistics.model;

import java.util.Collection;

import lombok.Value;

@Value
public class StatisticsModuleStatisticsResult<T> {
    Collection<T> results;
}
