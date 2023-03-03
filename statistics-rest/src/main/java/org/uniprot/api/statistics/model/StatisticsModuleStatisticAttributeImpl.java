package org.uniprot.api.statistics.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class StatisticsModuleStatisticAttributeImpl implements StatisticsModuleStatisticAttribute {
    String name;
    String label;
    long count;
    long entryCount;
    String description;
}
