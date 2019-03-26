package uk.ac.ebi.uniprot.rest.search;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.*;

import java.util.Arrays;
import java.util.List;

/**
 * This is a utility class to help extract information about solr query
 *
 * @author lgonzales
 */
public class SolrQueryUtil {

    public static boolean hasFieldTerms(String inputQuery, String... terms) {
        boolean isValid = false;
        try {
            QueryParser qp = new QueryParser("", new StandardAnalyzer());
            Query query = qp.parse(inputQuery);
            isValid = hasFieldTerms(query, terms);
        } catch (Exception e) {
            //Syntax error is validated by ValidSolrQuerySyntax
        }
        return isValid;
    }

    public static boolean hasFieldTerms(Query inputQuery, String... terms) {
        boolean hasTerm = false;
        List<String> termList = Arrays.asList(terms);
        if (inputQuery instanceof TermQuery) {
            TermQuery termQuery = (TermQuery) inputQuery;
            String fieldName = termQuery.getTerm().field();
            hasTerm = termList.contains(fieldName);
        } else if (inputQuery instanceof WildcardQuery) {
            WildcardQuery wildcardQuery = (WildcardQuery) inputQuery;
            String fieldName = wildcardQuery.getTerm().field();
            hasTerm = termList.contains(fieldName);
        } else if (inputQuery instanceof TermRangeQuery) {
            TermRangeQuery rangeQuery = (TermRangeQuery) inputQuery;
            String fieldName = rangeQuery.getField();
            hasTerm = termList.contains(fieldName);
        } else if (inputQuery instanceof PhraseQuery) {
            PhraseQuery phraseQuery = (PhraseQuery) inputQuery;
            String fieldName = phraseQuery.getTerms()[0].field();
            hasTerm = termList.contains(fieldName);
        } else if (inputQuery instanceof BooleanQuery) {
            BooleanQuery booleanQuery = (BooleanQuery) inputQuery;
            for (BooleanClause clause : booleanQuery.clauses()) {
                if (hasFieldTerms(clause.getQuery(), terms)) {
                    hasTerm = true;
                }
            }
        }
        return hasTerm;
    }

}
