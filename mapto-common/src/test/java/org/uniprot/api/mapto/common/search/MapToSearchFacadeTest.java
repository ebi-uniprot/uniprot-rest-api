package org.uniprot.api.mapto.common.search;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.uniprot.store.config.UniProtDataType;

@ExtendWith(MockitoExtension.class)
class MapToSearchFacadeTest {
    @Mock private UniProtKBMapToSearchService uniProtKBMapToSearchService;
    @InjectMocks private MapToSearchFacade mapToSearchFacade;

    @Test
    void getMapToSearchService_uniProtKB() {
        MapToSearchService result =
                mapToSearchFacade.getMapToSearchService(UniProtDataType.UNIPROTKB);

        assertEquals(uniProtKBMapToSearchService, result);
    }
}
