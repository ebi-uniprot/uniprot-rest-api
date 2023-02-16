package org.uniprot.api.statistics.model;

import lombok.Getter;

@Getter
public enum StatisticType {
    SWISSPROT("Swiss-Prot"),
    TREMBL("TrEMBL");

    private final String value;

    StatisticType(String value) {
        this.value = value;
    }
}
