package org.uniprot.api.common.repository.search.request;

import org.apache.lucene.queryparser.flexible.core.nodes.FieldQueryNode;
import org.apache.lucene.queryparser.flexible.core.nodes.QueryNode;
import org.apache.lucene.queryparser.flexible.standard.config.StandardQueryConfigHandler;
import org.uniprot.api.common.repository.search.SolrRequestConverter;
import org.uniprot.api.rest.service.query.UniProtQueryProcessor;
import org.uniprot.api.rest.service.query.processor.UniProtDefaultFieldQueryNodeProcessor;

import java.util.HashSet;
import java.util.Set;

import static org.uniprot.api.rest.service.query.UniProtQueryProcessor.IMPOSSIBLE_FIELD;

/**
 * The purpose of this class is to extract default terms from an arbitrary query. Currently, this
 * class is used to aid the creation of boost queries (where specific boosts are applied to default
 * search terms) in {@link SolrRequestConverter}.
 *
 * <p>Created 14/04/2022
 *
 * @author Edd
 */
public class DefaultTermExtractor {
    public static Set<String> extractDefaultTerms(String query) {
        Set<String> defaultTerms = new HashSet<>();

        UniProtQueryProcessor processor =
                UniProtQueryProcessor.newInstance(
                        new DefaultTermQueryNodeProcessorPipeline(defaultTerms));
        processor.processQuery(query);

        return defaultTerms;
    }

    private static class DefaultTermQueryNodeProcessorPipeline
            extends org.apache.lucene.queryparser.flexible.core.processors
                    .QueryNodeProcessorPipeline {
        public DefaultTermQueryNodeProcessorPipeline(Set<String> defaultTerms) {
            super(new StandardQueryConfigHandler());
            add(new DefaultTermQueryHandler(defaultTerms));
        }
    }

    private static class DefaultTermQueryHandler extends UniProtDefaultFieldQueryNodeProcessor {
        private final Set<String> defaultTerms;

        DefaultTermQueryHandler(Set<String> defaultTerms) {
            this.defaultTerms = defaultTerms;
        }

        @Override
        protected QueryNode postProcessNode(QueryNode node) {
            if (node instanceof FieldQueryNode
                    && ((FieldQueryNode) node).getField().equals(IMPOSSIBLE_FIELD)) {
                String defaultQueryTerm = ((FieldQueryNode) node).getText().toString();
                defaultTerms.add(defaultQueryTerm);
            }
            return super.postProcessNode(node);
        }
    }
}
