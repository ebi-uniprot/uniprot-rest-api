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

    public void checkTargetLimits(Long totalElements) {
        if (totalElements > getMaxIdMappingToIdsCount()) {
            throw new IllegalStateException(
                    "No of target ids: %d is greater than the limit: %d"
                            .formatted(totalElements, maxIdMappingToIdsCount));
        }
    }

    public abstract MapToSearchResult getTargetIds(MapToJob mapToJob, String cursor);

    public Integer getMaxIdMappingToIdsCount() {
        return maxIdMappingToIdsCount;
    }
}
