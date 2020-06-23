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
import org.uniprot.store.search.SolrCollection;

/** @author sahmad */
class TupleStreamTemplateTest {
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
                        .httpClient(httpClient)
                        .build();
    }

    @Test
    void testCreateTupleStream() {
        SolrStreamingFacetRequest.SolrStreamingFacetRequestBuilder builder =
                SolrStreamingFacetRequest.builder();
        String query = "accession_id:(P12345 OR Q12345)";
        String facet1 = "sample_facet1";
        String facet2 = "sample_facet2";
        String facet3 = "sample_facet3";
        List<String> facets = Arrays.asList(facet1, facet2, facet3);
        SolrStreamingFacetRequest request = builder.query(query).facets(facets).build();
        TupleStream tupleStream = tupleStreamTemplate.create(request);
        assertThat(tupleStream, Matchers.is(Matchers.notNullValue()));
        assertThat(tupleStream.children(), Matchers.is(Matchers.iterableWithSize(3)));
        tupleStream
                .children()
                .forEach(child -> assertThat(child, Matchers.instanceOf(FacetStream.class)));
    }
}
