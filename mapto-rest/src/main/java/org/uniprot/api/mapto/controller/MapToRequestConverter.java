package org.uniprot.api.mapto.controller;

import static org.uniprot.api.mapto.common.search.MapToSearchService.INCLUDE_ISOFORM;

import java.util.Map;

import org.springframework.stereotype.Component;
import org.uniprot.api.mapto.common.model.MapToJobRequest;
import org.uniprot.api.mapto.request.MapToSearchRequest;
import org.uniprot.store.config.UniProtDataType;

@Component
public class MapToRequestConverter {
    public MapToJobRequest convert(MapToSearchRequest request) {
        String query = request.getQuery();
        MapToJobRequest mapToJobRequest =
                new MapToJobRequest(request.getFrom(), request.getTo(), query);
        Map<String, String> extraParams = getExtraParams(request);
        mapToJobRequest.setExtraParams(extraParams);

        return mapToJobRequest;
    }

    private Map<String, String> getExtraParams(MapToSearchRequest request) {
        if (isUniProtKBSourceType(request)) {
            return Map.of(INCLUDE_ISOFORM, String.valueOf(request.isIncludeIsoform()));
        }
        return Map.of();
    }

    private boolean isUniProtKBSourceType(MapToSearchRequest request) {
        return request.getFrom().equals(UniProtDataType.UNIPROTKB);
    }
}
