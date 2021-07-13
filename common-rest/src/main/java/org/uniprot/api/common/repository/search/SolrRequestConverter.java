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
import org.apache.solr.common.params.ModifiableSolrParams;
import org.uniprot.api.common.exception.InvalidRequestException;
import org.uniprot.api.common.repository.search.facet.FacetConfig;
import org.uniprot.api.common.repository.search.facet.FacetProperty;
import org.uniprot.core.util.Utils;

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
        final ModifiableSolrParams solrQuery = new ModifiableSolrParams();
        solrQuery.add("q", request.getQuery());

        if (!isEntry) {
            setDefaults(solrQuery, request.getDefaultField());
        }

        solrQuery.add("rows", String.valueOf(request.getRows()));
        setFilterQueries(solrQuery, request.getFilterQueries());
        setQueryOperator(solrQuery, request.getDefaultQueryOperator());
        if (!request.getTermFields().isEmpty()) {
            if (nullOrEmpty(request.getTermQuery())) {
                throw new InvalidRequestException("Please specify required field, term query.");
            }
            setTermFields(solrQuery, request.getTermQuery(), request.getTermFields());
        }

        if (notNull(request.getQueryConfig())) {
            if (request.getRows() > 1) {
                setQueryBoostConfigs(solrQuery, request.getQuery(), request.getQueryConfig());
                setHighlightFieldsConfigs(solrQuery, request.getQueryConfig());
            }
            setQueryFields(solrQuery, request.getQueryConfig());
        }

        JsonQueryRequest result = new JsonQueryRequest(solrQuery);
        setSort(result, request.getSorts());
        if (!request.getFacets().isEmpty()) {
            setFacets(result, request.getFacets(), request.getFacetConfig());
        }

        log.debug("Solr Query without facet and sort details: " + solrQuery);

        return result;
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
        private static final String HIGHLIGHT = "hl";
        private static final String HIGHLIGHT_FIELDS = "hl.fl";
        private static final String HIGHLIGHT_PRE = "hl.simple.pre";
        private static final String HIGHLIGHT_POST = "hl.simple.post";

        private SolrQueryConverter() {}

        static void setTermFields(
                ModifiableSolrParams solrQuery, String termQuery, List<String> termFields) {
            if (isSingleTerm(termQuery)) {
                solrQuery.add(TERMS_LIST, termQuery.toLowerCase());
            } else {
                throw new InvalidRequestException(
                        "Term information will only be returned for single value searches that do not specify a field.");
            }

            solrQuery.add(TERMS, "true");
            solrQuery.add(DISTRIB, "true");
            solrQuery.add(MINCOUNT, "1");

            String[] termsFieldsArr = new String[termFields.size()];
            termsFieldsArr = termFields.toArray(termsFieldsArr);
            solrQuery.add(TERMS_FIELDS, termsFieldsArr);
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
                ModifiableSolrParams solrQuery, QueryOperator defaultQueryOperator) {
            solrQuery.add(QUERY_OPERATOR, defaultQueryOperator.name());
        }

        static void setFilterQueries(ModifiableSolrParams solrQuery, List<String> filterQueries) {
            String[] filterQueryArr = new String[filterQueries.size()];
            filterQueryArr = filterQueries.toArray(filterQueryArr);
            solrQuery.add("fq", filterQueryArr);
        }

        static void setSort(JsonQueryRequest solrQuery, List<SolrQuery.SortClause> sorts) {
            String sort =
                    sorts.stream()
                            .map(clause -> clause.getItem() + " " + clause.getOrder().toString())
                            .collect(Collectors.joining(","));
            solrQuery.setSort(sort);
        }

        static void setHighlightFieldsConfigs(
                ModifiableSolrParams solrQuery, SolrQueryConfig config) {
            if (Utils.notNullNotEmpty(config.getHighlightFields())) {
                solrQuery.add(HIGHLIGHT, "on");
                solrQuery.add(HIGHLIGHT_FIELDS, config.getHighlightFields());
                solrQuery.add(HIGHLIGHT_PRE, "<span class=\"match-highlight\">");
                solrQuery.add(HIGHLIGHT_POST, "</span>");
            }
        }

        static void setQueryBoostConfigs(
                ModifiableSolrParams solrQuery, String query, SolrQueryConfig boosts) {
            Matcher fieldQueryMatcher = FIELD_QUERY_PATTERN.matcher(query);
            if (fieldQueryMatcher.find()) {
                // a query involving field queries
                if (notNullNotEmpty(boosts.getAdvancedSearchBoosts())) {
                    boosts.getAdvancedSearchBoosts()
                            .forEach(boost -> solrQuery.add(BOOST_QUERY, boost));
                }
                if (!nullOrEmpty(boosts.getAdvancedSearchBoostFunctions())) {
                    solrQuery.add(BOOST_FUNCTIONS, boosts.getAdvancedSearchBoostFunctions());
                }
            } else {
                // a default query
                if (notNullNotEmpty(boosts.getDefaultSearchBoosts())) {
                    // replace all occurrences of "{query}" with X, given that q=X
                    boosts.getDefaultSearchBoosts()
                            .forEach(boost -> addQueryBoost(solrQuery, boost, query));
                }
                if (!nullOrEmpty(boosts.getDefaultSearchBoostFunctions())) {
                    solrQuery.add(BOOST_FUNCTIONS, boosts.getDefaultSearchBoostFunctions());
                }
            }
        }

        private static void addQueryBoost(
                ModifiableSolrParams solrQuery, String boost, String query) {
            if (boostingOnANumericField(boost)) {
                // only apply the boost if the value is numeric
                if (StringUtils.isNumeric(query)) {
                    // user query is numeric and therefore we can replace
                    // the "{query}" placeholder with their value
                    String processedBoost =
                            boost.replace(BOOST_FIELD_TYPE_NUMBER, ":")
                                    .replace(QUERY_PLACEHOLDER, "(" + query + ")");
                    solrQuery.add(BOOST_QUERY, processedBoost);
                }
            } else {
                // apply the boost as normal
                String processedBoost = boost.replace(QUERY_PLACEHOLDER, "(" + query + ")");
                solrQuery.add(BOOST_QUERY, processedBoost);
            }
        }

        static void setQueryFields(ModifiableSolrParams solrQuery, SolrQueryConfig boosts) {
            if (notNullNotEmpty(boosts.getQueryFields())) {
                solrQuery.add(QUERY_FIELDS, boosts.getQueryFields());
            }
        }
        /**
         * @param boostDefinition the boost definition
         * @return whether a boost definition involves a numeric field
         */
        private static boolean boostingOnANumericField(String boostDefinition) {
            return boostDefinition.contains(BOOST_FIELD_TYPE_NUMBER);
        }

        static void setDefaults(ModifiableSolrParams solrQuery, String defaultField) {
            if (defaultField != null) {
                solrQuery.add(DEFAULT_FIELD, defaultField);
            }
            solrQuery.add(DEF_TYPE, EDISMAX);
        }
    }
}
