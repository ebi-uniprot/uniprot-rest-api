package org.uniprot.api.support.data.configure.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.lucene.index.Term;
import org.apache.lucene.search.*;
import org.uniprot.api.support.data.configure.response.SolrJsonQuery;

/**
 * This class convert Solr {@link Query} to {@link SolrJsonQuery}.
 *
 * @author lgonzales
 */
public class SolrQueryConverter {

    private SolrQueryConverter() {}

    public static SolrJsonQuery convert(Query inputQuery) {
        SolrJsonQuery result;
        if (inputQuery instanceof TermQuery) {
            result = buildTermQuery((TermQuery) inputQuery);
        } else if (inputQuery instanceof MatchAllDocsQuery) {
            result = buildMatchAllDocsQuery();
        } else if (inputQuery instanceof WildcardQuery) {
            result = buildWildcardQuery((WildcardQuery) inputQuery);
        } else if (inputQuery instanceof PrefixQuery) {
            result = buildPrefixQuery((PrefixQuery) inputQuery);
        } else if (inputQuery instanceof TermRangeQuery) {
            result = buildTermRangeQuery((TermRangeQuery) inputQuery);
        } else if (inputQuery instanceof PhraseQuery) {
            result = buildPhraseQuery((PhraseQuery) inputQuery);
        } else if (inputQuery instanceof BooleanQuery) {
            result = buildBooleanQuery((BooleanQuery) inputQuery);
        } else {
            throw new UnsupportedOperationException(
                    "Unsupported query type" + inputQuery.getClass().getName());
        }
        return result;
    }

    private static SolrJsonQuery buildBooleanQuery(BooleanQuery booleanQuery) {
        List<SolrJsonQuery> booleanQueries = new ArrayList<>();
        for (BooleanClause clause : booleanQuery.clauses()) {
            SolrJsonQuery clauseQuery = convert(clause.getQuery());
            clauseQuery.setQueryOperator(getQueryOperator(clause));
            booleanQueries.add(clauseQuery);
        }
        return SolrJsonQuery.builder().type("booleanQuery").booleanQuery(booleanQueries).build();
    }

    private static String getQueryOperator(BooleanClause clause) {
        String result = "";
        switch (clause.getOccur()) {
            case MUST:
                result = "AND";
                break;
            case SHOULD:
                result = "OR";
                break;
            case MUST_NOT:
                result = "NOT";
                break;
        }
        return result;
    }

    private static SolrJsonQuery buildPhraseQuery(PhraseQuery phraseQuery) {
        String value =
                Arrays.stream(phraseQuery.getTerms())
                        .map(Term::text)
                        .collect(Collectors.joining(" "));
        String fieldName = phraseQuery.getTerms()[0].field();

        return SolrJsonQuery.builder().type("phraseQuery").field(fieldName).value(value).build();
    }

    private static SolrJsonQuery buildTermRangeQuery(TermRangeQuery rangeQuery) {
        return SolrJsonQuery.builder()
                .type("rangeQuery")
                .field(rangeQuery.getField())
                .from(rangeQuery.getLowerTerm().utf8ToString())
                .fromInclude(rangeQuery.includesLower())
                .to(rangeQuery.getUpperTerm().utf8ToString())
                .toInclude(rangeQuery.includesUpper())
                .build();
    }

    private static SolrJsonQuery buildWildcardQuery(WildcardQuery wildcardQuery) {
        return SolrJsonQuery.builder()
                .type("wildcardQuery")
                .field(wildcardQuery.getTerm().field())
                .value(wildcardQuery.getTerm().text())
                .build();
    }

    private static SolrJsonQuery buildMatchAllDocsQuery() {
        return SolrJsonQuery.builder().type("matchAllDocsQuery").field("*").value("*").build();
    }

    private static SolrJsonQuery buildTermQuery(TermQuery termQuery) {
        return SolrJsonQuery.builder()
                .type("termQuery")
                .field(termQuery.getTerm().field())
                .value(termQuery.getTerm().text())
                .build();
    }

    private static SolrJsonQuery buildPrefixQuery(PrefixQuery prefixQuery) {
        return SolrJsonQuery.builder()
                .type("prefixQuery")
                .field(prefixQuery.getPrefix().field())
                .value(prefixQuery.getPrefix().text() + "*")
                .build();
    }
}
