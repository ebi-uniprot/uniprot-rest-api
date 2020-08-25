package org.uniprot.api.rest.service.query;

import lombok.Builder;
import lombok.extern.slf4j.Slf4j;
import org.apache.lucene.queryparser.flexible.core.QueryNodeException;
import org.apache.lucene.queryparser.flexible.core.nodes.QueryNode;
import org.apache.lucene.queryparser.flexible.standard.parser.EscapeQuerySyntaxImpl;
import org.apache.lucene.queryparser.flexible.standard.parser.StandardSyntaxParser;
import org.uniprot.api.rest.service.query.processor.UniProtQueryNodeProcessorPipeline;

/**
 * Created 24/08/2020
 *
 * @author Edd
 */
@Slf4j
@Builder
public class UniProtQueryProcessor implements QueryProcessor {
    public static final String IMPOSSIBLE_FIELD = "NOT_REAL_FIELD";
    private static final EscapeQuerySyntaxImpl ESCAPER = new EscapeQuerySyntaxImpl();
    private final UniProtQueryNodeProcessorPipeline queryProcessorPipeline;

    @Override
    public String processQuery(String query) {
        StandardSyntaxParser syntaxParser = new StandardSyntaxParser();
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
