package org.uniprot.api.rest.download.model;

import lombok.Getter;

@Getter
public enum StoreType {
    UNI_REF("UniRef"),
    UNIPROT_KB("UniProtKB"),
    UNI_PARC("UniParc");

    private final String name;

    StoreType(String name) {
        this.name = name;
    }
}
