package uk.ac.ebi.uniprot.api.rest.search;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.*;

import java.util.Arrays;
import java.util.List;

/**
 * This class helps to improve default solr query search to get a better score and results.
 *
 * 1. to get a better score, we add boost OR queries based on SearchField configuration ENUM.
 *  for example: P53 query will be converted to: +(content:p53 (taxonomy_name:p53)^2.0 (gene:p53)^2.0)
 *  See test class for more examples.
 *
 * 2. to get a more accurate result we check if the term value is a valid id value and replace it to an id query
 *  for example: P21802 query will be converted to accession:P21802
 *  See test class for more examples.
 *
 * @author lgonzales
 */
public class DefaultSearchHandler {

    private final SearchField defaultField;
    private final SearchField idField;
    private final List<SearchField> boostFields;

    public DefaultSearchHandler(SearchField defaultField,SearchField idField, List<SearchField> boostFields){
        this.defaultField = defaultField;
        this.idField = idField;
        this.boostFields = boostFields;
    }

    public boolean hasDefaultSearch(String inputQuery) {
        boolean isValid = false;
        try {
            QueryParser qp = new QueryParser(defaultField.getName(), new StandardAnalyzer());
            Query query = qp.parse(inputQuery);
            isValid = SolrQueryUtil.hasFieldTerms(query, defaultField.getName());
        } catch (Exception e) {
            //Syntax error is validated by ValidSolrQuerySyntax
        }
        return isValid;
    }

    public String optimiseDefaultSearch(String inputQuery){
        try {
            QueryParser qp = new QueryParser(defaultField.getName(), new StandardAnalyzer());
            qp.setDefaultOperator(QueryParser.Operator.AND); // the same that we have in the solrschema
            Query query = qp.parse(inputQuery);
            return optimiseDefaultSearch(query).toString();
        } catch (Exception e) {
            //Syntax error is validated by ValidSolrQuerySyntax
        }
        return null;
    }

    private Query optimiseDefaultSearch(Query query){
        if (query instanceof TermQuery) {
            TermQuery termQuery = (TermQuery) query;
            String fieldName = termQuery.getTerm().field();
            if(isDefaultSearchTerm(fieldName)){
                return rewriteDefaultTermQuery(termQuery);
            }else {
                return query;
            }
        } else if (query instanceof PhraseQuery) {
            PhraseQuery phraseQuery = (PhraseQuery) query;
            String fieldName = phraseQuery.getTerms()[0].field();
            if(isDefaultSearchTerm(fieldName)){
                return rewriteDefaultPhraseQuery((PhraseQuery)query);
            }else {
                return query;
            }
        } else if (query instanceof BooleanQuery) {
            BooleanQuery.Builder builder = new BooleanQuery.Builder();
            BooleanQuery booleanQuery = (BooleanQuery) query;
            for (BooleanClause clause : booleanQuery.clauses()) {
                Query rewritedQuery = optimiseDefaultSearch(clause.getQuery());
                builder.add(new BooleanClause(rewritedQuery,clause.getOccur()));
            }
            return builder.build();
        } else {
            return query;
        }
    }

    private boolean isDefaultSearchTerm(String fieldName) {
        return fieldName.equalsIgnoreCase(defaultField.getName());
    }

    private Query rewriteDefaultPhraseQuery(PhraseQuery query) {
        BooleanQuery.Builder builder = new BooleanQuery.Builder();
        builder.add(new BooleanClause(query, BooleanClause.Occur.SHOULD));
        String[] values = Arrays.stream(query.getTerms()).map(Term::text).toArray(String[]::new);
        boostFields.stream()
                .filter(searchField -> searchField.hasValidValue(String.join(" ",values)))
                .forEach((field) ->{
                    BoostQuery boostQuery = new BoostQuery(new PhraseQuery(field.getName(),values),field.getBoostValue());
                    builder.add(new BooleanClause(boostQuery, BooleanClause.Occur.SHOULD));
                });
        return builder.build();
    }

    private Query rewriteDefaultTermQuery(TermQuery query) {
        if(idField.hasValidValue(query.getTerm().text())){
            // if it is a valid id (accession) value for example... we search directly in id (accession) field...
            return new TermQuery(new Term(idField.getName(),query.getTerm().bytes()));
        } else {
            // we need to add all boosted fields to the query to return a better scored result.
            BooleanQuery.Builder builder = new BooleanQuery.Builder();
            builder.add(new BooleanClause(query, BooleanClause.Occur.SHOULD));
            boostFields.stream()
                    .filter(searchField -> searchField.hasValidValue(query.getTerm().text()))
                    .forEach((field) -> {
                        Term term = new Term(field.getName(), query.getTerm().bytes());
                        BoostQuery boostQuery = new BoostQuery(new TermQuery(term), field.getBoostValue());
                        builder.add(new BooleanClause(boostQuery, BooleanClause.Occur.SHOULD));
                    });
            return builder.build();
        }
    }

}
