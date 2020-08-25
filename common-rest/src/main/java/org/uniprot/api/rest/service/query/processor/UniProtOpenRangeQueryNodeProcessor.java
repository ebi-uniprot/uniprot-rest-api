package org.uniprot.api.rest.service.query.processor;

import org.apache.lucene.queryparser.flexible.core.QueryNodeException;
import org.apache.lucene.queryparser.flexible.core.nodes.QueryNode;
import org.apache.lucene.queryparser.flexible.standard.nodes.TermRangeQueryNode;
import org.apache.lucene.queryparser.flexible.standard.processors.OpenRangeQueryNodeProcessor;

/**
 * Created 22/08/2020
 *
 * @author Edd
 */
class UniProtOpenRangeQueryNodeProcessor extends OpenRangeQueryNodeProcessor {
    @Override
    protected QueryNode postProcessNode(QueryNode node) throws QueryNodeException {
        QueryNode queryNode = super.postProcessNode(node);
        if (queryNode instanceof TermRangeQueryNode) {
            return new UniProtTermRangeQueryNodeProcessor.UniProtTermRangeQueryNode(
                    (TermRangeQueryNode) queryNode);
        } else {
            return queryNode;
        }
    }
}
