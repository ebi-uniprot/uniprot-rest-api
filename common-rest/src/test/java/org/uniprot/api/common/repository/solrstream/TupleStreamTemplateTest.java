package org.uniprot.api.common.repository.solrstream;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;

import java.util.Arrays;
import java.util.List;

import org.apache.http.client.HttpClient;
import org.apache.solr.client.solrj.io.stream.FacetStream;
import org.apache.solr.client.solrj.io.stream.TupleStream;
import org.apache.solr.client.solrj.io.stream.expr.*;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.uniprot.store.search.SolrCollection;

/** @author sahmad */
class TupleStreamTemplateTest {
    private static TupleStreamTemplate tupleStreamTemplate;
    private static HttpClient httpClient;
    private static String collection = SolrCollection.uniprot.name();
    private static String zkHost = "localhost:2181";

    @BeforeAll
    static void setUp() {
        httpClient = mock(HttpClient.class);
        tupleStreamTemplate =
                TupleStreamTemplate.builder()
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

    @Test
    void testCreateFacetExpression() {
        SolrStreamingFacetRequest.SolrStreamingFacetRequestBuilder builder =
                SolrStreamingFacetRequest.builder();
        String query = "accession_id:(P12345 OR Q12345)";
        String facet = "sample_facet";
        SolrStreamingFacetRequest request =
                builder.query(query).facets(Arrays.asList(facet)).build();
        StreamExpression facetExpression =
                tupleStreamTemplate.createFacetExpression(facet, request);

        assertThat(facetExpression, Matchers.is(Matchers.notNullValue()));
        assertThat(facetExpression.getFunctionName(), Matchers.is(Matchers.equalTo("facet")));
        assertThat(facetExpression.getParameters(), Matchers.iterableWithSize(6));
        List<StreamExpressionParameter> params = facetExpression.getParameters();

        for (StreamExpressionParameter param : params) {
            if (param instanceof StreamExpressionNamedParameter) {
                StreamExpressionNamedParameter namedParam = (StreamExpressionNamedParameter) param;
                assertThat(
                        namedParam.getName(),
                        Matchers.isIn(
                                Arrays.asList("q", "buckets", "bucketSorts", "bucketSizeLimit")));
                assertThat(
                        ((StreamExpressionValue) namedParam.getParameter()).getValue(),
                        Matchers.isIn(
                                Arrays.asList(
                                        "accession_id:(P12345 OR Q12345)",
                                        "sample_facet",
                                        "count(*) desc",
                                        "1000")));

            } else if (param instanceof StreamExpressionValue) {
                StreamExpressionValue valueParam = (StreamExpressionValue) param;
                assertThat(
                        valueParam.getValue(),
                        Matchers.isIn(Arrays.asList(collection, "count(*)")));
            }
        }
    }
}
