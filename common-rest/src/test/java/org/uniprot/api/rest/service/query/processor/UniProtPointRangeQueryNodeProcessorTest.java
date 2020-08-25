package org.uniprot.api.rest.service.query.processor;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import java.text.DecimalFormat;

import org.apache.lucene.queryparser.flexible.core.QueryNodeException;
import org.apache.lucene.queryparser.flexible.core.nodes.QueryNode;
import org.apache.lucene.queryparser.flexible.standard.config.PointsConfig;
import org.apache.lucene.queryparser.flexible.standard.nodes.PointQueryNode;
import org.apache.lucene.queryparser.flexible.standard.nodes.PointRangeQueryNode;
import org.apache.lucene.queryparser.flexible.standard.parser.EscapeQuerySyntaxImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * This specific test class is required, because {@link UniProtPointRangeQueryNodeProcessorTest}
 * does not cover {@link PointRangeQueryNode} queries. At the time of writing, it is not clear how
 * to create such queries via a query String. Therefore, this test manually ensures it behaves
 * correctly.
 *
 * <p>Created 24/08/2020
 *
 * @author Edd
 */
class UniProtPointRangeQueryNodeProcessorTest {

    private UniProtPointRangeQueryNodeProcessor processor;

    @BeforeEach
    void setUp() {
        processor = new UniProtPointRangeQueryNodeProcessor();
    }

    @Test
    void lowerUpperInclusive() throws QueryNodeException {
        String field = "f";
        int from = 1;
        int to = 2;
        PointRangeQueryNode queryNode =
                new PointRangeQueryNode(
                        new PointQueryNode(field, from, DecimalFormat.getInstance()),
                        new PointQueryNode(field, to, DecimalFormat.getInstance()),
                        true,
                        true,
                        new PointsConfig(DecimalFormat.getInstance(), Integer.class));

        QueryNode processedQueryNode = processor.postProcessNode(queryNode);

        String processedQuery =
                processedQueryNode.toQueryString(new EscapeQuerySyntaxImpl()).toString();

        System.out.println(processedQuery);
        assertThat(processedQuery, is(field + ":[" + from + " TO " + to + "]"));
    }

    @Test
    void lowerInclusiveUpperNonInclusive() throws QueryNodeException {
        String field = "f";
        int from = 1;
        int to = 2;
        PointRangeQueryNode queryNode =
                new PointRangeQueryNode(
                        new PointQueryNode(field, from, DecimalFormat.getInstance()),
                        new PointQueryNode(field, to, DecimalFormat.getInstance()),
                        true,
                        false,
                        new PointsConfig(DecimalFormat.getInstance(), Integer.class));

        QueryNode processedQueryNode = processor.postProcessNode(queryNode);

        String processedQuery =
                processedQueryNode.toQueryString(new EscapeQuerySyntaxImpl()).toString();

        System.out.println(processedQuery);
        assertThat(processedQuery, is(field + ":[" + from + " TO " + to + "}"));
    }

    @Test
    void lowerUpperNonInclusive() throws QueryNodeException {
        String field = "f";
        int from = 1;
        int to = 2;
        PointRangeQueryNode queryNode =
                new PointRangeQueryNode(
                        new PointQueryNode(field, from, DecimalFormat.getInstance()),
                        new PointQueryNode(field, to, DecimalFormat.getInstance()),
                        false,
                        false,
                        new PointsConfig(DecimalFormat.getInstance(), Integer.class));

        QueryNode processedQueryNode = processor.postProcessNode(queryNode);

        String processedQuery =
                processedQueryNode.toQueryString(new EscapeQuerySyntaxImpl()).toString();

        System.out.println(processedQuery);
        assertThat(processedQuery, is(field + ":{" + from + " TO " + to + "}"));
    }

    @Test
    void lowerNonInclusiveUpperInclusive() throws QueryNodeException {
        String field = "f";
        int from = 1;
        int to = 2;
        PointRangeQueryNode queryNode =
                new PointRangeQueryNode(
                        new PointQueryNode(field, from, DecimalFormat.getInstance()),
                        new PointQueryNode(field, to, DecimalFormat.getInstance()),
                        false,
                        true,
                        new PointsConfig(DecimalFormat.getInstance(), Integer.class));

        QueryNode processedQueryNode = processor.postProcessNode(queryNode);

        String processedQuery =
                processedQueryNode.toQueryString(new EscapeQuerySyntaxImpl()).toString();

        System.out.println(processedQuery);
        assertThat(processedQuery, is(field + ":{" + from + " TO " + to + "]"));
    }
}
