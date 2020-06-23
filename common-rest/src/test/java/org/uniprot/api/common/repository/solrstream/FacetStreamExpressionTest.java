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

public class FacetStreamExpressionTest {

    @Test
    void testCreate() {
        FacetStreamExpression.FacetStreamExpressionBuilder builder =
                new FacetStreamExpression.FacetStreamExpressionBuilder();
        String collection = "sample collection";
        String query = "q:*:*";
        String buckets = "facet";
        String metrics = "count(facet)";
        String bucketSorts = "count(facet)";
        int bucketSizeLimit = 10;
        builder.collection(collection).query(query).buckets(buckets);
        builder.metrics(metrics).bucketSorts(bucketSorts).bucketSizeLimit(bucketSizeLimit);
        FacetStreamExpression facetExpression = builder.build();
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
        FacetStreamExpression.FacetStreamExpressionBuilder builder =
                new FacetStreamExpression.FacetStreamExpressionBuilder();
        String collection = "sample collection";
        String query = "q:*:*";
        String buckets = "facet";
        String metrics = "count(facet)";
        String bucketSorts = "count(facet)";
        int bucketSizeLimit = -1;
        builder.collection(collection).query(query).buckets(buckets);
        builder.metrics(metrics).bucketSorts(bucketSorts).bucketSizeLimit(bucketSizeLimit);
        IllegalArgumentException exception =
                Assertions.assertThrows(IllegalArgumentException.class, () -> builder.build());
        Assertions.assertEquals(
                "bucketSizeLimit should be a positive integer", exception.getMessage());
    }

    @Test
    void testCreateFailureWithoutQuery() {
        FacetStreamExpression.FacetStreamExpressionBuilder builder =
                new FacetStreamExpression.FacetStreamExpressionBuilder();
        String collection = "sample collection";
        builder.collection(collection);
        IllegalArgumentException exception =
                Assertions.assertThrows(IllegalArgumentException.class, () -> builder.build());
        Assertions.assertEquals("query is a mandatory param", exception.getMessage());
    }

    @Test
    void testCreateFailureWithoutCollection() {
        FacetStreamExpression.FacetStreamExpressionBuilder builder =
                new FacetStreamExpression.FacetStreamExpressionBuilder();
        IllegalArgumentException exception =
                Assertions.assertThrows(IllegalArgumentException.class, () -> builder.build());
        Assertions.assertEquals("collection is a mandatory param", exception.getMessage());
    }

    @Test
    void testCreateMetricFailure() {
        FacetStreamExpression.FacetStreamExpressionBuilder builder =
                new FacetStreamExpression.FacetStreamExpressionBuilder();
        String collection = "sample collection";
        String query = "q:*:*";
        String buckets = "facet";
        String metrics = "median(facet)";
        String bucketSorts = "count(facet)";
        int bucketSizeLimit = 1;
        builder.collection(collection).query(query).buckets(buckets);
        builder.metrics(metrics).bucketSorts(bucketSorts).bucketSizeLimit(bucketSizeLimit);
        IllegalArgumentException exception =
                Assertions.assertThrows(IllegalArgumentException.class, () -> builder.build());
        Assertions.assertEquals("Unknown function median(facet)", exception.getMessage());
    }
}
