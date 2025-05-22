package org.uniprot.api.uniparc.common.service.light;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.uniprot.core.xml.CrossReferenceConverterUtils.PROPERTY_SOURCES;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

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
import org.uniprot.api.uniparc.common.service.request.UniParcDatabasesStreamRequest;
import org.uniprot.core.Property;
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
    void testGetCrossReferencesByUniParcId_filterWithDatabaseId_Success() {
        // given
        String uniParcId = "UPI000000001";
        UniParcDatabasesRequest request = new UniParcDatabasesRequest();
        request.setSize(10);
        String xRefId = "P10001";
        request.setId(xRefId);

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
        List<UniParcCrossReference> content = result.getContent().toList();
        assertTrue(content.stream().map(UniParcCrossReference::getId).allMatch(xRefId::equals));
        assertEquals(10, content.size());
        assertNotNull(result.getPage());
    }

    @Test
    void testGetCrossReferencesByUniParcId_includeSources_Success() {
        // given
        String uniParcId = "UPI000000001";
        UniParcDatabasesRequest request = new UniParcDatabasesRequest();
        request.setSize(10);
        request.setIncludeSources(true);

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
        List<UniParcCrossReference> content = result.getContent().toList();
        List<Property> propertyList =
                content.stream()
                        .map(UniParcCrossReference::getProperties)
                        .flatMap(List::stream)
                        .toList();
        List<String> keys = propertyList.stream().map(Property::getKey).toList();
        assertThat(keys, contains(PROPERTY_SOURCES, PROPERTY_SOURCES, PROPERTY_SOURCES));
        List<String> values = propertyList.stream().map(Property::getValue).toList();
        assertThat(
                values,
                contains(
                        "WP_168893201:UP000005640:chromosome",
                        "WP_168893201:UP000005640:chromosome",
                        "WP_168893201:UP000005640:chromosome"));
        assertEquals(10, content.size());
        assertNotNull(result.getPage());
    }

    @Test
    void testStreamCrossReferences() {
        when(uniParcLightStoreClient.getEntry(any())).thenReturn(Optional.empty());
        assertThrows(
                ResourceNotFoundException.class,
                () -> service.streamCrossReferencesByUniParcId("UPI000000001", null));
    }

    @Test
    void testStreamCrossReferences_success() {
        String uniParcId = "UPI000000001";
        UniParcEntryLight uniParcEntryLight =
                UniParcEntryMocker.createUniParcEntryLight(1, "UPI", 10);
        when(uniParcLightStoreClient.getEntry(uniParcId))
                .thenReturn(Optional.of(uniParcEntryLight));
        when(storeConfigProperties.getGroupSize()).thenReturn(1);
        UniParcCrossReferencePair pair =
                new UniParcCrossReferencePair(
                        "key", UniParcCrossReferenceMocker.createCrossReferences(1, 3));
        when(uniParcCrossReferenceStoreClient.getEntry(any())).thenReturn(Optional.of(pair));
        UniParcDatabasesStreamRequest request = new UniParcDatabasesStreamRequest();

        Stream<UniParcCrossReference> response =
                service.streamCrossReferencesByUniParcId("UPI000000001", request);

        assertEquals(30, response.toList().size());
    }

    @Test
    void testStreamCrossReferences_filterWithDatabaseId_success() {
        String uniParcId = "UPI000000001";
        UniParcEntryLight uniParcEntryLight =
                UniParcEntryMocker.createUniParcEntryLight(1, "UPI", 10);
        when(uniParcLightStoreClient.getEntry(uniParcId))
                .thenReturn(Optional.of(uniParcEntryLight));
        when(storeConfigProperties.getGroupSize()).thenReturn(1);
        UniParcCrossReferencePair pair =
                new UniParcCrossReferencePair(
                        "key", UniParcCrossReferenceMocker.createCrossReferences(1, 3));
        when(uniParcCrossReferenceStoreClient.getEntry(any())).thenReturn(Optional.of(pair));
        UniParcDatabasesStreamRequest request = new UniParcDatabasesStreamRequest();
        String xRefId = "P10001";
        request.setId(xRefId);

        Stream<UniParcCrossReference> response =
                service.streamCrossReferencesByUniParcId("UPI000000001", request);

        assertTrue(response.map(UniParcCrossReference::getId).allMatch(xRefId::equals));
    }

    @Test
    void testStreamCrossReferences_includeSources_success() {
        String uniParcId = "UPI000000001";
        UniParcEntryLight uniParcEntryLight =
                UniParcEntryMocker.createUniParcEntryLight(1, "UPI", 10);
        when(uniParcLightStoreClient.getEntry(uniParcId))
                .thenReturn(Optional.of(uniParcEntryLight));
        when(storeConfigProperties.getGroupSize()).thenReturn(1);
        UniParcCrossReferencePair pair =
                new UniParcCrossReferencePair(
                        "key", UniParcCrossReferenceMocker.createCrossReferences(1, 3));
        when(uniParcCrossReferenceStoreClient.getEntry(any())).thenReturn(Optional.of(pair));
        UniParcDatabasesStreamRequest request = new UniParcDatabasesStreamRequest();
        request.setIncludeSources(true);

        Stream<UniParcCrossReference> response =
                service.streamCrossReferencesByUniParcId("UPI000000001", request);

        List<Property> propertyList =
                response.map(UniParcCrossReference::getProperties).flatMap(List::stream).toList();
        List<String> keys = propertyList.stream().map(Property::getKey).toList();
        assertThat(
                keys,
                contains(
                        PROPERTY_SOURCES,
                        PROPERTY_SOURCES,
                        PROPERTY_SOURCES,
                        PROPERTY_SOURCES,
                        PROPERTY_SOURCES,
                        PROPERTY_SOURCES,
                        PROPERTY_SOURCES,
                        PROPERTY_SOURCES,
                        PROPERTY_SOURCES,
                        PROPERTY_SOURCES));
        List<String> values = propertyList.stream().map(Property::getValue).toList();
        assertThat(
                values,
                contains(
                        "WP_168893201:UP000005640:chromosome",
                        "WP_168893201:UP000005640:chromosome",
                        "WP_168893201:UP000005640:chromosome",
                        "WP_168893201:UP000005640:chromosome",
                        "WP_168893201:UP000005640:chromosome",
                        "WP_168893201:UP000005640:chromosome",
                        "WP_168893201:UP000005640:chromosome",
                        "WP_168893201:UP000005640:chromosome",
                        "WP_168893201:UP000005640:chromosome",
                        "WP_168893201:UP000005640:chromosome"));
    }
}
