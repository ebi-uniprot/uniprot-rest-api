package org.uniprot.api.statistics.model;

import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder
public class StatisticCategory {
    private final String name;
    private final long totalCount;
    private final long totalEntryCount;
    private final List<StatisticAttribute> attributes;
}
