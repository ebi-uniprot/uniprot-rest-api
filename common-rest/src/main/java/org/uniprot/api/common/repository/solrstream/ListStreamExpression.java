package org.uniprot.api.common.repository.solrstream;

import java.util.List;

import org.apache.solr.client.solrj.io.stream.expr.StreamExpression;

/**
 * This is a wrapper to prepare a list expression with N stream expressions
 * to call list function(solr streaming decorator function)
 * @author sahmad
 */
public class ListStreamExpression extends StreamExpression {
    public ListStreamExpression(List<StreamExpression> streamExpressions) {
        super("list");
        this.getParameters().addAll(streamExpressions);
    }
}
