package org.uniprot.api.common.repository.search;

import static org.uniprot.api.common.repository.search.SolrRequestConverter.QueryConverter.getSimpleFacetQuery;
import static org.uniprot.api.common.repository.search.SolrRequestConverter.SolrQueryConverter.*;
import static org.uniprot.core.util.Utils.*;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import lombok.extern.slf4j.Slf4j;

import org.apache.solr.client.solrj.SolrQuery;
import org.springframework.data.domain.Sort;
import org.springframework.data.solr.core.query.FacetOptions;
import org.springframework.data.solr.core.query.Query;
import org.springframework.data.solr.core.query.SimpleFacetQuery;
import org.springframework.data.solr.core.query.SimpleQuery;
import org.uniprot.api.common.exception.InvalidRequestException;
import org.uniprot.api.common.repository.search.facet.FacetConfig;
import org.uniprot.api.common.repository.search.facet.FacetProperty;
import org.uniprot.core.util.Utils;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.uniprot.api.common.repository.search.SolrRequestConverter.QueryConverter.getSimpleFacetQuery;
import static org.uniprot.api.common.repository.search.SolrRequestConverter.SolrQueryConverter.*;
import static org.uniprot.core.util.Utils.*;

/**
 * Created 14/06/19
 *
 * @author Edd
 */
@Slf4j
public class SolrRequestConverter {
    /**
     * Creates a {@link SolrQuery} from a {@link SolrRequest}.
     *
     * @param request the request that specifies the query
     * @return the solr query
     */
    public SolrQuery toSolrQuery(SolrRequest request) {
        return toSolrQuery(request, false);
    }

    public SolrQuery toSolrQuery(SolrRequest request, boolean isEntry) {
        SolrQuery solrQuery = new SolrQuery(request.getQuery());

        if (!isEntry) {
            setDefaults(solrQuery, request.getDefaultField());
        }

        setFilterQueries(solrQuery, request.getFilterQueries());
        setSort(solrQuery, request.getSort());
        setQueryOperator(solrQuery, request.getDefaultQueryOperator());
        if (!request.getFacets().isEmpty()) {
            setFacets(solrQuery, request.getFacets(), request.getFacetConfig());
        }
        if (!request.getTermFields().isEmpty()) {
            if (nullOrEmpty(request.getTermQuery())) {
                throw new InvalidRequestException("Please specify required field, term query.");
            }
            setTermFields(solrQuery, request.getTermQuery(), request.getTermFields());
        }

        if (notNull(request.getQueryBoosts())) {
            setQueryBoosts(solrQuery, request.getQuery(), request.getQueryBoosts());
        }

        log.debug("Solr Query: " + solrQuery);

        return solrQuery;
    }

    static class SolrQueryConverter {
        private static final String QUERY_OPERATOR = "q.op";
        private static final Pattern SINGLE_TERM_PATTERN = Pattern.compile("^\\w+$");
        private static final String TERMS_LIST = "terms.list";
        private static final String TERMS = "terms";
        private static final String DISTRIB = "distrib";
        private static final String TERMS_FIELDS = "terms.fl";
        private static final String MINCOUNT = "terms.mincount";
        private static final Pattern FIELD_QUERY_PATTERN = Pattern.compile("\\w+:");
        private static final String BOOST_QUERY = "bq";
        private static final String BOOST_FUNCTIONS = "boost";
        private static final String EDISMAX = "edismax";
        private static final String DEF_TYPE = "defType";
        private static final String DEFAULT_FIELD = "df";

        static void setTermFields(SolrQuery solrQuery, String termQuery, List<String> termFields) {
            if (isSingleTerm(termQuery)) {
                solrQuery.setParam(TERMS_LIST, termQuery.toLowerCase());
            } else {
                throw new InvalidRequestException(
                        "Term information will only be returned for single value searches that do not specify a field.");
            }

            solrQuery.setParam(TERMS, "true");
            solrQuery.setParam(DISTRIB, "true");
            solrQuery.setParam(MINCOUNT, "1");

            String[] termsFieldsArr = new String[termFields.size()];
            termsFieldsArr = termFields.toArray(termsFieldsArr);
            solrQuery.setParam(TERMS_FIELDS, termsFieldsArr);
        }

        static boolean isSingleTerm(String query) {
            return SINGLE_TERM_PATTERN.matcher(query).matches();
        }

        static void setFacets(SolrQuery solrQuery, List<String> facets, FacetConfig facetConfig) {
            solrQuery.setFacet(true);

            for (String facetName : facets) {
                FacetProperty facetProperty = facetConfig.getFacetPropertyMap().get(facetName);
                if (Utils.notNullOrEmpty(facetProperty.getInterval())) {
                    String[] facetIntervals =
                            facetProperty.getInterval().values().toArray(new String[0]);
                    solrQuery.addIntervalFacets(facetName, facetIntervals);
                } else {
                    solrQuery.addFacetField(facetName);
                }
                if (facetProperty.getLimit() != 0) {
                    solrQuery.add(
                            String.format("f.%s.facet.limit", facetName),
                            String.valueOf(facetProperty.getLimit()));
                }
            }
            solrQuery.setFacetLimit(facetConfig.getLimit());
            solrQuery.setFacetMinCount(facetConfig.getMincount());
        }

        static void setQueryOperator(SolrQuery solrQuery, Query.Operator defaultQueryOperator) {
            solrQuery.set(QUERY_OPERATOR, defaultQueryOperator.asQueryStringRepresentation());
        }

        static void setFilterQueries(SolrQuery solrQuery, List<String> filterQueries) {
            String[] filterQueryArr = new String[filterQueries.size()];
            filterQueryArr = filterQueries.toArray(filterQueryArr);
            solrQuery.setFilterQueries(filterQueryArr);
        }

        static void setSort(SolrQuery solrQuery, Sort sort) {
            if (notNull(sort)) {
                for (Sort.Order order : sort) {
                    solrQuery.addSort(
                            order.getProperty(),
                            order.isAscending() ? SolrQuery.ORDER.asc : SolrQuery.ORDER.desc);
                }
            }
        }

        static void setQueryBoosts(SolrQuery solrQuery, String query, QueryBoosts boosts) {
            Matcher fieldQueryMatcher = FIELD_QUERY_PATTERN.matcher(query);
            if (fieldQueryMatcher.find()) {
                // a query involving field queries
                if (notNullOrEmpty(boosts.getAdvancedSearchBoosts())) {
                    boosts.getAdvancedSearchBoosts()
                            .forEach(boost -> solrQuery.add(BOOST_QUERY, boost));
                }
                if (!nullOrEmpty(boosts.getAdvancedSearchBoostFunctions())) {
                    solrQuery.add(BOOST_FUNCTIONS, boosts.getAdvancedSearchBoostFunctions());
                }
            } else {
                // a default query
                if (notNullOrEmpty(boosts.getDefaultSearchBoosts())) {
                    // replace all occurrences of "{query}" with X, given that q=X
                    boosts.getDefaultSearchBoosts().stream()
                            .map(boost -> boost.replaceAll("\\{query\\}", "(" + query + ")"))
                            .forEach(boost -> solrQuery.add(BOOST_QUERY, boost));
                }
                if (!nullOrEmpty(boosts.getDefaultSearchBoostFunctions())) {
                    solrQuery.add(BOOST_FUNCTIONS, boosts.getDefaultSearchBoostFunctions());
                }
            }
        }

        static void setDefaults(SolrQuery solrQuery, String defaultField) {
            solrQuery.setParam(DEFAULT_FIELD, defaultField);
            solrQuery.setParam(DEF_TYPE, EDISMAX);
        }
    }
}
