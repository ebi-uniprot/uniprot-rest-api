package org.uniprot.api.rest.service.query.processor;

import org.apache.lucene.queryparser.flexible.core.QueryNodeException;
import org.apache.lucene.queryparser.flexible.core.nodes.QueryNode;
import org.apache.lucene.queryparser.flexible.core.parser.EscapeQuerySyntax;
import org.apache.lucene.queryparser.flexible.standard.nodes.PointRangeQueryNode;
import org.apache.lucene.queryparser.flexible.standard.processors.PointRangeQueryNodeProcessor;

/**
 * Created 22/08/2020
 *
 * @author Edd
 */
class UniProtPointRangeQueryNodeProcessor extends PointRangeQueryNodeProcessor {
    @Override
    protected QueryNode postProcessNode(QueryNode node) throws QueryNodeException {
        QueryNode queryNode = super.postProcessNode(node);
        if (queryNode instanceof PointRangeQueryNode) {
            return new UniProtPointRangeQueryNode((PointRangeQueryNode) queryNode);
        }
        return queryNode;
    }

    private static class UniProtPointRangeQueryNode extends PointRangeQueryNode {
        public UniProtPointRangeQueryNode(PointRangeQueryNode queryNode) throws QueryNodeException {
            super(
                    queryNode.getLowerBound(),
                    queryNode.getUpperBound(),
                    queryNode.isLowerInclusive(),
                    queryNode.isUpperInclusive(),
                    queryNode.getPointsConfig());
        }

        @Override
        public CharSequence toQueryString(EscapeQuerySyntax escapeSyntaxParser) {
            return RangeToQueryString.toQueryString(this, escapeSyntaxParser);
        }
    }
}
