package org.uniprot.api.common.repository.solrstream;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import org.apache.solr.client.solrj.io.stream.expr.StreamExpression;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class ListStreamExpressionTest {

    @Test
    void testCreateList() {
        List<StreamExpression> streamExpressions = new ArrayList<>();
        streamExpressions.add(createExpression());
        streamExpressions.add(createExpression());
        ListStreamExpression listStreamExpression = new ListStreamExpression(streamExpressions);
        Assertions.assertNotNull(listStreamExpression);
        Assertions.assertEquals("list", listStreamExpression.getFunctionName());
        Assertions.assertEquals(2, listStreamExpression.getParameters().size());
    }

    private FacetStreamExpression createExpression() {
        SolrStreamFacetRequest.SolrStreamFacetRequestBuilder builder =
                SolrStreamFacetRequest.builder();
        String collection = "sample collection";
        String query = "q:*:*";
        String buckets = "facet";
        String metrics = "count(facet)";
        String bucketSorts = "count(facet)";
        int bucketSizeLimit = ThreadLocalRandom.current().nextInt(10, Integer.MAX_VALUE);
        builder.query(query)
                .metrics(metrics)
                .bucketSorts(bucketSorts)
                .bucketSizeLimit(bucketSizeLimit);
        return new FacetStreamExpression(collection, buckets, builder.build());
    }
}
