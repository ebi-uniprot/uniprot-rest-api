package org.uniprot.api.mapto.common.model;

import lombok.Data;
import org.uniprot.api.common.repository.search.page.impl.CursorPage;

import java.util.List;

@Data
public class MapToSearchResult {
    private final List<String> targetIds;
    private final CursorPage page;
}
