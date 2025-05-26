package org.uniprot.api.mapto.common.search;

import org.uniprot.api.mapto.common.model.MapToJob;
import org.uniprot.api.mapto.common.model.MapToSearchResult;

public abstract class MapToSearchService {
    public static final String INCLUDE_ISOFORM = "includeIsoform";
    public static final int MAP_TO_PAGE_SIZE = 100;
    private final Integer maxIdMappingToIdsCount;

    protected MapToSearchService(Integer maxIdMappingToIdsCount) {
        this.maxIdMappingToIdsCount = maxIdMappingToIdsCount;
    }

    public String validateTargetLimit(Long totalElements) {
        Integer limit = getMaxIdMappingToIdsCount();
        if (limit != null && totalElements > limit) {
            return "Number of target ids: %d exceeds the allowed limit: %d"
                    .formatted(totalElements, limit);
        }
        return null;
    }

    public abstract MapToSearchResult getTargetIds(MapToJob mapToJob, String cursor);

    public Integer getMaxIdMappingToIdsCount() {
        return maxIdMappingToIdsCount;
    }
}
