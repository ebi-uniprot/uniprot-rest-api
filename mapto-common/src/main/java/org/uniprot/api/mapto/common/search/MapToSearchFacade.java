package org.uniprot.api.mapto.common.search;

import org.springframework.stereotype.Component;
import org.uniprot.store.config.UniProtDataType;

@Component
public class MapToSearchFacade {
    private final UniProtKBMapToSearchService uniProtKBMapToSearchService;

    public MapToSearchFacade(UniProtKBMapToSearchService uniProtKBMapToSearchService) {
        this.uniProtKBMapToSearchService = uniProtKBMapToSearchService;
    }

    public MapToSearchService getMapToSearchService(UniProtDataType dataType) {
        switch (dataType) {
            case UNIPROTKB -> {
                return uniProtKBMapToSearchService;
            }
        }
        throw new IllegalArgumentException("Illegal or Unsupported Source Data type: " + dataType);
    }
}
