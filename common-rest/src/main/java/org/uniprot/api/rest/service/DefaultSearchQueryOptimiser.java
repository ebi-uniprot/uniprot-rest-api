package org.uniprot.api.rest.service;

import static org.uniprot.core.util.Utils.notNullNotEmpty;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.apache.lucene.analysis.core.WhitespaceAnalyzer;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.uniprot.api.common.exception.ServiceException;
import org.uniprot.store.config.searchfield.model.SearchFieldItem;

/**
 * @author lgonzales
 * @since 10/06/2020
 */
public class DefaultSearchQueryOptimiser {

    private final List<SearchFieldItem> optimisedFields;

    public DefaultSearchQueryOptimiser(SearchFieldItem idField) {
        this(Collections.singletonList(idField));
    }

    public DefaultSearchQueryOptimiser(List<SearchFieldItem> optimisedFields) {
        this.optimisedFields = optimisedFields;
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
            List<String> defaultTerms = getDefaultSearchFieldTerms(parsedQuery);
            for (String defaultTerm : defaultTerms) {
                Optional<TermQuery> optimizedQuery = getOptimisedDefaultTermForValue(defaultTerm);
                if (optimizedQuery.isPresent()) {
                    if (result.contains("\"" + defaultTerm + "\"")) {
                        defaultTerm = "\"" + defaultTerm + "\"";
                    }
                    result = result.replace(defaultTerm, optimizedQuery.get().toString());
                }
            }
        } catch (ParseException e) {
            throw new ServiceException(
                    "Unable to parse query in the optimisation" + requestedQuery, e);
        }
        return result;
    }

    private List<String> getDefaultSearchFieldTerms(Query query) {
        List<String> defaultTerms = new ArrayList<>();
        if (query instanceof TermQuery) {
            TermQuery termQuery = (TermQuery) query;
            if (termQuery.getTerm().field().equals("")) {
                defaultTerms.add(termQuery.getTerm().text());
            }
        } else if (query instanceof BooleanQuery) {
            BooleanQuery booleanQuery = (BooleanQuery) query;
            for (BooleanClause clause : booleanQuery.clauses()) {
                defaultTerms.addAll(getDefaultSearchFieldTerms(clause.getQuery()));
            }
        }
        return defaultTerms;
    }

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
    private Optional<TermQuery> getOptimisedDefaultTermForValue(String termQueryValue) {
        Optional<TermQuery> result = Optional.empty();
        for (SearchFieldItem field : optimisedFields) {
            if (notNullNotEmpty(field.getValidRegex())
                    && termQueryValue.matches(field.getValidRegex())) {
                Term optimisedTerm = new Term(field.getFieldName(), termQueryValue.toUpperCase());
                result = Optional.of(new TermQuery(optimisedTerm));
                break;
            }
        }
        return result;
    }
}
