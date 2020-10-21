package org.uniprot.api.rest.service.query.processor;

import org.apache.lucene.queryparser.flexible.core.nodes.*;
import org.apache.lucene.queryparser.flexible.core.processors.QueryNodeProcessorImpl;
import org.uniprot.api.common.repository.search.QueryOperator;
import org.uniprot.api.common.repository.search.SolrRequest;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.uniprot.api.rest.service.query.UniProtQueryProcessor.IMPOSSIBLE_FIELD;

/**
 * Created 23/08/2020
 *
 * @author Edd
 */
class UniProtDefaultFieldQueryNodeProcessor extends QueryNodeProcessorImpl {
    private final Set<String> stopWords;

    UniProtDefaultFieldQueryNodeProcessor(Set<String> stopWords) {
        HashSet<String> realStopWords = new HashSet<>();
        realStopWords.add("and");
        realStopWords.add("or");
        realStopWords.addAll(stopWords);
        this.stopWords = realStopWords;
    }

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

    private AndQueryNode conjuctionWithoutStopTerms(QueryNode node1) {
        List<QueryNode> children = node1.getChildren();
        List<QueryNode> childrenWithoutStopTerms = new ArrayList<>();
        for (QueryNode node : children) {
            if (node instanceof FieldQueryNode) {
                // handle all subtypes of FieldQueryNode
                if (!(node instanceof QuotedFieldQueryNode) && !(node instanceof FuzzyQueryNode)) {
                    FieldQueryNode fieldQueryNode = (FieldQueryNode) node;
                    if (isNotAStopWord(fieldQueryNode)) {
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

    private boolean isNotAStopWord(FieldQueryNode fieldQueryNode) {
        if (stopWords.isEmpty()) {
            return true;
        } else {
            return !(fieldQueryNode.getField().equals(IMPOSSIBLE_FIELD)
                    && stopWords.contains(fieldQueryNode.getText().toString().toLowerCase()));
        }
    }

    @Override
    protected List<QueryNode> setChildrenOrder(List<QueryNode> children) {
        return children;
    }
}
