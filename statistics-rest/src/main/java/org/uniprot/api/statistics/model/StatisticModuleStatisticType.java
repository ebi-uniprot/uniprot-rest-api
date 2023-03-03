package org.uniprot.api.statistics.model;

import lombok.Getter;

@Getter
public enum StatisticModuleStatisticType {
    REVIEWED("reviewed"),
    UNREVIEWED("unreviewed");

    private final String value;

    StatisticModuleStatisticType(String value) {
        this.value = value;
    }
}
