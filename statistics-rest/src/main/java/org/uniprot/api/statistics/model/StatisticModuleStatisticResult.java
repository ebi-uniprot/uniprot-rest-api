package org.uniprot.api.statistics.model;

import lombok.Value;

import java.util.Collection;

@Value
public class StatisticModuleStatisticResult<T> {
    Collection<T> results;
}
