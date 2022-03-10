package org.uniprot.api.common.repository.solrstream;

import org.apache.solr.client.solrj.io.stream.expr.StreamExpression;
import org.apache.solr.client.solrj.io.stream.expr.StreamExpressionNamedParameter;
import org.uniprot.api.common.repository.search.QueryOperator;
import org.uniprot.core.util.Utils;

/**
 * @author sahmad
 * @created 25/03/2021
 */
public class UniProtStreamExpression extends StreamExpression {
    public UniProtStreamExpression(String functionName) {
        super(functionName);
    }

    protected void addFQRelatedParams(SolrStreamFacetRequest request) {
        this.addParameter(new StreamExpressionNamedParameter("defType", "edismax"));
        this.addParameter(
                new StreamExpressionNamedParameter(
                        "qf", request.getQueryConfig().getQueryFields()));
        this.addParameter(new StreamExpressionNamedParameter("fq", request.getFilteredQuery()));
        this.addParameter(new StreamExpressionNamedParameter("q.op", QueryOperator.AND.name()));
    }

    protected boolean queryFilteredQuerySet(SolrStreamFacetRequest request) {
        return Utils.notNullNotEmpty(request.getFilteredQuery())
                && Utils.notNullNotEmpty(request.getQuery());
    }
}
