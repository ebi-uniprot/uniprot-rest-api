package org.uniprot.api.common.repository.solrstream;

import static org.uniprot.api.common.repository.solrstream.FacetStreamExpression.DEFAULT_BUCKET_SIZE;
import static org.uniprot.api.common.repository.solrstream.FacetStreamExpression.DEFAULT_BUCKET_SORTS;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.solr.client.solrj.io.stream.expr.StreamExpressionNamedParameter;
import org.apache.solr.client.solrj.io.stream.expr.StreamExpressionValue;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.uniprot.api.common.repository.search.SolrFacetRequest;
import org.uniprot.api.common.repository.search.SolrRequest;

class FacetStreamExpressionTest {

    @Test
    void testCreateFullFacetStreamExpression() {
        SolrRequest.SolrRequestBuilder builder = SolrRequest.builder();
        String collection = "sample collection";
        String query = "q:*:*";
        String idsQuery = "P21802 OR P12345";
        String idFields = "id_field";
        String queryField = "field1 field2";
        String buckets = "reviewed";
        String bucketSorts = "count(*)";
        int bucketSizeLimit = 10;
        builder.query(query);
        builder.idField(idFields);
        builder.idsQuery(idsQuery);
        builder.queryField(queryField);
        SolrFacetRequest facetRequest =
                SolrFacetRequest.builder()
                        .name(buckets)
                        .limit(bucketSizeLimit)
                        .minCount(1)
                        .sort(bucketSorts)
                        .build();
        builder.facet(facetRequest);
        FacetStreamExpression facetExpression =
                new FacetStreamExpression(collection, builder.build(), facetRequest);
        Assertions.assertNotNull(facetExpression);
        Assertions.assertEquals("facet", facetExpression.getFunctionName());
        Assertions.assertEquals(10, facetExpression.getParameters().size());

        Map<String, String> params = getMappedParameters(facetExpression);
        Assertions.assertEquals(8, params.size());
        Assertions.assertEquals(query, params.get("q"));
        Assertions.assertEquals(buckets, params.get("buckets"));
        Assertions.assertEquals(bucketSorts, params.get("bucketSorts"));
        Assertions.assertEquals(String.valueOf(bucketSizeLimit), params.get("bucketSizeLimit"));
        Assertions.assertEquals("edismax", params.get("defType"));
        Assertions.assertEquals(queryField, params.get("qf"));
        Assertions.assertEquals(idsQuery, params.get("fq"));
        Assertions.assertEquals("AND", params.get("q.op"));

        List<String> expressionValue = getExpressionValues(facetExpression);
        Assertions.assertEquals(List.of(collection), expressionValue);
    }

    @Test
    void testCreateMinimunParameters() {
        String idsQuery = "P21802 OR P12345";
        String buckets = "reviewed";
        String collection = "collection";
        SolrRequest.SolrRequestBuilder builder = SolrRequest.builder();
        builder.idsQuery(idsQuery);
        SolrFacetRequest facetRequest = SolrFacetRequest.builder().name(buckets).build();
        builder.facet(facetRequest);

        FacetStreamExpression facetExpression =
                new FacetStreamExpression(collection, builder.build(), facetRequest);
        Assertions.assertNotNull(facetExpression);
        Assertions.assertEquals("facet", facetExpression.getFunctionName());
        Assertions.assertEquals(7, facetExpression.getParameters().size());
        Map<String, String> params = getMappedParameters(facetExpression);
        Assertions.assertEquals(5, params.size());
        Assertions.assertEquals("*:*", params.get("q"));
        Assertions.assertEquals(idsQuery, params.get("fq"));
        Assertions.assertEquals(buckets, params.get("buckets"));
        Assertions.assertEquals(DEFAULT_BUCKET_SORTS, params.get("bucketSorts"));
        Assertions.assertEquals(DEFAULT_BUCKET_SIZE, params.get("bucketSizeLimit"));
        Assertions.assertNull(params.get("q.op"));
        Assertions.assertNull(params.get("defType"));
        Assertions.assertNull(params.get("qf"));

        List<String> expressionValue = getExpressionValues(facetExpression);
        Assertions.assertEquals(List.of(collection), expressionValue);
    }

