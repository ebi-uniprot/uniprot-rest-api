package org.uniprot.api.rest.service.query.processor;

import static org.uniprot.api.rest.service.query.UniProtQueryProcessor.IMPOSSIBLE_FIELD;
import static org.uniprot.api.rest.service.query.UniProtQueryProcessor.UNIPROTKB_ACCESSION_FIELD;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.apache.lucene.queryparser.flexible.core.nodes.FieldQueryNode;
import org.apache.lucene.queryparser.flexible.core.nodes.FuzzyQueryNode;
import org.apache.lucene.queryparser.flexible.core.nodes.QueryNode;
import org.apache.lucene.queryparser.flexible.core.nodes.QuotedFieldQueryNode;
import org.apache.lucene.queryparser.flexible.core.parser.EscapeQuerySyntax;
import org.apache.lucene.queryparser.flexible.core.processors.QueryNodeProcessorImpl;
import org.uniprot.store.config.searchfield.common.SearchFieldConfig;
import org.uniprot.store.config.searchfield.model.SearchFieldItem;
import org.uniprot.store.search.SolrQueryUtil;

/**
 * Created 23/08/2020
 *
 * @author Edd
 */
class UniProtFieldQueryNodeProcessor extends QueryNodeProcessorImpl {
    public static final String SOLR_FIELD_SEPARATOR = ":";
    private final UniProtQueryProcessorConfig conf;

    UniProtFieldQueryNodeProcessor(UniProtQueryProcessorConfig conf) {
        this.conf = conf;
    }

    @Override
    protected QueryNode preProcessNode(QueryNode node) {
        return node;
    }

    @Override
    protected QueryNode postProcessNode(QueryNode node) {
        // do not delegate to UniProtFieldQueryNode unless we want
        if (node instanceof FieldQueryNode) {
            // handle all subtypes of FieldQueryNode
            if (node instanceof QuotedFieldQueryNode) {
                CharSequence field = ((QuotedFieldQueryNode) node).getField();
                if (field.equals(IMPOSSIBLE_FIELD)) {
                    ((QuotedFieldQueryNode) node).setField(null);
                }
            } else if (node instanceof FuzzyQueryNode) {
                CharSequence field = ((FuzzyQueryNode) node).getField();
                if (field.equals(IMPOSSIBLE_FIELD)) {
                    ((FuzzyQueryNode) node).setField(null);
                }
            } else {
                return new UniProtFieldQueryNode((FieldQueryNode) node, conf);
            }
        }
        return node;
    }

    @Override
    protected List<QueryNode> setChildrenOrder(List<QueryNode> children) {
        return children;
    }

    private static class UniProtFieldQueryNode extends FieldQueryNode {
        private final Map<String, String> whiteListFields;
        private final Set<String> searchFields;
        private final Set<String> leadingWildcardFields;
        private final SearchFieldConfig searchFieldConfig;

        public UniProtFieldQueryNode(FieldQueryNode node, UniProtQueryProcessorConfig conf) {
            super(node.getField(), node.getText(), node.getBegin(), node.getEnd());
            this.whiteListFields = conf.getWhiteListFields();
            this.leadingWildcardFields = conf.getLeadingWildcardFields();
            this.searchFieldConfig = conf.getSearchFieldConfig();
            this.searchFields = conf.getSearchFieldConfig().getSearchFieldNames();
        }

        @Override
        public CharSequence toQueryString(EscapeQuerySyntax escaper) {
            String field = getField().toString();
            String text = stripLeadingWildcardIfNeeded(field, getTextAsString());

            if (field.equals(IMPOSSIBLE_FIELD)) {
                return defaultSearchToQueryString(text);
            }
            if (validWhiteListFields(field, text)) {
                return field.toUpperCase() + "\\:" + text;
            }

            if (UNIPROTKB_ACCESSION_FIELD.equals(field)) {
                text = text.toUpperCase();
            }
            if (validFieldIgnoreCase(field)) {
                return field.toLowerCase() + SOLR_FIELD_SEPARATOR + text;
            }
            Optional<SearchFieldItem> searchFieldItem =
                    searchFieldConfig.findSearchFieldItemByAlias(field);
            if (searchFieldItem.isPresent()) {
                return searchFieldItem.get().getFieldName() + SOLR_FIELD_SEPARATOR + text;
            }

            return super.toQueryString(escaper);
        }

        private String stripLeadingWildcardIfNeeded(String field, String text) {
            while (SolrQueryUtil.ignoreLeadingWildcard(field, text, this.leadingWildcardFields)) {
                text = text.substring(1);
                this.text = text;
            }
            return text;
        }

        private boolean validWhiteListFields(String field, String text) {
            boolean result = false;
            if (whiteListFields.containsKey(field.toLowerCase())) {
                String validRegex = whiteListFields.get(field.toLowerCase());
                result = text.matches(validRegex);
            }
            return result;
        }

        private boolean validFieldIgnoreCase(String field) {
            String fieldNameLowerCase = field.toLowerCase();
            return searchFields.stream().anyMatch(fieldNameLowerCase::equals);
        }

        private String defaultSearchToQueryString(String text) {
            String[] splittedText = text.strip().split("_");
            if (splittedText.length == 2
                    && splittedText[0].matches("[a-zA-Z]+")
                    && splittedText[1].matches("[0-9]+")) {
                text = "\"" + text + "\"";
            }
            return text;
        }
    }
}
