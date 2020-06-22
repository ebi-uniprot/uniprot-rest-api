package org.uniprot.api.common.repository.solrstream;

import java.util.ArrayList;
import java.util.List;

import org.apache.solr.client.solrj.io.stream.expr.StreamExpression;
import org.apache.solr.client.solrj.io.stream.expr.StreamExpressionNamedParameter;
import org.apache.solr.client.solrj.io.stream.expr.StreamExpressionValue;

// TODO add builder pattern
public class FacetStreamExpression extends StreamExpression {

    public FacetStreamExpression(
            String collection,
            String query,
            String buckets,
            String metrics,
            String bucketSorts,
            int bucketSizeLimit)
            throws IllegalArgumentException {
        super("facet");
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

    private List<StreamExpression> parseMetrics(String metrics) {
        String[] metricTokens = metrics.split(",");
        List<StreamExpression> expressions = new ArrayList<>();
        for (String token : metricTokens) {
            String functionName = getFunctionName(token);
            String columnName = token.substring(token.indexOf("(") + 1, token.indexOf(")"));
            ;
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
            throw new IllegalArgumentException("Unknown function name " + token);
        }
    }

    private enum MetricFunctionName {
        avg,
        count,
        max,
        min,
        sum;
    }
}