    @Test
    void testCreateWithIntervalReturnDefaultBucketSize() {
        String idsQuery = "P21802 OR P12345";
        String buckets = "reviewed";
        SolrRequest.SolrRequestBuilder builder = SolrRequest.builder();
        builder.idsQuery(idsQuery);
        SolrFacetRequest facetRequest =
                SolrFacetRequest.builder()
                        .name(buckets)
                        .limit(10)
                        .interval(Map.of("10", "20"))
                        .build();
        builder.facet(facetRequest);

        FacetStreamExpression facetExpression =
                new FacetStreamExpression("collection", builder.build(), facetRequest);
        Assertions.assertNotNull(facetExpression);
        Assertions.assertEquals("facet", facetExpression.getFunctionName());
        Assertions.assertEquals(7, facetExpression.getParameters().size());
        Map<String, String> params = getMappedParameters(facetExpression);
        Assertions.assertEquals(5, params.size());
        Assertions.assertEquals("*:*", params.get("q"));
        Assertions.assertEquals(idsQuery, params.get("fq"));
        Assertions.assertEquals(buckets, params.get("buckets"));
        Assertions.assertEquals(DEFAULT_BUCKET_SORTS, params.get("bucketSorts"));
        Assertions.assertEquals(DEFAULT_BUCKET_SIZE, params.get("bucketSizeLimit"));
    }

    @Test
    void testCreateFailureWithoutQuery() {
        SolrRequest.SolrRequestBuilder builder = SolrRequest.builder();
        String collection = "sample collection";
        SolrFacetRequest facetRequest = SolrFacetRequest.builder().build();
        builder.facet(facetRequest);
        IllegalArgumentException exception =
                Assertions.assertThrows(
                        IllegalArgumentException.class,
                        () -> new FacetStreamExpression(collection, builder.build(), facetRequest));
        Assertions.assertEquals("query or Ids is a mandatory param", exception.getMessage());
    }

    @Test
    void testCreateFailureWithoutCollection() {
        SolrRequest.SolrRequestBuilder builder = SolrRequest.builder();
        SolrFacetRequest facetRequest = SolrFacetRequest.builder().build();
        builder.facet(facetRequest);
        IllegalArgumentException exception =
                Assertions.assertThrows(
                        IllegalArgumentException.class,
                        () -> new FacetStreamExpression(null, builder.build(), facetRequest));
        Assertions.assertEquals("collection is a mandatory param", exception.getMessage());
    }

    @Test
    void testCreateFullFacetStreamExpressionWithQuery() {
        SolrRequest.SolrRequestBuilder builder = SolrRequest.builder();
        String collection = "sample collection";
        String query = "p53";
        String idsQuery = "P21802 OR P12345";
        String idFields = "id_field";
        String queryField = "field1 field2";
        String buckets = "reviewed";
        String bucketSorts = "count(*)";
        int bucketSizeLimit = 10;
        builder.query(query);
        builder.idField(idFields);
        builder.idsQuery(idsQuery);
        builder.queryField(queryField);
        SolrFacetRequest facetRequest =
                SolrFacetRequest.builder()
                        .name(buckets)
                        .limit(bucketSizeLimit)
                        .minCount(1)
                        .sort(bucketSorts)
                        .build();
        builder.facet(facetRequest);
        FacetStreamExpression facetExpression =
                new FacetStreamExpression(collection, builder.build(), facetRequest);
        Assertions.assertNotNull(facetExpression);
        Assertions.assertEquals("facet", facetExpression.getFunctionName());
        Assertions.assertEquals(10, facetExpression.getParameters().size());
        Map<String, String> params = getMappedParameters(facetExpression);
        Assertions.assertEquals("p53", params.get("q"));
        Assertions.assertEquals("AND", params.get("q.op"));
        Assertions.assertEquals("edismax", params.get("defType"));
        Assertions.assertEquals(queryField, params.get("qf"));
        Assertions.assertEquals(idsQuery, params.get("fq"));
    }

    private static List<String> getExpressionValues(FacetStreamExpression facetExpression) {
        return facetExpression.getParameters().stream()
                .filter(p -> p instanceof StreamExpressionValue)
                .map(p -> (StreamExpressionValue) p)
                .map(StreamExpressionValue::getValue)
                .toList();
    }

    private static Map<String, String> getMappedParameters(FacetStreamExpression facetExpression) {
        return facetExpression.getParameters().stream()
                .filter(p -> p instanceof StreamExpressionNamedParameter)
                .map(p -> (StreamExpressionNamedParameter) p)
                .collect(
                        Collectors.groupingBy(
                                StreamExpressionNamedParameter::getName,
                                Collectors.mapping(
                                        FacetStreamExpressionTest::getParameterValues,
                                        Collectors.joining(","))));
    }

    private static String getParameterValues(StreamExpressionNamedParameter p) {
        StreamExpressionValue ep = (StreamExpressionValue) p.getParameter();
        return ep.getValue();
    }
}
