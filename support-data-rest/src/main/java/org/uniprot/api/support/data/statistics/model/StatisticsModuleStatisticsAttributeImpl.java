package org.uniprot.api.support.data.statistics.model;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.Builder;
import lombok.Value;

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
