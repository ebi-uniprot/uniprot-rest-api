package org.uniprot.api.common.repository.solrstream;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;

import java.util.Arrays;
import java.util.List;

import org.apache.http.client.HttpClient;
import org.apache.solr.client.solrj.io.stream.FacetStream;
import org.apache.solr.client.solrj.io.stream.TupleStream;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
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
        SolrStreamFacetRequest.SolrStreamFacetRequestBuilder builder =
                SolrStreamFacetRequest.builder();
        String query = "accession_id:(P12345 OR Q12345)";
        String facet1 = "reviewed";
        String facet2 = "annotation";
        String facet3 = "fragment";
        List<String> facets = Arrays.asList(facet1, facet2, facet3);
        SolrStreamFacetRequest request = builder.query(query).facets(facets).build();
        FacetConfig facetConfig = new FakeFacetConfig();
        facetConfig.setLimit(5);
        TupleStream tupleStream = tupleStreamTemplate.create(request, facetConfig);
        assertThat(tupleStream, Matchers.is(Matchers.notNullValue()));
        assertThat(tupleStream.children(), Matchers.is(Matchers.iterableWithSize(3)));
        tupleStream
                .children()
                .forEach(child -> assertThat(child, Matchers.instanceOf(FacetStream.class)));
    }
}
