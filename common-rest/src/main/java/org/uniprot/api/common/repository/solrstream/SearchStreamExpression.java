package org.uniprot.api.common.repository.solrstream;

import lombok.Getter;

import org.apache.solr.client.solrj.io.stream.expr.StreamExpressionNamedParameter;
import org.apache.solr.client.solrj.io.stream.expr.StreamExpressionValue;
import org.uniprot.core.util.Utils;

/**
 * This class creates expression to make solr streaming search function call.
 *
 * @author sahmad
 */
@Getter
public class SearchStreamExpression extends UniProtStreamExpression {
    public SearchStreamExpression(String collection, SolrStreamFacetRequest request)
            throws IllegalArgumentException {
        super("search");
        validateParams(collection, request);
        this.addParameter(new StreamExpressionValue(collection));
        this.addParameter(new StreamExpressionNamedParameter("q", request.getQuery()));
        this.addParameter(new StreamExpressionNamedParameter("fl", request.getSearchFieldList()));
        this.addParameter(new StreamExpressionNamedParameter("sort", request.getSearchSort()));
        this.addParameter(new StreamExpressionNamedParameter("qt", request.getRequestHandler()));

        if (queryFilteredQuerySet(
                request)) { // order of params is important. this code should be in the end
            addFQRelatedParams(request);
        }
    }

    private void validateParams(String collection, SolrStreamFacetRequest request) {
        if (Utils.nullOrEmpty(collection)) {
            throw new IllegalArgumentException("collection is a mandatory param");
        }
        if (Utils.nullOrEmpty(request.getQuery())) {
            throw new IllegalArgumentException("query is a mandatory param");
        }
        if (Utils.nullOrEmpty(request.getSearchFieldList())) {
            throw new IllegalArgumentException("fl is a mandatory param");
        }
        if (Utils.nullOrEmpty(request.getSearchSort())) {
            throw new IllegalArgumentException("sort is a mandatory param");
        }
        if (Utils.nullOrEmpty(request.getRequestHandler())) {
            throw new IllegalArgumentException("qt is a mandatory param");
        }
    }
}
