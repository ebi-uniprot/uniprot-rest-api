package org.uniprot.api.mapto.common.model;

import java.util.List;

import org.uniprot.api.common.repository.search.page.impl.CursorPage;

import lombok.Data;

@Data
public class MapToSearchResult {
    private final List<String> targetIds;
    private final CursorPage page;
}
