package org.uniprot.api.uniparc.common.service.light;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.uniprot.api.uniparc.common.service.light.UniParcLightEntryService.UNIPARC_ID_FIELD;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.uniprot.api.common.repository.search.SolrQueryConfig;
import org.uniprot.api.common.repository.search.SolrRequest;
import org.uniprot.api.common.repository.solrstream.FacetTupleStreamTemplate;
import org.uniprot.api.common.repository.stream.document.TupleStreamDocumentIdStream;
import org.uniprot.api.common.repository.stream.rdf.RdfStreamer;
import org.uniprot.api.common.repository.stream.store.StoreStreamer;
import org.uniprot.api.common.repository.stream.store.uniparc.UniParcCrossReferenceLazyLoader;
import org.uniprot.api.rest.respository.facet.impl.UniParcFacetConfig;
import org.uniprot.api.rest.service.request.RequestConverter;
import org.uniprot.api.uniparc.common.repository.search.UniParcQueryRepository;
import org.uniprot.api.uniparc.common.response.converter.UniParcLightQueryResultConverter;
import org.uniprot.api.uniparc.common.service.request.UniParcStreamRequest;
import org.uniprot.core.uniparc.UniParcEntryLight;
import org.uniprot.core.uniparc.impl.UniParcEntryLightBuilder;
import org.uniprot.store.config.searchfield.common.SearchFieldConfig;
import org.uniprot.store.config.searchfield.model.SearchFieldItem;
import org.uniprot.store.search.document.uniparc.UniParcDocument;

@ExtendWith(MockitoExtension.class)
public class UniParcLightEntryServiceTest {
    @Mock private UniParcQueryRepository repository;
    @Mock private UniParcFacetConfig facetConfig;
    @Mock private UniParcLightQueryResultConverter uniParcLightQueryResultConverter;
    @Mock private StoreStreamer<UniParcEntryLight> storeStreamer;
    @Mock private SolrQueryConfig uniParcSolrQueryConf;
    @Mock private SearchFieldConfig uniParcSearchFieldConfig;
    @Mock private RdfStreamer uniParcRdfStreamer;
    @Mock private FacetTupleStreamTemplate uniParcFacetTupleStreamTemplate;
    @Mock private TupleStreamDocumentIdStream uniParcTupleStreamDocumentIdStream;
    @Mock private UniParcCrossReferenceLazyLoader uniParcCrossReferenceLazyLoader;
    @Mock private RequestConverter requestConverter;

    private UniParcLightEntryService lightEntryService;

    @BeforeEach
    void init() {
        this.lightEntryService =
                new UniParcLightEntryService(
                        repository,
                        facetConfig,
                        uniParcLightQueryResultConverter,
                        storeStreamer,
                        uniParcSolrQueryConf,
                        uniParcSearchFieldConfig,
                        uniParcRdfStreamer,
                        uniParcFacetTupleStreamTemplate,
                        uniParcTupleStreamDocumentIdStream,
                        uniParcCrossReferenceLazyLoader,
                        requestConverter);
    }

    @Test
    void testFindByUniqueId() {
        // given
        String upi = "UPI000000017F";
        UniParcDocument doc1 = UniParcDocument.builder().upi(upi).build();
        UniParcEntryLight entryLight = new UniParcEntryLightBuilder().uniParcId(upi).build();
        SearchFieldItem searchField = new SearchFieldItem();
        searchField.setFieldName("upi");
        when(uniParcSearchFieldConfig.getSearchFieldItemByName(UNIPARC_ID_FIELD))
                .thenReturn(searchField);
        when(repository.getEntry(any())).thenReturn(Optional.of(doc1));
        when(uniParcLightQueryResultConverter.apply(doc1)).thenReturn(entryLight);
        // when
        UniParcEntryLight returnedEntry = this.lightEntryService.findByUniqueId(upi, null);
        // then
        Assertions.assertNotNull(returnedEntry);
        assertEquals(upi, returnedEntry.getUniParcId());
    }

    @Test
    void testStreamRDF() {
        String datatype = "uniparc";
        String format = "rdf";
        UniParcStreamRequest request = mock(UniParcStreamRequest.class);
        SolrRequest.SolrRequestBuilder solrRequestBuilder =
                mock(SolrRequest.SolrRequestBuilder.class);
        List<String> upis = List.of("UPI0000000001", "UPI0000000002", "UPI0000000003");
        Stream<String> rdfStream =
                List.of("UPI0000000001-rdf", "UPI0000000002-rdf", "UPI0000000003-rdf").stream();
        Mockito.lenient()
                .when(uniParcTupleStreamDocumentIdStream.fetchIds(any()))
                .thenReturn(upis.stream());
        Mockito.lenient()
                .when(uniParcRdfStreamer.stream(any(), any(), any()))
                .thenReturn(rdfStream);
        Stream<String> result = this.lightEntryService.streamRdf(request, datatype, format);
        Assertions.assertNotNull(result);
        List<String> resultList = result.toList();
        assertEquals(3, resultList.size());
        assertEquals(
                List.of("UPI0000000001-rdf", "UPI0000000002-rdf", "UPI0000000003-rdf"), resultList);
    }
}
