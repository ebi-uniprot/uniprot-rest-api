package org.uniprot.api.statistics.model;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class StatisticAttributeImpl implements StatisticAttribute {
    String name;
    long count;
    long entryCount;
    String description;
    StatisticType statisticType;
}
