package org.uniprot.api.idmapping.model;

import lombok.Builder;
import lombok.Getter;

import org.uniprot.core.uniparc.UniParcEntry;

/**
 * @author lgonzales
 * @since 23/02/2021
 */
@Getter
@Builder
public class UniParcEntryPair implements EntryPair<UniParcEntry> {
    private final String from;
    private final UniParcEntry to;
}
