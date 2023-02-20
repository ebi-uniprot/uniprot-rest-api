package org.uniprot.api.statistics.model;

import java.util.Collection;

import lombok.Value;

@Value
public class StatisticResult<T> {
    Collection<T> results;
}
