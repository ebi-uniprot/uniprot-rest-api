package org.uniprot.api.support.data.statistics.model;

import lombok.Getter;

@Getter
public enum StatisticsModuleStatisticsType {
    REVIEWED("reviewed"),
    UNREVIEWED("unreviewed");

    private final String value;

    StatisticsModuleStatisticsType(String value) {
        this.value = value;
    }
}
