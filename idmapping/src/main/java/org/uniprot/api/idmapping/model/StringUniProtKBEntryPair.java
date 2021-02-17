package org.uniprot.api.idmapping.model;

import lombok.Builder;
import lombok.Getter;

import org.uniprot.core.uniprotkb.UniProtKBEntry;

/**
 * @author sahmad
 * @created 17/02/2021
 */
@Getter
@Builder
public class StringUniProtKBEntryPair {
    private String from;
    private UniProtKBEntry entry;
}
