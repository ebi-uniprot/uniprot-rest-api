package org.uniprot.api.statistics.model;

import lombok.Builder;
import lombok.Value;

import com.fasterxml.jackson.annotation.JsonInclude;

@Value
@Builder
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class StatisticsModuleStatisticsAttributeImpl
        implements StatisticsModuleStatisticsAttribute {
    String name;
    String label;
    long count;
    long entryCount;
    String description;
}
