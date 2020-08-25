package org.uniprot.api.rest.service.query.processor;

import java.util.List;

import org.apache.lucene.queryparser.flexible.core.processors.QueryNodeProcessorPipeline;
import org.apache.lucene.queryparser.flexible.standard.config.StandardQueryConfigHandler;
import org.uniprot.store.config.searchfield.model.SearchFieldItem;

/**
 * Created 22/08/2020
 *
 * @author Edd
 */
public class UniProtQueryNodeProcessorPipeline extends QueryNodeProcessorPipeline {
    public UniProtQueryNodeProcessorPipeline(List<SearchFieldItem> optimisableFields) {
        super(new StandardQueryConfigHandler());

        add(new UniProtFieldQueryNodeProcessor(optimisableFields));
        add(new UniProtOpenRangeQueryNodeProcessor());
        add(new UniProtPointRangeQueryNodeProcessor());
        add(new UniProtTermRangeQueryNodeProcessor());
    }
}
