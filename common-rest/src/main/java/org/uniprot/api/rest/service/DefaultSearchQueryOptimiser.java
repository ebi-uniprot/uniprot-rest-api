package org.uniprot.api.rest.service;

import lombok.extern.slf4j.Slf4j;
import org.apache.lucene.analysis.core.WhitespaceAnalyzer;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.util.BytesRef;
import org.uniprot.api.common.exception.ServiceException;
import org.uniprot.store.config.searchfield.model.SearchFieldItem;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.List;

import static org.uniprot.core.util.Utils.notNullNotEmpty;

/**
 * @author lgonzales
 * @since 10/06/2020
 */
@Slf4j
public class DefaultSearchQueryOptimiser {

    private final List<SearchFieldItem> optimisedFields;
    private Field reflectedTermText;
    private Field reflectedTermField;
    private boolean optimisePossible;

    public DefaultSearchQueryOptimiser(SearchFieldItem idField) {
        this(Collections.singletonList(idField));
    }

    public DefaultSearchQueryOptimiser(List<SearchFieldItem> optimisedFields) {
        this.optimisedFields = optimisedFields;
        reflectedTermField = null;
        try {
            reflectedTermField = Term.class.getDeclaredField("field");
            reflectedTermField.setAccessible(true);
            reflectedTermText = Term.class.getDeclaredField("bytes");
            reflectedTermText.setAccessible(true);
            optimisePossible = true;
        } catch (NoSuchFieldException e) {
            log.error(
                    "Could not get Term.field for use when adding concrete fields to default fields, e.g., P12345 -> accession_id:P12345",
                    e);
            optimisePossible = false;
        }
    }

    /**
     * This method go through requested user query and verify if it can be optimised
     *
     * @param requestedQuery requested query
     * @return Optimised search query string
     */
    public String optimiseSearchQuery(String requestedQuery) {
        String result = requestedQuery;
        try {
            QueryParser qp = new QueryParser("", new WhitespaceAnalyzer());
            qp.setAllowLeadingWildcard(true);
            Query parsedQuery = qp.parse(requestedQuery);
            if (optimisePossible) {
                addFieldToDefaultQueries(parsedQuery);
                result = parsedQuery.toString();
            }

            // +- way, deploy, do searches (simple searches + ones that get optimised), check
            // results
            // wwwdev, do searches (simple searches + manually typed optimised queries), check
            // results

            String thing = "XXXXXXXXX ";
            //            List<String> defaultTerms = getDefaultSearchFieldTerms(parsedQuery);
            //            for (String defaultTerm : defaultTerms) {
            //                Optional<TermQuery> optimizedQuery =
            // getOptimisedDefaultTermForValue(defaultTerm);
            //                if (optimizedQuery.isPresent()) {
            //                    if (result.contains("\"" + defaultTerm + "\"")) {
            //                        defaultTerm = "\"" + defaultTerm + "\"";
            //                    }
            //
            //                    result = result.replace(defaultTerm,
            // optimizedQuery.get().toString());
            //                }
            //            }
        } catch (ParseException e) {
            throw new ServiceException(
                    "Unable to parse query in the optimisation" + requestedQuery, e);
        }
        return result;
    }

    private void addFieldToDefaultQueries(Query query) {
        if (query instanceof TermQuery) {
            TermQuery termQuery = (TermQuery) query;
            optimiseIfNecessary(termQuery.getTerm());
        } else if (query instanceof BooleanQuery) {
            BooleanQuery booleanQuery = (BooleanQuery) query;
            for (BooleanClause clause : booleanQuery.clauses()) {
                addFieldToDefaultQueries(clause.getQuery());
            }
        }
    }

    //    private List<String> getDefaultSearchFieldTerms(Query query) {
    //        Field field = null;
    //        try {
    //            field = Term.class.getDeclaredField("field");
    //        } catch (NoSuchFieldException e) {
    //            e.printStackTrace();
    //        }
    //        if (field != null) {
    //
    //            List<String> defaultTerms = new ArrayList<>();
    //            if (query instanceof TermQuery) {
    //                TermQuery termQuery = (TermQuery) query;
    //                if (termQuery.getTerm().field().equals("")) {
    //                    defaultTerms.add(termQuery.getTerm().text());
    //
    //                    Term term = termQuery.getTerm();
    //                    if (term.field().equals("")) {
    //                        try {
    //                            field.setAccessible(true);
    //                            field.set(term, "something");
    //                        } catch (IllegalAccessException e) {
    //                            e.printStackTrace();
    //                        }
    //                    }
    //                }
    //            } else if (query instanceof BooleanQuery) {
    //                BooleanQuery booleanQuery = (BooleanQuery) query;
    //                for (BooleanClause clause : booleanQuery.clauses()) {
    //                    defaultTerms.addAll(getDefaultSearchFieldTerms(clause.getQuery()));
    //                }
    //            }
    //            return defaultTerms;
    //        }
    //        return emptyList();
    //    }

    /**
     * Method to verify if the default term query value can be optimised to use an specific search
     * field
     *
     * <p>For example: In UniProtEntryService implementation if the user type a valid Accession
     * P12345 as a default term value This method implementation would return an optimised query
     * accession:P12345
     *
     * @param termQueryValue requested default term query value
     * @return the optimised term query if can be optimised
     */
    //    private Optional<TermQuery> getOptimisedDefaultTermForValue(String termQueryValue) {
    //        Optional<TermQuery> result = Optional.empty();
    //        for (SearchFieldItem field : optimisedFields) {
    //            if (notNullNotEmpty(field.getValidRegex())
    //                    && termQueryValue.matches(field.getValidRegex())) {
    //                Term optimisedTerm = new Term(field.getFieldName(),
    // termQueryValue.toUpperCase());
    //                result = Optional.of(new TermQuery(optimisedTerm));
    //                break;
    //            }
    //        }
    //        return result;
    //    }

    private void optimiseIfNecessary(Term term) {
        if (term.field().equals("")) {
            for (SearchFieldItem field : optimisedFields) {
                if (notNullNotEmpty(field.getValidRegex())
                        && term.text().matches(field.getValidRegex())) {
                    try {
                        reflectedTermField.set(term, field.getFieldName());
                        reflectedTermText.set(term, new BytesRef(term.text().toUpperCase()));
                    } catch (IllegalAccessException e) {
                        e.printStackTrace();
                    }
                    break;
                }
            }
        }
    }
}
