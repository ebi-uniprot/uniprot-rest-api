package org.uniprot.api.common.repository.solrstream;

import java.util.*;

import org.apache.solr.client.solrj.io.stream.expr.StreamExpression;
import org.apache.solr.client.solrj.io.stream.expr.StreamExpressionNamedParameter;
import org.apache.solr.client.solrj.io.stream.expr.StreamExpressionValue;
import org.uniprot.api.common.repository.search.SolrFacetRequest;
import org.uniprot.api.common.repository.search.SolrRequest;
import org.uniprot.api.common.repository.search.facet.FacetItem;
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

    private static String getBucketSizeLimit(SolrFacetRequest facetRequest) {
        String bucketSize = DEFAULT_BUCKET_SIZE;
        if (facetRequest.getLimit() != 0 && Utils.nullOrEmpty(facetRequest.getInterval())) {
            bucketSize = String.valueOf(facetRequest.getLimit());
        }
        return bucketSize;
    }

    private static String getBucketSorts(SolrFacetRequest facetRequest) {
        String bucketSorts = DEFAULT_BUCKET_SORTS;
        if (Utils.notNullNotEmpty(facetRequest.getSort())) {
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

    public static Map<String, Map.Entry<Integer, Comparator<FacetItem>>>
            getFacetNameComparatorAndLimitMap(List<SolrFacetRequest> facetRequests) {
        Map<String, Map.Entry<Integer, Comparator<FacetItem>>> nameComparatorLimitMap =
                new HashMap<>();
        for (SolrFacetRequest request : facetRequests) {
            Integer limit = Integer.parseInt(getBucketSizeLimit(request));
            Comparator<FacetItem> comparator = getFacetItemComparator(request);
            Map.Entry<Integer, Comparator<FacetItem>> limitComparator =
                    new AbstractMap.SimpleEntry<>(limit, comparator);
            nameComparatorLimitMap.put(request.getName(), limitComparator);
        }
        return nameComparatorLimitMap;
    }

    private static Comparator<FacetItem> getFacetItemComparator(SolrFacetRequest request) {
        if ("index asc".equals(getBucketSorts(request))) {
            if ("proteins_with".equals(request.getName())
                    || "database_facet".equals(request.getName())) {
                return Comparator.comparingInt(f -> Integer.parseInt(f.getValue()));
            }
            return Comparator.comparing(FacetItem::getLabel);
        } else if ("index desc".equals(getBucketSorts(request))) {
            return Comparator.comparing(FacetItem::getValue, Comparator.reverseOrder());
        } else if ("count(*) desc".equals(getBucketSorts(request))) {
            return Comparator.comparing(FacetItem::getCount, Comparator.reverseOrder());
        }
        throw new IllegalArgumentException("Illegal sort " + getBucketSorts(request));
    }
}
