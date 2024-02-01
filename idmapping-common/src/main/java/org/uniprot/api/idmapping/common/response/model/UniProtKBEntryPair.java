package org.uniprot.api.idmapping.common.response.model;

import lombok.Builder;
import lombok.Getter;

import org.uniprot.api.common.repository.search.EntryPair;
import org.uniprot.core.uniprotkb.UniProtKBEntry;

/**
 * @author lgonzales
 * @since 23/02/2021
 */
@Getter
@Builder
public class UniProtKBEntryPair implements EntryPair<UniProtKBEntry> {
    private final String from;
    private final UniProtKBEntry to;
}
