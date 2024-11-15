package org.uniprot.api.common.repository.solrstream;

import org.apache.solr.client.solrj.io.stream.expr.StreamExpression;
import org.apache.solr.client.solrj.io.stream.expr.StreamExpressionNamedParameter;
import org.apache.solr.client.solrj.io.stream.expr.StreamExpressionValue;
import org.uniprot.api.common.repository.search.SolrFacetRequest;
import org.uniprot.api.common.repository.search.SolrRequest;
import org.uniprot.core.util.Utils;

import lombok.Getter;

/**
 * This class creates expression to make solr streaming facet function call.
 *
 * @author sahmad
 */
@Getter
public class FacetStreamExpression extends UniProtStreamExpression {
    static final String DEFAULT_BUCKET_SIZE = "1000";
    static final String DEFAULT_BUCKET_SORTS = "count(*) desc";

    public enum MetricFunctionName {
        avg,
        count,
        max,
        min,
        sum;
    }

    public FacetStreamExpression(
            String collection, SolrRequest request, SolrFacetRequest facetRequest) {
        super("facet");

        validateParams(collection, request, facetRequest.getName());
        this.addParameter(new StreamExpressionValue(collection));
        this.addParameter(new StreamExpressionNamedParameter("q", constructQuery(request)));
        this.addParameter(new StreamExpressionNamedParameter("buckets", facetRequest.getName()));
        this.addParameter(
                new StreamExpressionNamedParameter("bucketSorts", getBucketSorts(facetRequest)));
        this.addParameter(
                new StreamExpressionNamedParameter(
                        "bucketSizeLimit", getBucketSizeLimit(facetRequest)));
        StreamExpression expression =
                new StreamExpression(MetricFunctionName.count.name()).withParameter("*");
        this.getParameters().add(expression);

        // order of params is important. this code should be in the end
        if (queryFilteredQuerySet(request)) {
            addFQRelatedParams(request);
        }
    }

    private String getBucketSizeLimit(SolrFacetRequest facetRequest) {
        String bucketSize = DEFAULT_BUCKET_SIZE;
        if (facetRequest.getLimit() > 0 && Utils.nullOrEmpty(facetRequest.getInterval())) {
            bucketSize = String.valueOf(facetRequest.getLimit());
        }
        return bucketSize;
    }

    private String getBucketSorts(SolrFacetRequest facetRequest) {
        String bucketSorts = DEFAULT_BUCKET_SORTS;
        if (facetRequest.getSort() != null) {
            bucketSorts = facetRequest.getSort();
        }
        return bucketSorts;
    }

    private void validateParams(String collection, SolrRequest solrRequest, String buckets) {
        if (Utils.nullOrEmpty(collection)) {
            throw new IllegalArgumentException("collection is a mandatory param");
        }
        if (Utils.nullOrEmpty(solrRequest.getQuery())
                && Utils.nullOrEmpty(solrRequest.getIdsQuery())) {
            throw new IllegalArgumentException("query or Ids is a mandatory param");
        }
        if (Utils.nullOrEmpty(buckets)) {
            throw new IllegalArgumentException("buckets is a mandatory param");
        }
    }
}
