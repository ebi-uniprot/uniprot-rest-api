package org.uniprot.api.statistics.model;

import lombok.Getter;

@Getter
public enum StatisticType {
    REVIEWED("reviewed"),
    UNREVIWED("unreviewed");

    private final String value;

    StatisticType(String value) {
        this.value = value;
    }
}
