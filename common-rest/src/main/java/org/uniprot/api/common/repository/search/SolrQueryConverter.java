package org.uniprot.api.common.repository.search;

import static org.uniprot.core.util.Utils.notNullNotEmpty;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.request.json.JsonQueryRequest;
import org.apache.solr.client.solrj.request.json.TermsFacetMap;
import org.apache.solr.common.params.ModifiableSolrParams;
import org.uniprot.api.common.repository.search.facet.FacetConfig;
import org.uniprot.api.common.repository.search.facet.FacetProperty;
import org.uniprot.api.common.repository.search.request.BoostApplier;
import org.uniprot.core.util.Utils;

public class SolrQueryConverter {
    private static final String QUERY_OPERATOR = "q.op";
    private static final Pattern SINGLE_TERM_PATTERN = Pattern.compile("^\\w+$");
    public static final String TERMS_LIST = "terms.list";
    public static final String TERMS = "terms";
    public static final String DISTRIB = "distrib";
    public static final String TERMS_FIELDS = "terms.fl";
    public static final String MINCOUNT = "terms.mincount";
    public static final String EDISMAX = "edismax";
    public static final String DEF_TYPE = "defType";
    public static final String DEFAULT_FIELD = "df";
    public static final String QUERY_FIELDS = "qf";
    public static final String FILTER_QUERY = "fq";
    public static final String HIGHLIGHT = "hl";
    public static final String HIGHLIGHT_FIELDS = "hl.fl";
    public static final String HIGHLIGHT_PRE = "hl.simple.pre";
    public static final String HIGHLIGHT_POST = "hl.simple.post";

    private SolrQueryConverter() {}

    static void setTermFields(
            ModifiableSolrParams solrQuery, String termQuery, List<String> termFields) {
        if (isSingleTerm(termQuery)) {
            solrQuery.add(TERMS_LIST, termQuery.toLowerCase());

            solrQuery.add(TERMS, "true");
            solrQuery.add(DISTRIB, "true");
            solrQuery.add(MINCOUNT, "1");

            String[] termsFieldsArr = new String[termFields.size()];
            termsFieldsArr = termFields.toArray(termsFieldsArr);
            solrQuery.add(TERMS_FIELDS, termsFieldsArr);
        }
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

    static void setHighlightFieldsConfigs(ModifiableSolrParams solrQuery, SolrQueryConfig config) {
        if (Utils.notNullNotEmpty(config.getHighlightFields())) {
            solrQuery.add(HIGHLIGHT, "on");
            solrQuery.add(HIGHLIGHT_FIELDS, config.getHighlightFields());
            solrQuery.add(HIGHLIGHT_PRE, "<span class=\"match-highlight\">");
            solrQuery.add(HIGHLIGHT_POST, "</span>");
        }
    }

    static void setQueryBoostConfigs(
            ModifiableSolrParams solrQuery, String query, SolrQueryConfig boosts) {
        BoostApplier.addBoosts(solrQuery, query, boosts);
    }

    static void setQueryFields(ModifiableSolrParams solrQuery, SolrRequest request) {
        if (notNullNotEmpty(request.getQueryField())) {
            solrQuery.add(QUERY_FIELDS, request.getQueryField());
        }
    }

    static void setDefaults(ModifiableSolrParams solrQuery, String defaultField) {
        if (defaultField != null) {
            solrQuery.add(DEFAULT_FIELD, defaultField);
        }
        solrQuery.add(DEF_TYPE, EDISMAX);
    }
}
