package org.uniprot.api.common.repository.solrstream;

import java.util.List;

import org.apache.solr.client.solrj.io.stream.expr.StreamExpression;

public class ListStreamExpression extends StreamExpression {
    public ListStreamExpression(List<StreamExpression> streamExpressions) {
        super("list");
        this.getParameters().addAll(streamExpressions);
    }
}
