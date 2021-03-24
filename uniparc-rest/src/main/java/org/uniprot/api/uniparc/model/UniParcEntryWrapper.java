package org.uniprot.api.uniparc.model;

import org.uniprot.core.uniparc.UniParcEntry;

import lombok.Builder;
import lombok.Getter;

/**
 * @author sahmad
 * @created 24/03/2021
 */
@Getter
@Builder
public class UniParcEntryWrapper {
    private UniParcEntry entry;
}
