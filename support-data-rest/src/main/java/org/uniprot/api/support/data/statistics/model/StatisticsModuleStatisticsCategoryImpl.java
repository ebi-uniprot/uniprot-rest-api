package org.uniprot.api.support.data.statistics.model;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class StatisticsModuleStatisticsCategoryImpl implements StatisticsModuleStatisticsCategory {
    String categoryName;
    String searchField;
    String label;
    long totalCount;
    List<StatisticsModuleStatisticsAttribute> items;
}
