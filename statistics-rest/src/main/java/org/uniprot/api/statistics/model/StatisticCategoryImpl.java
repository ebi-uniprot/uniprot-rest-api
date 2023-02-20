package org.uniprot.api.statistics.model;

import java.util.List;

import lombok.Builder;
import lombok.Value;

import com.fasterxml.jackson.annotation.JsonInclude;

@Value
@Builder
public class StatisticCategoryImpl implements StatisticCategory {
    String name;

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    String searchField;

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    String label;

    long totalCount;
    long totalEntryCount;
    List<StatisticAttribute> attributes;
}
