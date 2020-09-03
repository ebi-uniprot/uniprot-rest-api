package org.uniprot.api.rest.service.query;

import java.util.List;

import lombok.extern.slf4j.Slf4j;

import org.apache.lucene.queryparser.flexible.core.QueryNodeException;
import org.apache.lucene.queryparser.flexible.core.nodes.QueryNode;
import org.apache.lucene.queryparser.flexible.core.parser.EscapeQuerySyntax;
import org.apache.lucene.queryparser.flexible.standard.parser.EscapeQuerySyntaxImpl;
import org.apache.lucene.queryparser.flexible.standard.parser.StandardSyntaxParser;
import org.uniprot.api.rest.service.query.processor.UniProtQueryNodeProcessorPipeline;
import org.uniprot.store.config.searchfield.model.SearchFieldItem;

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
 * <p>Created 24/08/2020
 *
 * @author Edd
 */
@Slf4j
public class UniProtQueryProcessor implements QueryProcessor {
    public static final String IMPOSSIBLE_FIELD = "NOT_REAL_FIELD";
    private static final EscapeQuerySyntaxImpl ESCAPER = new EscapeQuerySyntaxImpl();
    private final UniProtQueryNodeProcessorPipeline queryProcessorPipeline;
    private final StandardSyntaxParser syntaxParser;

    public UniProtQueryProcessor(List<SearchFieldItem> optimisableFields) {
        this(new UniProtQueryNodeProcessorPipeline(optimisableFields));
    }

    public UniProtQueryProcessor(UniProtQueryNodeProcessorPipeline pipeline) {
        syntaxParser = new StandardSyntaxParser();
        queryProcessorPipeline = pipeline;
    }

    @Override
    public String processQuery(String query) {
        try {
            QueryNode queryTree = syntaxParser.parse(query, IMPOSSIBLE_FIELD);
            QueryNode process = queryProcessorPipeline.process(queryTree);
            return process.toQueryString(ESCAPER).toString();
        } catch (QueryNodeException e) {
            log.warn("Problem processing user query: " + query, e);
            return query;
        }
    }
}
