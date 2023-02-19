package org.uniprot.api.statistics.model;

import lombok.Value;

import java.util.Collection;

@Value
public class StatisticResult<T> {
    Collection<T> results;
}
