package org.uniprot.api.support.data.statistics.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Value;

import java.util.Date;

@Value
@Builder
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class StatisticsModuleStatisticsHistoryImpl implements StatisticsModuleStatisticsHistory {
    StatisticsModuleStatisticsType statisticsType;
    String releaseName;

    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "Europe/London")
    Date releaseDate;

    long valueCount;
    long entryCount;
}
