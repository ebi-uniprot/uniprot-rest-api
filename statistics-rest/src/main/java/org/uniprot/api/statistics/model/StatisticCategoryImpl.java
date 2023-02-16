package org.uniprot.api.statistics.model;

import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder
public class StatisticCategoryImpl implements StatisticCategory {
    String name;
    long totalCount;
    long totalEntryCount;
    List<StatisticAttribute> attributes;
}
