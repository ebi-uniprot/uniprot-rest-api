package org.uniprot.api.mapto.common.search;

import org.uniprot.api.mapto.common.model.MapToJob;
import org.uniprot.api.mapto.common.model.MapToSearchResult;
import org.uniprot.store.config.UniProtDataType;

public interface MapToSearchService {
    String INCLUDE_ISOFORM = "includeIsoform";
    int MAP_TO_PAGE_SIZE = 100;
    int MAX_TARGET_IDS = 500000; // resuse idmapping

    static void checkTheResultLimits(Long totalElements) {
        if (totalElements > MAX_TARGET_IDS) {
            throw new IllegalStateException(
                    "No of target ids: %d is greater than the limit: %d"
                            .formatted(totalElements, MAX_TARGET_IDS));
        }
    }

    MapToSearchResult getTargetIds(MapToJob mapToJob, String cursor);
}
