package org.uniprot.api.statistics.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class StatisticModuleStatisticCategoryImpl implements StatisticModuleStatisticCategory {
    String categoryName;
    String searchField;
    String label;
    long totalCount;
    List<StatisticsModuleStatisticAttribute> items;
}
