package org.uniprot.api.idmapping.common.response.model;

import lombok.EqualsAndHashCode;
import org.uniprot.api.common.repository.search.EntryPair;
import org.uniprot.core.uniref.UniRefEntryLight;

import lombok.Builder;
import lombok.Getter;

/**
 * @author lgonzales
 * @since 23/02/2021
 */
@Getter
@Builder
@EqualsAndHashCode
public class UniRefEntryPair implements EntryPair<UniRefEntryLight> {
    private final String from;
    private final UniRefEntryLight to;
}
