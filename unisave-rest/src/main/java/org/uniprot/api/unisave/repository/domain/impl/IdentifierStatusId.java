package org.uniprot.api.unisave.repository.domain.impl;

import java.io.Serializable;

import lombok.Data;

@Data
public class IdentifierStatusId implements Serializable {
    private String eventType;
    private String sourceAccession;
    private String targetAccession;
}
