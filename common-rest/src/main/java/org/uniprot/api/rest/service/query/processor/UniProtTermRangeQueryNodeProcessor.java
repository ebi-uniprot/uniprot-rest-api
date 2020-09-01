package org.uniprot.api.rest.service.query.processor;

import org.apache.lucene.queryparser.flexible.core.QueryNodeException;
import org.apache.lucene.queryparser.flexible.core.nodes.QueryNode;
import org.apache.lucene.queryparser.flexible.core.parser.EscapeQuerySyntax;
import org.apache.lucene.queryparser.flexible.standard.nodes.TermRangeQueryNode;
import org.apache.lucene.queryparser.flexible.standard.processors.TermRangeQueryNodeProcessor;

/**
 * Created 22/08/2020
 *
 * @author Edd
 */
class UniProtTermRangeQueryNodeProcessor extends TermRangeQueryNodeProcessor {
    @Override
    protected QueryNode postProcessNode(QueryNode node) throws QueryNodeException {
        QueryNode queryNode = super.postProcessNode(node);
        if (queryNode instanceof TermRangeQueryNode) {
            return new UniProtTermRangeQueryNode((TermRangeQueryNode) queryNode);
        }
        return queryNode;
    }

    static class UniProtTermRangeQueryNode extends TermRangeQueryNode {
        public UniProtTermRangeQueryNode(TermRangeQueryNode queryNode) {
            super(
                    queryNode.getLowerBound(),
                    queryNode.getUpperBound(),
                    queryNode.isLowerInclusive(),
                    queryNode.isUpperInclusive());
        }

        @Override
        public CharSequence toQueryString(EscapeQuerySyntax escapeSyntaxParser) {
            return RangeToQueryString.toQueryString(this, escapeSyntaxParser);
        }
    }
}
