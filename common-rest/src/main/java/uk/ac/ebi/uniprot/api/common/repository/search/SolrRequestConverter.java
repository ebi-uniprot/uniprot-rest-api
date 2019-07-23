package uk.ac.ebi.uniprot.api.common.repository.search;

import org.apache.solr.client.solrj.SolrQuery;
import org.springframework.data.domain.Sort;
import org.springframework.data.solr.core.query.FacetOptions;
import org.springframework.data.solr.core.query.Query;
import org.springframework.data.solr.core.query.SimpleFacetQuery;
import org.springframework.data.solr.core.query.SimpleQuery;
import uk.ac.ebi.uniprot.api.common.exception.InvalidRequestException;
import uk.ac.ebi.uniprot.api.common.repository.search.facet.FacetConfig;
import uk.ac.ebi.uniprot.api.common.repository.search.facet.FacetProperty;
import uk.ac.ebi.uniprot.common.Utils;

import java.util.List;
import java.util.regex.Pattern;

import static uk.ac.ebi.uniprot.api.common.repository.search.SolrRequestConverter.QueryConverter.getSimpleFacetQuery;
import static uk.ac.ebi.uniprot.api.common.repository.search.SolrRequestConverter.SolrQueryConverter.*;
import static uk.ac.ebi.uniprot.common.Utils.nonNull;
import static uk.ac.ebi.uniprot.common.Utils.nullOrEmpty;

/**
 * Created 14/06/19
 *
 * @author Edd
 */
public class SolrRequestConverter {
    /**
     * Creates a {@link SolrQuery} from a {@link SolrRequest}.
     * @param request the request that specifies the query
     * @return the solr query
     */
    public SolrQuery toSolrQuery(SolrRequest request) {
        SolrQuery solrQuery = new SolrQuery(request.getQuery());

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

        return solrQuery;
    }

    /**
     * Creates a Spring {@link Query} from a {@link SolrRequest}. Note that this does not
     * handle term queries, which are not supported by Spring's standard Query API.
     * And it also does not support Interval Facets.
     *
     * @param request the request that specifies the query
     * @return the query
     */
    public Query toQuery(SolrRequest request) {
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

            for (String facetName : request.getFacets()) {
                FacetProperty facetProperty = request.getFacetConfig().getFacetPropertyMap().get(facetName);
                if (Utils.notEmpty(facetProperty.getInterval())) {
                    throw new UnsupportedOperationException("Interval facets are not supported by Spring Data Solr");
                }
            }
            FacetOptions facetOptions = new FacetOptions();
            facetOptions.addFacetOnFlieldnames(request.getFacets());
            facetOptions.setFacetMinCount(request.getFacetConfig().getMincount());
            facetOptions.setFacetLimit(request.getFacetConfig().getLimit());
            simpleFacetQuery.setFacetOptions(facetOptions);

            return simpleFacetQuery;
        }
    }

    static class SolrQueryConverter {
        private static final String QUERY_OPERATOR = "q.op";
        private static final Pattern SINGLE_TERM_PATTERN = Pattern.compile("^\\w+$");
        private static final String TERMS_LIST = "terms.list";
        private static final String TERMS = "terms";
        private static final String DISTRIB = "distrib";
        private static final String TERMS_FIELDS = "terms.fl";
        private static final String MINCOUNT = "terms.mincount";

        static void setTermFields(SolrQuery solrQuery, String termQuery, List<String> termFields) {
            if (isSingleTerm(termQuery)) {
                solrQuery.setParam(TERMS_LIST, termQuery.toLowerCase());
            } else {
                throw new InvalidRequestException("Term information will only be returned for single value searches that do not specify a field.");
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
                if (Utils.notEmpty(facetProperty.getInterval())) {
                    String[] facetIntervals = facetProperty.getInterval().values().toArray(new String[0]);
                    solrQuery.addIntervalFacets(facetName, facetIntervals);
                } else {
                    solrQuery.addFacetField(facetName);
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
            if (nonNull(sort)) {
                for (Sort.Order order : sort) {
                    solrQuery
                            .addSort(order.getProperty(), order.isAscending() ? SolrQuery.ORDER.asc : SolrQuery.ORDER.desc);
                }
            }
        }
    }
}
