package org.uniprot.api.common.repository.search;

import static org.uniprot.api.common.repository.search.SolrRequestConverter.SolrQueryConverter.*;
import static org.uniprot.core.util.Utils.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import lombok.extern.slf4j.Slf4j;

import org.apache.commons.lang.StringUtils;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.request.json.JsonQueryRequest;
import org.apache.solr.client.solrj.request.json.TermsFacetMap;
import org.uniprot.api.common.exception.InvalidRequestException;
import org.uniprot.api.common.repository.search.facet.FacetConfig;
import org.uniprot.api.common.repository.search.facet.FacetProperty;

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
    public JsonQueryRequest toJsonQueryRequest(SolrRequest request) {
        return toJsonQueryRequest(request, false);
    }

    public JsonQueryRequest toJsonQueryRequest(SolrRequest request, boolean isEntry) {
        JsonQueryRequest solrQuery = new JsonQueryRequest();

        solrQuery.setQuery(request.getQuery());
        if (!isEntry) {
            setDefaults(solrQuery, request.getDefaultField());
        }

        solrQuery.setLimit(request.getRows());
        setFilterQueries(solrQuery, request.getFilterQueries());
        setSort(solrQuery, request.getSorts());
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

        if (notNull(request.getQueryConfig())) {
            setQueryConfigs(solrQuery, request.getQuery(), request.getQueryConfig());
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
        private static final String BOOST_FIELD_TYPE_NUMBER = "=number:";
        private static final String QUERY_PLACEHOLDER = "{query}";
        private static final String QUERY_FIELDS = "qf";

        private SolrQueryConverter() {}

        static void setTermFields(
                JsonQueryRequest solrQuery, String termQuery, List<String> termFields) {
            if (isSingleTerm(termQuery)) {
                solrQuery.withParam(TERMS_LIST, termQuery.toLowerCase());
            } else {
                throw new InvalidRequestException(
                        "Term information will only be returned for single value searches that do not specify a field.");
            }

            solrQuery.withParam(TERMS, "true");
            solrQuery.withParam(DISTRIB, "true");
            solrQuery.withParam(MINCOUNT, "1");

            String[] termsFieldsArr = new String[termFields.size()];
            termsFieldsArr = termFields.toArray(termsFieldsArr);
            solrQuery.withParam(TERMS_FIELDS, termsFieldsArr);
        }

        static boolean isSingleTerm(String query) {
            return SINGLE_TERM_PATTERN.matcher(query).matches();
        }

        static void setFacets(
                JsonQueryRequest solrQuery, List<String> facets, FacetConfig facetConfig) {
            for (String facetName : facets) {
                FacetProperty facetProperty = facetConfig.getFacetPropertyMap().get(facetName);
                if (notNullNotEmpty(facetProperty.getInterval())) {
                    Map<String, Object> rangeFacet = new HashMap<>();
                    rangeFacet.put("field", facetName);
                    rangeFacet.put("type", "range");
                    rangeFacet.put("refine", "true");
                    List<Object> ranges = new ArrayList<>();
                    for (int i = 1; i <= facetProperty.getInterval().size(); i++) {
                        String rangeItem = facetProperty.getInterval().get("" + i);
                        Map<String, Object> range = new HashMap<>();
                        range.put("range", rangeItem);
                        ranges.add(range);
                    }
                    rangeFacet.put("ranges", ranges);
                    solrQuery.withFacet(facetName, rangeFacet);
                } else {
                    final TermsFacetMap facet = new TermsFacetMap(facetName);
                    facet.setMinCount(facetConfig.getMincount());

                    if (facetProperty.getLimit() <= 0) {
                        facet.setLimit(facetConfig.getLimit()); // default facet.limit property
                    } else {
                        facet.setLimit(facetProperty.getLimit());
                    }

                    if (facetProperty.getSort() != null) {
                        facet.setSort(facetProperty.getSort());
                    }
                    facet.useDistributedFacetRefining(true);
                    solrQuery.withFacet(facetName, facet);
                }
            }
        }

        static void setQueryOperator(
                JsonQueryRequest solrQuery, QueryOperator defaultQueryOperator) {
            solrQuery.withParam(QUERY_OPERATOR, defaultQueryOperator.name());
        }

        static void setFilterQueries(JsonQueryRequest solrQuery, List<String> filterQueries) {
            filterQueries.forEach(solrQuery::withFilter);
        }

        static void setSort(JsonQueryRequest solrQuery, List<SolrQuery.SortClause> sorts) {
            String sort =
                    sorts.stream()
                            .map(clause -> clause.getItem() + " " + clause.getOrder().toString())
                            .collect(Collectors.joining(","));
            solrQuery.setSort(sort);
        }

        static void setQueryConfigs(
                JsonQueryRequest solrQuery, String query, SolrQueryConfig boosts) {
            Matcher fieldQueryMatcher = FIELD_QUERY_PATTERN.matcher(query);
            if (fieldQueryMatcher.find()) {
                // a query involving field queries
                if (notNullNotEmpty(boosts.getAdvancedSearchBoosts())) {
                    String boostValues = String.join(" ", boosts.getAdvancedSearchBoosts());
                    solrQuery.withParam(BOOST_QUERY, boostValues);
                }
                if (!nullOrEmpty(boosts.getAdvancedSearchBoostFunctions())) {
                    solrQuery.withParam(BOOST_FUNCTIONS, boosts.getAdvancedSearchBoostFunctions());
                }
            } else {
                // a default query
                if (notNullNotEmpty(boosts.getDefaultSearchBoosts())) {
                    // replace all occurrences of "{query}" with X, given that q=X
                    addQueryBoost(solrQuery, boosts.getDefaultSearchBoosts(), query);
                }
                if (!nullOrEmpty(boosts.getDefaultSearchBoostFunctions())) {
                    solrQuery.withParam(BOOST_FUNCTIONS, boosts.getDefaultSearchBoostFunctions());
                }
            }

            if (notNullNotEmpty(boosts.getQueryFields())) {
                solrQuery.withParam(QUERY_FIELDS, boosts.getQueryFields());
            }
        }

        private static void addQueryBoost(
                JsonQueryRequest solrQuery, List<String> boosts, String query) {
            List<String> boostQueries = new ArrayList<>();
            for (String boost : boosts) {
                if (boostingOnANumericField(boost)) {
                    // only apply the boost if the value is numeric
                    if (StringUtils.isNumeric(query)) {
                        // user query is numeric and therefore we can replace
                        // the "{query}" placeholder with their value
                        String processedBoost =
                                boost.replace(BOOST_FIELD_TYPE_NUMBER, ":")
                                        .replace(QUERY_PLACEHOLDER, "(" + query + ")");
                        boostQueries.add(processedBoost);
                    }
                } else {
                    // apply the boost as normal
                    String processedBoost = boost.replace(QUERY_PLACEHOLDER, "(" + query + ")");
                    boostQueries.add(processedBoost);
                }
            }
            solrQuery.withParam(BOOST_QUERY, String.join(" ", boostQueries));
        }

        /**
         * @param boostDefinition the boost definition
         * @return whether a boost definition involves a numeric field
         */
        private static boolean boostingOnANumericField(String boostDefinition) {
            return boostDefinition.contains(BOOST_FIELD_TYPE_NUMBER);
        }

        static void setDefaults(JsonQueryRequest solrQuery, String defaultField) {
            if (defaultField != null) {
                solrQuery.withParam(DEFAULT_FIELD, defaultField);
            }
            solrQuery.withParam(DEF_TYPE, EDISMAX);
        }
    }
}
