package org.uniprot.api.unisave.repository.domain.impl;

import java.io.Serializable;

import lombok.Data;

import org.uniprot.api.unisave.repository.domain.EventTypeEnum;

@Data
public class IdentifierStatusId implements Serializable {
    private EventTypeEnum eventType;
    private String sourceAccession;
    private String targetAccession;
}
