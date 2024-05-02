package org.uniprot.api.idmapping.common.response.model;

import org.uniprot.api.common.repository.search.EntryPair;
import org.uniprot.core.uniparc.UniParcEntry;

import lombok.Builder;
import lombok.Getter;

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
