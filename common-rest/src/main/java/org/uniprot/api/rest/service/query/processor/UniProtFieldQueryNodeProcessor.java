package org.uniprot.api.rest.service.query.processor;

import static org.uniprot.api.rest.service.query.UniProtQueryProcessor.IMPOSSIBLE_FIELD;
import static org.uniprot.core.util.Utils.notNullNotEmpty;

import java.util.List;
import java.util.Optional;

import org.apache.lucene.queryparser.flexible.core.nodes.FieldQueryNode;
import org.apache.lucene.queryparser.flexible.core.nodes.QueryNode;
import org.apache.lucene.queryparser.flexible.core.parser.EscapeQuerySyntax;
import org.apache.lucene.queryparser.flexible.core.processors.QueryNodeProcessorImpl;
import org.uniprot.store.config.searchfield.model.SearchFieldItem;

/**
 * Created 23/08/2020
 *
 * @author Edd
 */
class UniProtFieldQueryNodeProcessor extends QueryNodeProcessorImpl {
    private final List<SearchFieldItem> optimisableFields;

    UniProtFieldQueryNodeProcessor(List<SearchFieldItem> optimisableFields) {
        this.optimisableFields = optimisableFields;
    }

    @Override
    protected QueryNode preProcessNode(QueryNode node) {
        return node;
    }

    @Override
    protected QueryNode postProcessNode(QueryNode node) {
        if (node instanceof FieldQueryNode) {
            return new UniProtFieldQueryNode((FieldQueryNode) node, optimisableFields);
        }
        return node;
    }

    @Override
    protected List<QueryNode> setChildrenOrder(List<QueryNode> children) {
        return children;
    }

    private static class UniProtFieldQueryNode extends FieldQueryNode {
        private final List<SearchFieldItem> optimisableFields;

        public UniProtFieldQueryNode(FieldQueryNode node, List<SearchFieldItem> optimisableFields) {
            super(node.getField(), node.getText(), node.getBegin(), node.getEnd());
            this.optimisableFields = optimisableFields;
        }

        @Override
        public CharSequence toQueryString(EscapeQuerySyntax escaper) {
            String field = getField().toString();
            String text = getTextAsString();

            if (field.equals(IMPOSSIBLE_FIELD)) {
                Optional<SearchFieldItem> optionalSearchField =
                        optimisableFields.stream()
                                .filter(
                                        f ->
                                                notNullNotEmpty(f.getValidRegex())
                                                        && text.matches(f.getValidRegex()))
                                .findFirst();

                return optionalSearchField.map(f -> f.getFieldName() + ":" + text).orElse(text);
            } else {
                return super.toQueryString(escaper);
            }
        }
    }
}
