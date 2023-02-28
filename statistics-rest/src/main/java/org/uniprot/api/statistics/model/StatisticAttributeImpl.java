package org.uniprot.api.statistics.model;

import lombok.Builder;
import lombok.Value;

import com.fasterxml.jackson.annotation.JsonInclude;

@Value
@Builder
public class StatisticAttributeImpl implements StatisticAttribute {
    String name;

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    String label;

    long count;
    long entryCount;
    String description;
    StatisticType statisticType;
}
