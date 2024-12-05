package org.uniprot.api.common.repository.solrstream;

import org.apache.solr.client.solrj.io.stream.expr.StreamExpressionNamedParameter;
import org.apache.solr.client.solrj.io.stream.expr.StreamExpressionValue;
import org.uniprot.api.common.repository.search.SolrRequest;
import org.uniprot.core.util.Utils;

import lombok.Getter;

/**
 * This class creates expression to make solr streaming search function call.
 *
 * @author sahmad
 */
@Getter
public class SearchStreamExpression extends UniProtStreamExpression {

    private static final String REQUEST_HANDLER = "/export";

    public SearchStreamExpression(String collection, SolrRequest request)
            throws IllegalArgumentException {
        super("search");
        validateParams(collection, request);
        this.addParameter(new StreamExpressionValue(collection));
        this.addParameter(new StreamExpressionNamedParameter("q", constructQuery(request)));
        this.addParameter(new StreamExpressionNamedParameter("fl", constructFieldList(request)));
        this.addParameter(new StreamExpressionNamedParameter("sort", constructSortQuery(request)));
        this.addParameter(new StreamExpressionNamedParameter("qt", REQUEST_HANDLER));

        // order of params is important. this code should be in the end
        if (queryFilteredQuerySet(request)) {
            addFQRelatedParams(request);
        }
    }

    private void validateParams(String collection, SolrRequest request) {
        if (Utils.nullOrEmpty(collection)) {
            throw new IllegalArgumentException("collection is a mandatory param");
        }
        if (Utils.nullOrEmpty(request.getQuery()) && Utils.nullOrEmpty(request.getIdsQuery())) {
            throw new IllegalArgumentException("query or ids is a mandatory param");
        }
        if (Utils.nullOrEmpty(request.getQueryField()) && Utils.nullOrEmpty(request.getIdField())) {
            throw new IllegalArgumentException("fl is a mandatory param");
        }
        if (Utils.nullOrEmpty(request.getSorts()) && Utils.nullOrEmpty(request.getIdField())) {
            throw new IllegalArgumentException("sort is a mandatory param");
        }
    }
}
