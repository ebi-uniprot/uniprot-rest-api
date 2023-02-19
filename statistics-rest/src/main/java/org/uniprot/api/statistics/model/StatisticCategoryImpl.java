package org.uniprot.api.statistics.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Value;

import java.util.List;

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
