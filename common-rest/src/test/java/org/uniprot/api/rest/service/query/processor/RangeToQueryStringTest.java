package org.uniprot.api.rest.service.query.processor;

import org.apache.lucene.queryparser.flexible.core.nodes.FieldQueryNode;
import org.apache.lucene.queryparser.flexible.core.parser.EscapeQuerySyntax;
import org.apache.lucene.queryparser.flexible.standard.nodes.TermRangeQueryNode;
import org.apache.lucene.queryparser.flexible.standard.parser.EscapeQuerySyntaxImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Locale;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Created 25/08/2020
 *
 * @author Edd
 */
class RangeToQueryStringTest {

    private EscapeQuerySyntaxImpl escaper;

    @BeforeEach
    void setUp() {
        escaper = new EscapeQuerySyntaxImpl();
    }

    @Test
    void lowerUpperInclusive() {
        String field = "f";
        String textLower = "1";
        String textUpper = "2";
        int fromPositionInString = 123;
        int toPositionInString = 124;
        TermRangeQueryNode queryNode =
                new TermRangeQueryNode(
                        new FieldQueryNode(
                                field, textLower, fromPositionInString, toPositionInString),
                        new FieldQueryNode(
                                field, textUpper, fromPositionInString, toPositionInString),
                        true,
                        true);

        String processedQuery = RangeToQueryString.toQueryString(queryNode, escaper).toString();

        System.out.println(processedQuery);
        assertThat(
                processedQuery,
                is(field + ":[" + escape(textLower) + " TO " + escape(textUpper) + "]"));
    }

    @Test
    void lowerInclusiveUpperNonInclusive() {
        String field = "f";
        String textLower = "1";
        String textUpper = "2";
        int fromPositionInString = 123;
        int toPositionInString = 124;
        TermRangeQueryNode queryNode =
                new TermRangeQueryNode(
                        new FieldQueryNode(
                                field, textLower, fromPositionInString, toPositionInString),
                        new FieldQueryNode(
                                field, textUpper, fromPositionInString, toPositionInString),
                        true,
                        false);

        String processedQuery = RangeToQueryString.toQueryString(queryNode, escaper).toString();

        System.out.println(processedQuery);
        assertThat(
                processedQuery,
                is(field + ":[" + escape(textLower) + " TO " + escape(textUpper) + "}"));
    }

    @Test
    void lowerUpperNonInclusive() {
        String field = "f";
        String textLower = "1";
        String textUpper = "2";
        int fromPositionInString = 123;
        int toPositionInString = 124;
        TermRangeQueryNode queryNode =
                new TermRangeQueryNode(
                        new FieldQueryNode(
                                field, textLower, fromPositionInString, toPositionInString),
                        new FieldQueryNode(
                                field, textUpper, fromPositionInString, toPositionInString),
                        false,
                        false);

        String processedQuery = RangeToQueryString.toQueryString(queryNode, escaper).toString();

        System.out.println(processedQuery);
        assertThat(
                processedQuery,
                is(field + ":{" + escape(textLower) + " TO " + escape(textUpper) + "}"));
    }

    @Test
    void lowerNonInclusiveUpperInclusive() {
        String field = "f";
        String textLower = "1";
        String textUpper = "2";
        int fromPositionInString = 123;
        int toPositionInString = 124;
        TermRangeQueryNode queryNode =
                new TermRangeQueryNode(
                        new FieldQueryNode(
                                field, textLower, fromPositionInString, toPositionInString),
                        new FieldQueryNode(
                                field, textUpper, fromPositionInString, toPositionInString),
                        false,
                        true);

        String processedQuery = RangeToQueryString.toQueryString(queryNode, escaper).toString();

        System.out.println(processedQuery);
        assertThat(
                processedQuery,
                is(field + ":{" + escape(textLower) + " TO " + escape(textUpper) + "]"));
    }

    @Test
    void convertsStar() {
        String field = "f";
        String textLower = "";
        String textUpper = "";
        int fromPositionInString = 123;
        int toPositionInString = 124;
        TermRangeQueryNode queryNode =
                new TermRangeQueryNode(
                        new FieldQueryNode(
                                field, textLower, fromPositionInString, toPositionInString),
                        new FieldQueryNode(
                                field, textUpper, fromPositionInString, toPositionInString),
                        true,
                        true);

        String processedQuery = RangeToQueryString.toQueryString(queryNode, escaper).toString();

        System.out.println(processedQuery);
        assertThat(processedQuery, is(field + ":[* TO *]"));
    }

    @Test
    void convertsDates() {
        String field = "f";
        String textLower = "2013-07-17T00:00:00Z";
        String textUpper = "NOW";
        int fromPositionInString = 123;
        int toPositionInString = 124;
        TermRangeQueryNode queryNode =
                new TermRangeQueryNode(
                        new FieldQueryNode(
                                field, textLower, fromPositionInString, toPositionInString),
                        new FieldQueryNode(
                                field, textUpper, fromPositionInString, toPositionInString),
                        true,
                        true);

        String processedQuery = RangeToQueryString.toQueryString(queryNode, escaper).toString();

        System.out.println(processedQuery);
        assertThat(
                processedQuery,
                is(field + ":[" + escape(textLower) + " TO " + escape(textUpper) + "]"));
    }

    private String escape(String toEscape) {
        return escaper.escape(toEscape, Locale.ENGLISH, EscapeQuerySyntax.Type.NORMAL).toString();
    }
}
