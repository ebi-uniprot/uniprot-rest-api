package org.uniprot.api.uniparc.common.service.light;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.uniprot.api.common.exception.ResourceNotFoundException;
import org.uniprot.api.common.repository.search.QueryResult;
import org.uniprot.api.common.repository.stream.store.uniparc.UniParcCrossReferenceStoreConfigProperties;
import org.uniprot.api.uniparc.common.repository.store.crossref.UniParcCrossReferenceFacetConfig;
import org.uniprot.api.uniparc.common.repository.store.crossref.UniParcCrossReferenceStoreClient;
import org.uniprot.api.uniparc.common.repository.store.light.UniParcLightStoreClient;
import org.uniprot.api.uniparc.common.service.request.UniParcDatabasesRequest;
import org.uniprot.core.uniparc.UniParcCrossReference;
import org.uniprot.core.uniparc.UniParcEntryLight;
import org.uniprot.core.uniparc.impl.UniParcCrossReferencePair;
import org.uniprot.store.indexer.uniparc.mockers.UniParcCrossReferenceMocker;
import org.uniprot.store.indexer.uniparc.mockers.UniParcEntryMocker;

@ExtendWith(MockitoExtension.class)
class UniParcCrossReferenceServiceTest {
    @Mock private UniParcLightStoreClient uniParcLightStoreClient;
    @Mock private UniParcCrossReferenceStoreClient uniParcCrossReferenceStoreClient;
    @Mock private UniParcCrossReferenceStoreConfigProperties storeConfigProperties;

    @Mock private UniParcCrossReferenceFacetConfig uniParcCrossReferenceFacetConfig;
    private UniParcCrossReferenceService service;

    @BeforeEach
    void init() {
        when(storeConfigProperties.getFetchRetryDelayMillis()).thenReturn(1000);
        this.service =
                new UniParcCrossReferenceService(
                        uniParcLightStoreClient,
                        uniParcCrossReferenceStoreClient,
                        storeConfigProperties,
                        uniParcCrossReferenceFacetConfig);
    }

    @Test
    void testGetCrossReferencesByUniParcId_Success() {
        // given
        String uniParcId = "UPI000000001";
        UniParcDatabasesRequest request = new UniParcDatabasesRequest();
        request.setSize(10); // set request size

        UniParcEntryLight uniParcEntryLight =
                UniParcEntryMocker.createUniParcEntryLight(1, "UPI", 10);
        // when
        when(uniParcLightStoreClient.getEntry(uniParcId))
                .thenReturn(Optional.of(uniParcEntryLight));
        when(storeConfigProperties.getGroupSize()).thenReturn(1);
        UniParcCrossReferencePair pair =
                new UniParcCrossReferencePair(
                        "key", UniParcCrossReferenceMocker.createCrossReferences(1, 3));
        when(uniParcCrossReferenceStoreClient.getEntry(any())).thenReturn(Optional.of(pair));
        // when
        QueryResult<UniParcCrossReference> result =
                service.getCrossReferencesByUniParcId(uniParcId, request);

        // then
        assertNotNull(result);
        assertEquals(10, result.getContent().count());
        assertNotNull(result.getPage());
    }

    @Test
    void testStreamCrossReferences() {
        when(uniParcLightStoreClient.getEntry(any())).thenReturn(Optional.empty());
        assertThrows(
                ResourceNotFoundException.class, () -> service.streamCrossReferences("UPI000000001", null));
    }
}
