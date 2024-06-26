package org.uniprot.api.idmapping.common.response.model;

import org.uniprot.api.common.repository.search.EntryPair;
import org.uniprot.core.uniprotkb.UniProtKBEntry;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;

/**
 * @author lgonzales
 * @since 23/02/2021
 */
@Getter
@Builder
@EqualsAndHashCode
public class UniProtKBEntryPair implements EntryPair<UniProtKBEntry> {
    private final String from;
    private final UniProtKBEntry to;
}
