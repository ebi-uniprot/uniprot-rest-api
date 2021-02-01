package org.uniprot.api.rest.service.query.processor;

import java.util.List;

import org.apache.lucene.queryparser.flexible.core.nodes.*;
import org.apache.lucene.queryparser.flexible.core.processors.QueryNodeProcessorImpl;
import org.uniprot.api.common.repository.search.QueryOperator;
import org.uniprot.api.common.repository.search.SolrRequest;

/**
 * Created 23/08/2020
 *
 * @author Edd
 */
class UniProtDefaultFieldQueryNodeProcessor extends QueryNodeProcessorImpl {

    @Override
    protected QueryNode preProcessNode(QueryNode node) {
        return node;
    }

    @Override
    protected QueryNode postProcessNode(QueryNode node) {
        // BooleanQueryNode represents a default boolean query (with no AND / OR)
        if (node instanceof BooleanQueryNode
                && !(node instanceof AndQueryNode)
                && !(node instanceof OrQueryNode)) {
            if (SolrRequest.DEFAULT_OPERATOR == QueryOperator.AND) {
                // explicitly interpret the default operator as AND
                return new AndQueryNode(node.getChildren());
            } else {
                // explicitly interpret the default operator as OR
                return new OrQueryNode((node.getChildren()));
            }
        }
        return node;
    }

    @Override
    protected List<QueryNode> setChildrenOrder(List<QueryNode> children) {
        return children;
    }
}
