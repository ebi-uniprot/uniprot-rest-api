package org.uniprot.api.rest.service.query.processor;

import java.util.List;
import java.util.Map;

import org.apache.lucene.queryparser.flexible.core.nodes.QueryNode;
import org.apache.lucene.queryparser.flexible.core.parser.EscapeQuerySyntax;
import org.apache.lucene.queryparser.flexible.core.processors.QueryNodeProcessorPipeline;
import org.apache.lucene.queryparser.flexible.standard.config.StandardQueryConfigHandler;
import org.uniprot.store.config.searchfield.model.SearchFieldItem;

/**
 * This class contains query node processors, that act to manipulate the query. If we need to do any
 * client query manipulation, we can add another processor and do it here.
 *
 * <p>The primary use case of this class is to manipulate a client's query into a query tree of
 * {@link QueryNode} instances, which have overridden {@link
 * QueryNode#toQueryString(EscapeQuerySyntax)} methods, which are called when getting the {@link
 * String} representation fo the manipulated query.
 *
 * <p>Created 22/08/2020
 *
 * @author Edd
 */
public class UniProtQueryNodeProcessorPipeline extends QueryNodeProcessorPipeline {
    public UniProtQueryNodeProcessorPipeline(
            List<SearchFieldItem> optimisableFields, Map<String, String> whiteListFields) {
        super(new StandardQueryConfigHandler());

        add(new UniProtFieldQueryNodeProcessor(optimisableFields, whiteListFields));
        add(new UniProtOpenRangeQueryNodeProcessor());
        add(new UniProtPointRangeQueryNodeProcessor());
        add(new UniProtTermRangeQueryNodeProcessor());
    }
}
