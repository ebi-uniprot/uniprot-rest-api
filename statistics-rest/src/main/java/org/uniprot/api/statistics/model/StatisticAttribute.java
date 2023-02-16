package org.uniprot.api.statistics.model;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class StatisticAttribute {
    private final String name;
    private final long count;
    private final long entryCount;
    private final String description;
    private final StatisticType statisticType;
}
