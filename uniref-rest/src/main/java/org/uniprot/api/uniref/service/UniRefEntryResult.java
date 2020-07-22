package org.uniprot.api.uniref.service;

import lombok.Builder;
import lombok.Getter;

import org.uniprot.api.common.repository.search.page.impl.CursorPage;
import org.uniprot.core.uniref.UniRefEntry;

/**
 * @author lgonzales
 * @since 22/07/2020
 */
@Builder
@Getter
public class UniRefEntryResult {

    private UniRefEntry entry;

    private CursorPage page;
}
