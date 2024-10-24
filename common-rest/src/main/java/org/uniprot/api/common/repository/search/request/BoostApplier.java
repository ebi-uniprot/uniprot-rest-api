package org.uniprot.api.common.repository.search.request;

import static org.uniprot.api.common.repository.search.SolrQueryConfigFileReader.BOOST_FIELD_TYPE_NUMBER;
import static org.uniprot.api.common.repository.search.SolrQueryConfigFileReader.QUERY_PLACEHOLDER;
import static org.uniprot.core.util.Utils.nullOrEmpty;

import java.util.List;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.apache.solr.common.params.ModifiableSolrParams;
import org.uniprot.api.common.repository.search.SolrQueryConfig;
import org.uniprot.api.common.repository.search.SolrRequest;

/**
 * The purpose of this class is to apply the boosts of a {@link SolrQueryConfig} to a Solr query
 * ({@link ModifiableSolrParams}), this includes field boosts, static boosts, and boost functions.
 *
 * <p>Created 14/04/2022
 *
 * @author Edd
 */
public class BoostApplier {
    private static final String BOOST_QUERY = "bq";
    private static final String BOOST_FUNCTIONS = "boost";

    public static void addBoosts(ModifiableSolrParams solrQuery, SolrRequest request) {
        List<String> fieldBoosts = request.getFieldBoosts();
        List<String> staticBoosts = request.getStaticBoosts();

        Set<String> defaultTermsInQuery =
                DefaultTermExtractor.extractDefaultTerms(request.getQuery());

        // for every default term, e.g., kinase
        for (String term : defaultTermsInQuery) {
            // for every field boost, e.g., x:{query}^1.1
            // apply the boost as normal
            fieldBoosts.forEach(
                    boost -> {
                        if (boost.contains(BOOST_FIELD_TYPE_NUMBER)) {
                            // apply the boost if the value is numeric
                            if (StringUtils.isNumeric(term)) {
                                // user query is numeric and therefore we can replace
                                // the "{query}" placeholder with their value
                                String processedBoost =
                                        boost.replace(BOOST_FIELD_TYPE_NUMBER, ":")
                                                .replace(QUERY_PLACEHOLDER, "(" + term + ")");
                                solrQuery.add(BOOST_QUERY, processedBoost);
                            }
                        } else {
                            // apply the boost if the value is non-numeric
                            if (!StringUtils.isNumeric(term)) {
                                String processedBoost =
                                        boost.replace(QUERY_PLACEHOLDER, "(" + term + ")");
                                solrQuery.add(BOOST_QUERY, processedBoost);
                            }
                        }
                    });
        }

        // add all static boosts
        staticBoosts.forEach(boost -> solrQuery.add(BOOST_QUERY, boost));

        // set boost functions
        if (!nullOrEmpty(request.getBoostFunctions())) {
            solrQuery.add(BOOST_FUNCTIONS, request.getBoostFunctions());
        }
    }
}
