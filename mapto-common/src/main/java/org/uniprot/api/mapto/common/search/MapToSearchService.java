package org.uniprot.api.mapto.common.search;

import org.uniprot.api.mapto.common.model.MapToJob;
import org.uniprot.api.mapto.common.model.MapToSearchResult;

public abstract class MapToSearchService {
    public static final String INCLUDE_ISOFORM = "includeIsoform";
    public static final int MAP_TO_PAGE_SIZE = 100;

    public abstract MapToSearchResult getTargetIds(MapToJob mapToJob, String cursor);
}
