package org.uniprot.api.common.repository.solrstream;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import org.apache.solr.client.solrj.io.stream.expr.StreamExpression;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.uniprot.api.common.repository.search.facet.FacetConfig;
import org.uniprot.api.common.repository.search.facet.FakeFacetConfig;

class ListStreamExpressionTest {

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
        String buckets = "reviewed";
        String metrics = "count(reviewed)";
        String bucketSorts = "count(reviewed)";
        int bucketSizeLimit = ThreadLocalRandom.current().nextInt(10, Integer.MAX_VALUE);
        FacetConfig facetConfig = new FakeFacetConfig();
        builder.query(query)
                .metrics(metrics)
                .bucketSorts(bucketSorts)
                .bucketSizeLimit(bucketSizeLimit);
        return new FacetStreamExpression(collection, buckets, builder.build(), facetConfig);
    }
}
