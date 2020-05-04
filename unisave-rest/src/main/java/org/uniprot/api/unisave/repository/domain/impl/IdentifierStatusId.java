package org.uniprot.api.unisave.repository.domain.impl;

import lombok.Data;

import java.io.Serializable;

@Data
public class IdentifierStatusId implements Serializable {
    private String eventType;
    private String sourceAccession;
    private String targetAccession;
}
