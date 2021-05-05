package org.uniprot.api.common.repository.solrstream;

import static org.hamcrest.MatcherAssert.assertThat;

import java.util.Arrays;
import java.util.List;

import org.apache.solr.client.solrj.io.stream.expr.StreamExpressionNamedParameter;
import org.apache.solr.client.solrj.io.stream.expr.StreamExpressionParameter;
import org.apache.solr.client.solrj.io.stream.expr.StreamExpressionValue;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.uniprot.api.common.repository.search.facet.FacetConfig;
import org.uniprot.api.common.repository.search.facet.FakeFacetConfig;

class FacetStreamExpressionTest {

    @Test
    void testCreate() {
        SolrStreamFacetRequest.SolrStreamFacetRequestBuilder builder =
                SolrStreamFacetRequest.builder();
        String collection = "sample collection";
        String query = "q:*:*";
        String buckets = "reviewed";
        String metrics = "count(reviewed)";
        String bucketSorts = "count(reviewed)";
        int bucketSizeLimit = 10;
        builder.query(query);
        builder.metrics(metrics).bucketSorts(bucketSorts).bucketSizeLimit(bucketSizeLimit);
        FacetConfig facetConfig = new FakeFacetConfig();
        FacetStreamExpression facetExpression =
                new FacetStreamExpression(collection, buckets, builder.build(), facetConfig);
        Assertions.assertNotNull(facetExpression);
        Assertions.assertEquals("facet", facetExpression.getFunctionName());
        Assertions.assertEquals(6, facetExpression.getParameters().size());

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
                                        query,
                                        buckets,
                                        bucketSorts,
                                        String.valueOf(bucketSizeLimit))));

            } else if (param instanceof StreamExpressionValue) {
                StreamExpressionValue valueParam = (StreamExpressionValue) param;
                assertThat(
                        valueParam.getValue(),
                        Matchers.isIn(Arrays.asList(collection, "count(*)")));
            }
        }
    }

    @Test
    void testCreateBucketSizeFailure() {
        SolrStreamFacetRequest.SolrStreamFacetRequestBuilder builder =
                SolrStreamFacetRequest.builder();
        String collection = "sample collection";
        String query = "q:*:*";
        String buckets = "facet";
        String metrics = "count(facet)";
        String bucketSorts = "count(facet)";
        int bucketSizeLimit = -1;
        builder.query(query);
        builder.metrics(metrics).bucketSorts(bucketSorts).bucketSizeLimit(bucketSizeLimit);
        FacetConfig facetConfig = new FakeFacetConfig();
        IllegalArgumentException exception =
                Assertions.assertThrows(
                        IllegalArgumentException.class,
                        () ->
                                new FacetStreamExpression(
                                        collection, buckets, builder.build(), facetConfig));
        Assertions.assertEquals(
                "bucketSizeLimit should be a positive integer", exception.getMessage());
    }

    @Test
    void testCreateFailureWithoutQuery() {
        SolrStreamFacetRequest.SolrStreamFacetRequestBuilder builder =
                SolrStreamFacetRequest.builder();
        String collection = "sample collection";
        FacetConfig facetConfig = new FakeFacetConfig();
        IllegalArgumentException exception =
                Assertions.assertThrows(
                        IllegalArgumentException.class,
                        () ->
                                new FacetStreamExpression(
                                        collection, "buckets", builder.build(), facetConfig));
        Assertions.assertEquals("query is a mandatory param", exception.getMessage());
    }

    @Test
    void testCreateFailureWithoutCollection() {
        SolrStreamFacetRequest.SolrStreamFacetRequestBuilder builder =
                SolrStreamFacetRequest.builder();
        FacetConfig facetConfig = new FakeFacetConfig();
        IllegalArgumentException exception =
                Assertions.assertThrows(
                        IllegalArgumentException.class,
                        () ->
                                new FacetStreamExpression(
                                        null, "buckets", builder.build(), facetConfig));
        Assertions.assertEquals("collection is a mandatory param", exception.getMessage());
    }

    @Test
    void testCreateMetricFailure() {
        SolrStreamFacetRequest.SolrStreamFacetRequestBuilder builder =
                SolrStreamFacetRequest.builder();
        String collection = "sample collection";
        String query = "q:*:*";
        String buckets = "reviewed";
        String metrics = "median(reviewed)";
        String bucketSorts = "count(reviewed)";
        int bucketSizeLimit = 1;
        builder.query(query);
        builder.metrics(metrics).bucketSorts(bucketSorts).bucketSizeLimit(bucketSizeLimit);
        FacetConfig facetConfig = new FakeFacetConfig();
        IllegalArgumentException exception =
                Assertions.assertThrows(
                        IllegalArgumentException.class,
                        () ->
                                new FacetStreamExpression(
                                        collection, buckets, builder.build(), facetConfig));
        Assertions.assertEquals("Unknown function median(reviewed)", exception.getMessage());
    }
}
