package org.uniprot.api.common.repository.solrstream;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import org.apache.solr.client.solrj.io.stream.expr.StreamExpression;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.uniprot.api.common.repository.search.SolrFacetRequest;
import org.uniprot.api.common.repository.search.SolrRequest;

class ListStreamExpressionTest {

    @Test
    void testCreateList() {
        List<StreamExpression> streamExpressions = new ArrayList<>();
        streamExpressions.add(createExpression());
        streamExpressions.add(createExpression());
        ListStreamExpression listStreamExpression = new ListStreamExpression(streamExpressions);
        Assertions.assertNotNull(listStreamExpression);
        Assertions.assertEquals("plist", listStreamExpression.getFunctionName());
        Assertions.assertEquals(2, listStreamExpression.getParameters().size());
    }

    private FacetStreamExpression createExpression() {
        SolrRequest.SolrRequestBuilder builder = SolrRequest.builder();
        String collection = "sample collection";
        String query = "q:*:*";
        String buckets = "reviewed";
        String bucketSorts = "count(reviewed)";
        int bucketSizeLimit = ThreadLocalRandom.current().nextInt(10, Integer.MAX_VALUE);
        SolrFacetRequest solrFacetRequest =
                SolrFacetRequest.builder()
                        .name(buckets)
                        .sort(bucketSorts)
                        .limit(bucketSizeLimit)
                        .build();
        builder.query(query);
        return new FacetStreamExpression(collection, builder.build(), solrFacetRequest);
    }
}
