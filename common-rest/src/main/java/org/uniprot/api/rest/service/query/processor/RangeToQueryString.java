package org.uniprot.api.rest.service.query.processor;

import org.apache.lucene.queryparser.flexible.core.nodes.FieldValuePairQueryNode;
import org.apache.lucene.queryparser.flexible.core.parser.EscapeQuerySyntax;
import org.apache.lucene.queryparser.flexible.standard.nodes.AbstractRangeQueryNode;
import org.uniprot.core.util.Utils;

/**
 * Used by *RangeQueryNodeProcessor classes to get the query {@link String} representation of their
 * range value.
 *
 * <p>Created 22/08/2020
 *
 * @author Edd
 */
public class RangeToQueryString {
    private RangeToQueryString() {}

    static <T extends FieldValuePairQueryNode<?>> CharSequence toQueryString(
            AbstractRangeQueryNode<T> queryNode, EscapeQuerySyntax escapeSyntaxParser) {
        StringBuilder sb = new StringBuilder();

        T lower = queryNode.getLowerBound();
        T upper = queryNode.getUpperBound();

        String[] lowerParts = getFieldParts(lower.toQueryString(escapeSyntaxParser));
        String[] upperParts = getFieldParts(upper.toQueryString(escapeSyntaxParser));

        sb.append(lowerParts[0]).append(":");

        if (queryNode.isLowerInclusive()) {
            sb.append('[');
        } else {
            sb.append('{');
        }

        sb.append(lowerParts[1]).append(" TO ").append(upperParts[1]);

        if (queryNode.isUpperInclusive()) {
            sb.append(']');

        } else {
            sb.append('}');
        }

        return sb.toString();
    }

    static String[] getFieldParts(CharSequence charSequenceLower) {
        String charSequenceLowerStr = charSequenceLower.toString();
        String[] parts = new String[2];
        String field = charSequenceLowerStr.substring(0, charSequenceLowerStr.indexOf(':'));
        parts[0] = field;
        String value = charSequenceLowerStr.substring(charSequenceLowerStr.indexOf(':') + 1);
        if (Utils.notNullNotEmpty(value)) {
            parts[1] = value;
        } else {
            parts[1] = "*";
        }
        return parts;
    }
}
