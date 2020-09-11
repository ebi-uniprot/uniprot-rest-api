package org.uniprot.api.rest.service.query.processor;

import org.apache.lucene.queryparser.flexible.core.nodes.*;
import org.apache.lucene.queryparser.flexible.core.processors.QueryNodeProcessorImpl;
import org.uniprot.api.common.repository.search.QueryOperator;
import org.uniprot.api.common.repository.search.SolrRequest;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import static org.uniprot.api.rest.service.query.UniProtQueryProcessor.IMPOSSIBLE_FIELD;

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
                return conjuctionWithoutStopTerms(node);
            } else {
                // explicitly interpret the default operator as OR
                return new OrQueryNode((node.getChildren()));
            }
        }
        return node;
    }

    static Set<String> stopWords = new HashSet<>();

    static {
        Stream.of(
                        "a", "an", "and", "are", "as", "at", "be", "but", "by", "for", "if", "in",
                        "into", "is", "it", "no", "not", "of", "on", "or", "such", "that", "the",
                        "their", "then", "there", "these", "they", "this", "to", "was", "will",
                        "with", "which")
                .forEach(stopWords::add);
    }

    private AndQueryNode conjuctionWithoutStopTerms(QueryNode node1) {
        List<QueryNode> children = node1.getChildren();
        List<QueryNode> childrenWithoutStopTerms = new ArrayList<>();
        for (QueryNode node : children) {
            if (node instanceof FieldQueryNode) {
                // handle all subtypes of FieldQueryNode
                if (!(node instanceof QuotedFieldQueryNode) && !(node instanceof FuzzyQueryNode)) {
                    FieldQueryNode fieldQueryNode = (FieldQueryNode) node;
                    if (fieldQueryNode.getField().equals(IMPOSSIBLE_FIELD)
                            && !stopWords.contains(fieldQueryNode.getText().toString().toLowerCase())) {
                        childrenWithoutStopTerms.add(node);
                    }
                } else {
                    childrenWithoutStopTerms.add(node);
                }
            } else {
                childrenWithoutStopTerms.add(node);
            }
        }

        return new AndQueryNode(childrenWithoutStopTerms);
    }

    @Override
    protected List<QueryNode> setChildrenOrder(List<QueryNode> children) {
        return children;
    }
}
