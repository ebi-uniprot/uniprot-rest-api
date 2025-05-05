package org.uniprot.api.mapto.controller;

import static org.uniprot.api.mapto.common.search.MapToSearchService.INCLUDE_ISOFORM;

import java.util.Map;

import org.springframework.stereotype.Component;
import org.uniprot.api.mapto.common.model.MapToJobRequest;
import org.uniprot.api.mapto.request.MapToSearchRequest;
import org.uniprot.api.mapto.request.UniProtKBMapToSearchRequest;
import org.uniprot.store.config.UniProtDataType;

@Component
public class MapToRequestConverter {
    public MapToJobRequest convert(MapToSearchRequest request) {
        String query = request.getQuery();
        MapToJobRequest mapToJobRequest =
                new MapToJobRequest(request.getFrom(), request.getTo(), query);
        mapToJobRequest.setExtraParams(getExtraParams(request));

        return mapToJobRequest;
    }

    private static Map<String, String> getExtraParams(MapToSearchRequest request) {
        if (isUniProtKBSourceType(request)) {
            return Map.of(
                    INCLUDE_ISOFORM,
                    "" + ((UniProtKBMapToSearchRequest) request).isIncludeIsoform());
        }
        return Map.of();
    }

    private static boolean isUniProtKBSourceType(MapToSearchRequest request) {
        return request.getFrom().equals(UniProtDataType.UNIPROTKB);
    }
}
