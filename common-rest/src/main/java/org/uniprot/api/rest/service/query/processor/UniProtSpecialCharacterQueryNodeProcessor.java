package org.uniprot.api.rest.service.query.processor;

import org.apache.lucene.queryparser.flexible.core.QueryNodeException;
import org.apache.lucene.queryparser.flexible.core.config.QueryConfigHandler;
import org.apache.lucene.queryparser.flexible.core.nodes.QueryNode;
import org.apache.lucene.queryparser.flexible.core.processors.QueryNodeProcessor;
import org.apache.lucene.queryparser.flexible.core.processors.QueryNodeProcessorImpl;

import java.util.List;

public class UniProtSpecialCharacterQueryNodeProcessor extends QueryNodeProcessorImpl {
    @Override
    protected QueryNode preProcessNode(QueryNode node) throws QueryNodeException {
        return null;
    }

    @Override
    protected QueryNode postProcessNode(QueryNode node) throws QueryNodeException {
        return null;
    }

    @Override
    protected List<QueryNode> setChildrenOrder(List<QueryNode> children) throws QueryNodeException {
        return null;
    }
}
