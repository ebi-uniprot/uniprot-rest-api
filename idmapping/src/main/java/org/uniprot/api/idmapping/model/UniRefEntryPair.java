package org.uniprot.api.idmapping.model;

import lombok.Builder;
import lombok.Getter;

import org.uniprot.core.uniref.UniRefEntryLight;

/**
 * @author lgonzales
 * @since 23/02/2021
 */
@Getter
@Builder
public class UniRefEntryPair implements EntryPair<UniRefEntryLight> {
    private final String from;
    private final UniRefEntryLight to;
}
