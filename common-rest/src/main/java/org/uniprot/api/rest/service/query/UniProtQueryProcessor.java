package org.uniprot.api.rest.service.query;

import static org.uniprot.api.rest.validation.ValidSolrQuerySyntax.QuerySyntaxValidator.replaceForwardSlashes;

import lombok.Builder;
import lombok.extern.slf4j.Slf4j;

import org.apache.lucene.queryparser.flexible.core.nodes.QueryNode;
import org.apache.lucene.queryparser.flexible.core.parser.EscapeQuerySyntax;
import org.apache.lucene.queryparser.flexible.standard.parser.EscapeQuerySyntaxImpl;
import org.apache.lucene.queryparser.flexible.standard.parser.StandardSyntaxParser;
import org.uniprot.api.rest.service.query.processor.UniProtQueryNodeProcessorPipeline;
import org.uniprot.api.rest.service.query.processor.UniProtQueryProcessorConfig;

/**
 * This class does the following:
 *
 * <ul>
 *   <li>takes a client query as {@link String} and converts it to a {@link QueryNode} (a query tree
 *       representing the query)
 *   <li>processes it in a {@link UniProtQueryNodeProcessorPipeline}, to get a manipulated {@link
 *       QueryNode} with overridden {@link QueryNode#toQueryString(EscapeQuerySyntax)} methods.
 *   <li>{@link QueryNode#toQueryString(EscapeQuerySyntax)} is then called on the resulting query
 *       tree to give a {@link String} version of the processed client query.
 * </ul>
 *
 * <b>Note:</b> A new instance of this class should be used for every query that is to be processed.
 *
 * <p>Created 24/08/2020
 *
 * @author Edd
 */
@Slf4j
@Builder
public class UniProtQueryProcessor implements QueryProcessor {
    public static final String IMPOSSIBLE_FIELD = "NOT_REAL_FIELD";
    private static final EscapeQuerySyntaxImpl ESCAPER = new EscapeQuerySyntaxImpl();
    private final UniProtQueryNodeProcessorPipeline queryProcessorPipeline;

    public static UniProtQueryProcessor newInstance(UniProtQueryProcessorConfig config) {
        return new UniProtQueryProcessor(new UniProtQueryNodeProcessorPipeline(config));
    }

    public UniProtQueryProcessor(UniProtQueryNodeProcessorPipeline pipeline) {
        queryProcessorPipeline = pipeline;
    }

    @Override
    public String processQuery(String query) {
        try {
            StandardSyntaxParser syntaxParser = new StandardSyntaxParser();

            String queryWithEscapedForwardSlashes = replaceForwardSlashes(query);
            QueryNode queryTree =
                    syntaxParser.parse(queryWithEscapedForwardSlashes, IMPOSSIBLE_FIELD);
            QueryNode processedQueryTree = queryProcessorPipeline.process(queryTree);
            return processedQueryTree.toQueryString(ESCAPER).toString();
        } catch (Exception e) {
            log.warn("Problem processing user query: " + query, e);
            return query;
        }
    }
}
