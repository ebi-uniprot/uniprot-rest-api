package org.uniprot.api.unisave.repository.domain;

public enum DatabaseEnum {
    SWISSPROT("Swiss-Prot"),
    TREMBL("TrEMBL"),
    WRONG_TREMBL("TrEMBL-Depreciated");

    private String name;

    DatabaseEnum(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return name;
    }
}
