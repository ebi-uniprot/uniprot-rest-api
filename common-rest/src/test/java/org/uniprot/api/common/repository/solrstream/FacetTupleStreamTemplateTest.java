package org.uniprot.api.common.repository.solrstream;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;

import java.util.List;

import org.apache.http.client.HttpClient;
import org.apache.solr.client.solrj.io.stream.FacetStream;
import org.apache.solr.client.solrj.io.stream.TupleStream;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.uniprot.api.common.repository.search.SolrFacetRequest;
import org.uniprot.api.common.repository.search.SolrRequest;
import org.uniprot.api.common.repository.search.facet.FacetConfig;
import org.uniprot.api.common.repository.search.facet.FakeFacetConfig;
import org.uniprot.store.search.SolrCollection;

/**
 * @author sahmad
 */
class FacetTupleStreamTemplateTest {
    private static FacetTupleStreamTemplate tupleStreamTemplate;
    private static HttpClient httpClient;
    private static String collection = SolrCollection.uniprot.name();
    private static String zkHost = "localhost:2181";

    @BeforeAll
    static void setUp() {
        httpClient = mock(HttpClient.class);
        tupleStreamTemplate =
                FacetTupleStreamTemplate.builder()
                        .zookeeperHost(zkHost)
                        .collection(collection)
                        .build();
    }

    @Test
    void testCreateTupleStream() {
        SolrRequest.SolrRequestBuilder builder = SolrRequest.builder();
        String idsQuery = "accession_id:(P12345 OR Q12345)";
        SolrFacetRequest facet1 = SolrFacetRequest.builder().name("reviewed").build();
        SolrFacetRequest facet2 = SolrFacetRequest.builder().name("annotation").build();
        SolrFacetRequest facet3 = SolrFacetRequest.builder().name("fragment").build();
        String queryField = "id";
        List<SolrFacetRequest> facets = List.of(facet1, facet2, facet3);
        SolrRequest request =
                builder.idsQuery(idsQuery).queryField(queryField).facets(facets).build();
        FacetConfig facetConfig = new FakeFacetConfig();
        facetConfig.setLimit(5);
        TupleStream tupleStream = tupleStreamTemplate.create(request);
        assertThat(tupleStream, Matchers.is(Matchers.notNullValue()));
        assertThat(tupleStream.children(), Matchers.is(Matchers.iterableWithSize(3)));
        tupleStream
                .children()
                .forEach(child -> assertThat(child, Matchers.instanceOf(FacetStream.class)));
    }
}
