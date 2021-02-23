package org.uniprot.api.idmapping.model;

import lombok.Builder;
import lombok.Getter;

import org.uniprot.core.uniref.UniRefEntry;

/**
 * @author lgonzales
 * @since 23/02/2021
 */
@Getter
@Builder
public class UniRefEntryPair implements EntryPair<UniRefEntry> {
    private final String from;
    private final UniRefEntry to;
}
