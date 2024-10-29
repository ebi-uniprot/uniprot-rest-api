package org.uniprot.api.uniparc.common.service;

import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.uniprot.api.common.repository.search.SolrQueryConfig;
import org.uniprot.api.common.repository.solrstream.FacetTupleStreamTemplate;
import org.uniprot.api.common.repository.stream.document.TupleStreamDocumentIdStream;
import org.uniprot.api.common.repository.stream.rdf.RdfStreamer;
import org.uniprot.api.rest.output.UniProtMediaType;
import org.uniprot.api.rest.respository.facet.impl.UniParcFacetConfig;
import org.uniprot.api.rest.service.request.RequestConverter;
import org.uniprot.api.uniparc.common.repository.search.UniParcQueryRepository;
import org.uniprot.api.uniparc.common.repository.store.light.UniParcLightStoreClient;
import org.uniprot.api.uniparc.common.service.light.UniParcCrossReferenceService;
import org.uniprot.api.uniparc.common.service.request.UniParcSearchRequest;
import org.uniprot.api.uniparc.common.service.request.UniParcStreamRequest;
import org.uniprot.store.config.searchfield.common.SearchFieldConfig;

/**
 * @author sahmad
 * @created 13/06/2023
 */
@ExtendWith(MockitoExtension.class)
class UniParcEntryServiceTest {
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
    private UniParcEntryService service;

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
                        uniParcRequestConverter);
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
        List<String> upis =
                List.of(
                        "UPI0000000001",
                        "UPI0000000002",
                        "UPI0000000003",
                        "UPI0000000004",
                        "UPI0000000005");
        request.setQuery("field:value");
        request.setFormat(UniProtMediaType.LIST_MEDIA_TYPE_VALUE);
        assertThrows(UnsupportedOperationException.class, () -> service.stream(request));
    }
}
