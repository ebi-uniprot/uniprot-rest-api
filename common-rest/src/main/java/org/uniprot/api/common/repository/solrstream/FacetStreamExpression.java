package org.uniprot.api.common.repository.solrstream;

import java.util.ArrayList;
import java.util.List;

import lombok.Getter;

import org.apache.solr.client.solrj.io.stream.expr.StreamExpression;
import org.apache.solr.client.solrj.io.stream.expr.StreamExpressionNamedParameter;
import org.apache.solr.client.solrj.io.stream.expr.StreamExpressionValue;
import org.uniprot.core.util.Utils;

/**
 * This class creates expression to make solr streaming facet function call.
 *
 * @author sahmad
 */
@Getter
public class FacetStreamExpression extends UniProtStreamExpression {
    public enum MetricFunctionName {
        avg,
        count,
        max,
        min,
        sum;
    }

    public FacetStreamExpression(String collection, String facet, SolrStreamFacetRequest request)
            throws IllegalArgumentException {
        super("facet");
        validateParams(
                collection,
                request.getQuery(),
                facet,
                request.getMetrics(),
                request.getBucketSorts(),
                request.getBucketSizeLimit());

        this.addParameter(new StreamExpressionValue(collection));
        this.addParameter(new StreamExpressionNamedParameter("q", request.getQuery()));
        this.addParameter(new StreamExpressionNamedParameter("buckets", facet));
        this.addParameter(
                new StreamExpressionNamedParameter("bucketSorts", request.getBucketSorts()));
        this.addParameter(
                new StreamExpressionNamedParameter(
                        "bucketSizeLimit", String.valueOf(request.getBucketSizeLimit())));
        List<StreamExpression> metricExpressions = parseMetrics(request.getMetrics());
        this.getParameters().addAll(metricExpressions);

        if (queryFilteredQuerySet(
                request)) { // order of params is important. this code should be in the end
            addFQRelatedParams(request);
        }
    }

    private void validateParams(
            String collection,
            String query,
            String buckets,
            String metrics,
            String bucketSorts,
            int bucketSizeLimit) {
        if (Utils.nullOrEmpty(collection)) {
            throw new IllegalArgumentException("collection is a mandatory param");
        }
        if (Utils.nullOrEmpty(query)) {
            throw new IllegalArgumentException("query is a mandatory param");
        }
        if (Utils.nullOrEmpty(buckets)) {
            throw new IllegalArgumentException("buckets is a mandatory param");
        }
        if (Utils.nullOrEmpty(metrics)) {
            throw new IllegalArgumentException("metrics is a mandatory param");
        }
        if (Utils.nullOrEmpty(bucketSorts)) {
            throw new IllegalArgumentException("bucketSorts is a mandatory param");
        }
        if (bucketSizeLimit <= 0) {
            throw new IllegalArgumentException("bucketSizeLimit should be a positive integer");
        }
    }

    private List<StreamExpression> parseMetrics(String metrics) {
        String[] metricTokens = metrics.split(",");
        List<StreamExpression> expressions = new ArrayList<>();
        for (String token : metricTokens) {
            String functionName = getFunctionName(token);
            String columnName = token.substring(token.indexOf("(") + 1, token.indexOf(")"));
            StreamExpression expression =
                    new StreamExpression(functionName).withParameter(columnName);
            expressions.add(expression);
        }
        return expressions;
    }

    private String getFunctionName(String token) {
        if (token.toLowerCase().startsWith(MetricFunctionName.count.name())) {
            return MetricFunctionName.count.name();
        } else if (token.toLowerCase().startsWith(MetricFunctionName.avg.name())) {
            return MetricFunctionName.avg.name();
        } else if (token.toLowerCase().startsWith(MetricFunctionName.max.name())) {
            return MetricFunctionName.max.name();
        } else if (token.toLowerCase().startsWith(MetricFunctionName.min.name())) {
            return MetricFunctionName.min.name();
        } else if (token.toLowerCase().startsWith(MetricFunctionName.sum.name())) {
            return MetricFunctionName.sum.name();
        } else {
            throw new IllegalArgumentException("Unknown function " + token);
        }
    }
}
