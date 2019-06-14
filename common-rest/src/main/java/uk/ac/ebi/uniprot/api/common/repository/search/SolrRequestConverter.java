package uk.ac.ebi.uniprot.api.common.repository.search;

import org.apache.solr.client.solrj.SolrQuery;
import org.springframework.data.domain.Sort;
import org.springframework.data.solr.core.query.FacetOptions;
import org.springframework.data.solr.core.query.Query;
import org.springframework.data.solr.core.query.SimpleFacetQuery;
import org.springframework.data.solr.core.query.SimpleQuery;
import uk.ac.ebi.uniprot.api.common.exception.InvalidRequestException;
import uk.ac.ebi.uniprot.api.common.repository.search.facet.GenericFacetConfig;

import java.util.List;
import java.util.regex.Pattern;

import static uk.ac.ebi.uniprot.api.common.repository.search.SolrRequestConverter.QueryConverter.getSimpleFacetQuery;
import static uk.ac.ebi.uniprot.api.common.repository.search.SolrRequestConverter.SolrQueryConverter.*;
import static uk.ac.ebi.uniprot.common.Utils.nonNull;

/**
 * Created 14/06/19
 *
 * @author Edd
 */
// TODO: 14/06/19 test this class
class SolrRequestConverter {
    private static final String QUERY_OPERATOR = "q.op";
    private static final Pattern SINGLE_TERM_PATTERN = Pattern.compile("^\\w+$");
    private static final String TERMS_LIST = "terms.list";
    private static final String TERMS = "terms";
    private static final String DISTRIB = "distrib";
    private static final String TERMS_FIELDS = "terms.fl";

    static SolrQuery toSolrQuery(SolrRequest request) {
        SolrQuery solrQuery = new SolrQuery(request.getQuery());

        setFilterQueries(solrQuery, request.getFilterQueries());
        setSort(solrQuery, request.getSort());
        setQueryOperator(solrQuery, request.getDefaultQueryOperator());
        if (!request.getFacets().isEmpty()) {
            setFacets(solrQuery, request.getFacets(), request.getFacetConfig());
        }
        if (!request.getTermFields().isEmpty()) {
            setTermFields(solrQuery, request.getTermQuery(), request.getTermFields());
        }

        return solrQuery;
    }

    static Query toQuery(SolrRequest request) {
        SimpleQuery simpleQuery = new SimpleQuery(request.getQuery());

        if (!request.getFacets().isEmpty()) {
            simpleQuery = getSimpleFacetQuery(simpleQuery, request);
        }

        request.getFilterQueries().stream().map(SimpleQuery::new).forEach(simpleQuery::addFilterQuery);

        simpleQuery.addSort(request.getSort());
        simpleQuery.setDefaultOperator(request.getDefaultQueryOperator());

        return simpleQuery;
    }

    static class QueryConverter {
        static SimpleFacetQuery getSimpleFacetQuery(SimpleQuery simpleQuery, SolrRequest request) {
            SimpleFacetQuery simpleFacetQuery = new SimpleFacetQuery(simpleQuery.getCriteria());

            FacetOptions facetOptions = new FacetOptions();
            facetOptions.addFacetOnFlieldnames(request.getFacets());
            facetOptions.setFacetMinCount(request.getFacetConfig().getMincount());
            facetOptions.setFacetLimit(request.getFacetConfig().getLimit());
            simpleFacetQuery.setFacetOptions(facetOptions);

            return simpleFacetQuery;
        }
    }

    static class SolrQueryConverter {
        static void setTermFields(SolrQuery solrQuery, String termQuery, List<String> termFields) {
            if (isSingleTerm(termQuery)) {
                solrQuery.setParam(TERMS_LIST, termQuery);
            } else {
                throw new InvalidRequestException("Term information will only be returned for single value searches that do not specify a field.");
            }

            solrQuery.setParam(TERMS, "true");
            // TODO: 14/06/19 refactor class so that this can be overridden in tests
            solrQuery.setParam(DISTRIB, "false");

            String[] termsFieldsArr = new String[termFields.size()];
            termsFieldsArr = termFields.toArray(termsFieldsArr);
            solrQuery.setParam(TERMS_FIELDS, termsFieldsArr);
        }

        static boolean isSingleTerm(String query) {
            return SINGLE_TERM_PATTERN.matcher(query).matches();
        }

        static void setFacets(SolrQuery solrQuery, List<String> facets, GenericFacetConfig facetConfig) {
            solrQuery.setFacet(true);

            String[] facetArr = new String[facets.size()];
            facetArr = facets.toArray(facetArr);
            solrQuery.addFacetField(facetArr);

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
            if (nonNull(sort)) {
                for (Sort.Order order : sort) {
                    solrQuery
                            .addSort(order.getProperty(), order.isAscending() ? SolrQuery.ORDER.asc : SolrQuery.ORDER.desc);
                }
            }
        }
    }
}
