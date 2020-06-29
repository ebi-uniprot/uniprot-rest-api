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
public class FacetStreamExpression extends StreamExpression {
    public enum MetricFunctionName {
        avg,
        count,
        max,
        min,
        sum;
    }

    private FacetStreamExpression(
            String collection,
            String query,
            String buckets,
            String metrics,
            String bucketSorts,
            int bucketSizeLimit)
            throws IllegalArgumentException {
        super("facet");
        validateParams(collection, query, buckets, metrics, bucketSorts, bucketSizeLimit);
        this.addParameter(new StreamExpressionValue(collection));
        this.addParameter(new StreamExpressionNamedParameter("q", query));
        this.addParameter(new StreamExpressionNamedParameter("buckets", buckets));
        this.addParameter(new StreamExpressionNamedParameter("bucketSorts", bucketSorts));
        this.addParameter(
                new StreamExpressionNamedParameter(
                        "bucketSizeLimit", String.valueOf(bucketSizeLimit)));
        List<StreamExpression> metricExpressions = parseMetrics(metrics);
        this.getParameters().addAll(metricExpressions);
    }

    public static class FacetStreamExpressionBuilder {
        private String collection;
        private String query;
        private String buckets;
        private String metrics;
        private String bucketSorts;
        private int bucketSizeLimit;

        public FacetStreamExpressionBuilder() {}

        public FacetStreamExpressionBuilder(
                String collection, String facet, SolrStreamFacetRequest request) {
            this.collection = collection;
            this.query = request.getQuery();
            this.buckets = facet;
            this.metrics = request.getMetrics();
            this.bucketSorts = request.getBucketSorts();
            this.bucketSizeLimit = request.getBucketSizeLimit();
        }

        public FacetStreamExpressionBuilder collection(String collection) {
            this.collection = collection;
            return this;
        }

        public FacetStreamExpressionBuilder query(String query) {
            this.query = query;
            return this;
        }

        public FacetStreamExpressionBuilder buckets(String buckets) {
            this.buckets = buckets;
            return this;
        }

        public FacetStreamExpressionBuilder metrics(String metrics) {
            this.metrics = metrics;
            return this;
        }

        public FacetStreamExpressionBuilder bucketSorts(String bucketSorts) {
            this.bucketSorts = bucketSorts;
            return this;
        }

        public FacetStreamExpressionBuilder bucketSizeLimit(int bucketSizeLimit) {
            this.bucketSizeLimit = bucketSizeLimit;
            return this;
        }

        public FacetStreamExpression build() {
            return new FacetStreamExpression(
                    this.collection,
                    this.query,
                    this.buckets,
                    this.metrics,
                    this.bucketSorts,
                    this.bucketSizeLimit);
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
