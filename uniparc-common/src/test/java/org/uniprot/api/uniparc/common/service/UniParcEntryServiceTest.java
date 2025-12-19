package org.uniprot.api.uniparc.common.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.uniprot.api.common.exception.ResourceNotFoundException;
import org.uniprot.api.common.repository.search.SolrQueryConfig;
import org.uniprot.api.common.repository.solrstream.FacetTupleStreamTemplate;
import org.uniprot.api.common.repository.stream.document.TupleStreamDocumentIdStream;
import org.uniprot.api.common.repository.stream.rdf.RdfStreamer;
import org.uniprot.api.common.repository.stream.store.StoreStreamer;
import org.uniprot.api.rest.output.UniProtMediaType;
import org.uniprot.api.rest.respository.facet.impl.UniParcFacetConfig;
import org.uniprot.api.rest.service.request.RequestConverter;
import org.uniprot.api.uniparc.common.repository.search.UniParcQueryRepository;
import org.uniprot.api.uniparc.common.repository.store.light.UniParcLightStoreClient;
import org.uniprot.api.uniparc.common.service.light.UniParcCrossReferenceService;
import org.uniprot.api.uniparc.common.service.request.UniParcSearchRequest;
import org.uniprot.api.uniparc.common.service.request.UniParcStreamRequest;
import org.uniprot.core.Sequence;
import org.uniprot.core.uniparc.SequenceFeature;
import org.uniprot.core.uniparc.UniParcCrossReference;
import org.uniprot.core.uniparc.UniParcEntry;
import org.uniprot.core.uniparc.UniParcEntryLight;
import org.uniprot.store.config.searchfield.common.SearchFieldConfig;

/**
 * @author sahmad
 * @created 13/06/2023
 */
@ExtendWith(MockitoExtension.class)
class UniParcEntryServiceTest {
    private static final String UPI = "upi";
    private static final String X_REF_ID = "xRefId";
    @Mock private UniParcQueryRepository repository;
    @Mock private UniParcFacetConfig facetConfig;
    @Mock private SolrQueryConfig uniParcSolrQueryConf;
    @Mock private SearchFieldConfig uniParcSearchFieldConfig;
    @Mock private RdfStreamer uniparcRdfStreamer;
    @Mock private FacetTupleStreamTemplate facetTupleStreamTemplate;
    @Mock private TupleStreamDocumentIdStream solrIdStreamer;
    @Mock private UniParcLightStoreClient uniParcLightStoreClient;
    @Mock private UniParcCrossReferenceService uniParcCrossReferenceService;

    @Mock private RequestConverter uniParcRequestConverter;
    @Mock private StoreStreamer<UniParcEntry> uniParcFastaStoreStreamer;
    private UniParcEntryService service;
    @Mock private UniParcEntryLight uniParcLight;
    @Mock private Sequence sequence;
    private final List<SequenceFeature> sequenceFeatures = List.of();
    @Mock private UniParcCrossReference xRef0;
    @Mock private UniParcCrossReference xRef1;

    @BeforeEach
    void init() {
        service =
                new UniParcEntryService(
                        repository,
                        facetConfig,
                        uniParcSolrQueryConf,
                        uniParcSearchFieldConfig,
                        uniparcRdfStreamer,
                        facetTupleStreamTemplate,
                        solrIdStreamer,
                        uniParcLightStoreClient,
                        uniParcCrossReferenceService,
                        uniParcRequestConverter,
                        uniParcFastaStoreStreamer);
    }

    @Test
    void searchThrowsUnsupportedOperation() {
        // when
        UniParcSearchRequest searchRequest = new UniParcSearchRequest();
        searchRequest.setQuery("field:value");
        searchRequest.setFormat(MediaType.APPLICATION_JSON_VALUE);
        searchRequest.setSize(10);
        // then
        assertThrows(UnsupportedOperationException.class, () -> service.search(searchRequest));
    }

    @Test
    void streamThrowsUnsupportedOperationException() {
        UniParcStreamRequest request = new UniParcStreamRequest();
        request.setQuery("field:value");
        request.setFormat(UniProtMediaType.LIST_MEDIA_TYPE_VALUE);
        assertThrows(UnsupportedOperationException.class, () -> service.stream(request));
    }

    @Test
    void getUniParcEntryByUpiAndXrefId() {
        when(uniParcLightStoreClient.getEntry(UPI)).thenReturn(Optional.of(uniParcLight));
        when(uniParcLight.getUniParcId()).thenReturn(UPI);
        when(uniParcLight.getSequence()).thenReturn(sequence);
        when(uniParcLight.getSequenceFeatures()).thenReturn(sequenceFeatures);
        when(uniParcCrossReferenceService.getCrossReferences(uniParcLight, true))
                .thenReturn(Stream.of(xRef0));
        when(xRef0.getId()).thenReturn(X_REF_ID);

        UniParcEntry uniParcEntry = service.getUniParcEntry(UPI, X_REF_ID);

        assertSame(UPI, uniParcEntry.getUniParcId().getValue());
        assertSame(sequence, uniParcEntry.getSequence());
        assertEquals(sequenceFeatures, uniParcEntry.getSequenceFeatures());
        assertEquals(List.of(xRef0), uniParcEntry.getUniParcCrossReferences());
    }

    @Test
    void getUniParcEntryByUpiAndXrefId_whenMoreThanOneXref() {
        when(uniParcLightStoreClient.getEntry(UPI)).thenReturn(Optional.of(uniParcLight));
        when(uniParcLight.getUniParcId()).thenReturn(UPI);
        when(uniParcLight.getSequence()).thenReturn(sequence);
        when(uniParcLight.getSequenceFeatures()).thenReturn(sequenceFeatures);
        when(uniParcCrossReferenceService.getCrossReferences(uniParcLight, true))
                .thenReturn(Stream.of(xRef0, xRef1));
        when(xRef0.getId()).thenReturn(X_REF_ID);
        when(xRef0.getLastUpdated()).thenReturn(LocalDate.MAX);
        when(xRef1.getId()).thenReturn(X_REF_ID);
        when(xRef1.getLastUpdated()).thenReturn(LocalDate.MIN);

        UniParcEntry uniParcEntry = service.getUniParcEntry(UPI, X_REF_ID);

        assertSame(UPI, uniParcEntry.getUniParcId().getValue());
        assertSame(sequence, uniParcEntry.getSequence());
        assertEquals(sequenceFeatures, uniParcEntry.getSequenceFeatures());
        assertEquals(List.of(xRef0), uniParcEntry.getUniParcCrossReferences());
    }

    @Test
    void getUniParcEntryByUpiAndXrefId_invalidUpi() {
        when(uniParcLightStoreClient.getEntry(UPI)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.getUniParcEntry(UPI, X_REF_ID));
    }

    @Test
    void getUniParcEntryByUpiAndXrefId_invalidXref() {
        when(uniParcLightStoreClient.getEntry(UPI)).thenReturn(Optional.of(uniParcLight));
        when(uniParcCrossReferenceService.getCrossReferences(uniParcLight, true))
                .thenReturn(Stream.of());

        assertThrows(ResourceNotFoundException.class, () -> service.getUniParcEntry(UPI, X_REF_ID));
    }
}
