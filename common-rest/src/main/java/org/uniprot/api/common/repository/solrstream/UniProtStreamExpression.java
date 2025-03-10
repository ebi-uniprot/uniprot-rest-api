package org.uniprot.api.common.repository.solrstream;

import java.util.Set;
import java.util.stream.Collectors;

import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.io.stream.expr.StreamExpression;
import org.apache.solr.client.solrj.io.stream.expr.StreamExpressionNamedParameter;
import org.uniprot.api.common.repository.search.QueryOperator;
import org.uniprot.api.common.repository.search.SolrRequest;
import org.uniprot.core.util.Utils;

/**
 * @author sahmad
 * @created 25/03/2021
 */
public class UniProtStreamExpression extends StreamExpression {
    public UniProtStreamExpression(String functionName) {
        super(functionName);
    }

    protected void addFQRelatedParams(SolrRequest request) {
        // qf if there is a user query else no need for qf, we use fq for better performance
        if (shouldUseQueryFields(request)) {
            this.addParameter(new StreamExpressionNamedParameter("defType", "edismax"));
            this.addParameter(new StreamExpressionNamedParameter("qf", request.getQueryField()));
            this.addParameter(new StreamExpressionNamedParameter("q.op", QueryOperator.AND.name()));
        }
        this.addParameter(
                new StreamExpressionNamedParameter("fq", String.join(",", request.getIdsQuery())));
    }

    private static boolean shouldUseQueryFields(SolrRequest request) {
        return Utils.notNullNotEmpty(request.getQuery()) && !"*:*".equals(request.getQuery());
    }

    protected boolean queryFilteredQuerySet(SolrRequest request) {
        return Utils.notNullNotEmpty(request.getIdsQuery());
    }

    protected String constructQuery(SolrRequest request) {
        String query = request.getQuery();
        if (Utils.nullOrEmpty(query)) {
            query = "*:*";
        }
        return query;
    }

    protected String constructSortQuery(SolrRequest request) {
        if (Utils.nullOrEmpty(request.getSorts())) {
            return request.getIdField() + " " + SolrQuery.ORDER.asc;
        } else {
            return request.getSorts().stream()
                    .map(clause -> clause.getItem() + " " + clause.getOrder().name())
                    .collect(Collectors.joining(","));
        }
    }

    protected String constructFieldList(SolrRequest request) {
        Set<String> fieldList =
                request.getSorts().stream()
                        .map(SolrQuery.SortClause::getItem)
                        .collect(Collectors.toSet());
        fieldList.add(request.getIdField());
        return String.join(",", fieldList);
    }
}
