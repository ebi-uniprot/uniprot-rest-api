package uk.ac.ebi.uniprot.api.common.repository.store;

import lombok.Builder;
import lombok.Data;
import lombok.Singular;
import org.apache.solr.client.solrj.SolrQuery;
import org.springframework.data.domain.Sort;
import org.springframework.data.solr.core.query.FacetOptions;
import org.springframework.data.solr.core.query.Query;
import org.springframework.data.solr.core.query.SimpleFacetQuery;
import org.springframework.data.solr.core.query.SimpleQuery;
import uk.ac.ebi.uniprot.api.common.repository.search.QueryRetrievalException;
import uk.ac.ebi.uniprot.api.common.repository.search.facet.GenericFacetConfig;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import static uk.ac.ebi.uniprot.common.Utils.nonNull;

/**
 * Represents the request object for download stream request
 *
 * @author lgonzales
 */
@Data
@Builder(builderClassName = "SolrRequestBuilder")
public class SolrRequest {
    private static final Query.Operator DEFAULT_OPERATOR = Query.Operator.AND;
    private static final String QUERY_OPERATOR = "q.op";
    private static final Pattern SINGLE_TERM_PATTERN = Pattern.compile("^\\w+$");
    private static final String TERMS_LIST = "terms.list";
    private static final String TERMS = "terms";
    private static final String DISTRIB = "distrib";
    private static final String TERMS_FIELDS = "terms.fl";

    private String query;
    private Query.Operator defaultQueryOperator;
    private Sort sort;
    private GenericFacetConfig facetConfig;

    @Singular
    private List<String> termFields = new ArrayList<>();
    @Singular
    private List<String> filterQueries = new ArrayList<>();
    @Singular
    private List<String> facets = new ArrayList<>();

    // TODO: 13/06/19 refactor this class to that toSolrQuery and toQuery are in separate classes: SolrQueryConverter and QueryConverter
    public SolrQuery toSolrQuery() {
        SolrQuery solrQuery = new SolrQuery(query);

        setFilterQueries(solrQuery);
        setSort(solrQuery);
        setQueryOperator(solrQuery);
        setFacets(solrQuery);
        setTermFields(solrQuery);

        return solrQuery;
    }

    public Query toQuery() {
        SimpleQuery simpleQuery = new SimpleQuery(this.query);

        if (!facets.isEmpty()) {
            simpleQuery = getSimpleFacetQuery(simpleQuery);
        }

        filterQueries.stream().map(SimpleQuery::new).forEach(simpleQuery::addFilterQuery);

        simpleQuery.addSort(this.sort);
        simpleQuery.setDefaultOperator(defaultQueryOperator);

        return simpleQuery;
    }

    private void setTermFields(SolrQuery solrQuery) {
        if (isSingleTerm(query)) {
            solrQuery.setParam(TERMS_LIST, query);
        } else {
            throw new QueryRetrievalException("Term information will only be returned for single value searches that do not specify a field.");
        }

        solrQuery.setParam(TERMS, "true");
        solrQuery.setParam(DISTRIB, "true");
        termFields.forEach(termField -> solrQuery.setParam(TERMS_FIELDS, termField));
    }

    private boolean isSingleTerm(String query) {
        return SINGLE_TERM_PATTERN.matcher(query).matches();
    }

    private SimpleFacetQuery getSimpleFacetQuery(SimpleQuery simpleQuery) {
        SimpleFacetQuery simpleFacetQuery = new SimpleFacetQuery(simpleQuery.getCriteria());

        FacetOptions facetOptions = new FacetOptions();
        facetOptions.addFacetOnFlieldnames(facets);
        facetOptions.setFacetMinCount(facetConfig.getMincount());
        facetOptions.setFacetLimit(facetConfig.getLimit());
        simpleFacetQuery.setFacetOptions(facetOptions);

        return simpleFacetQuery;
    }

    private void setFacets(SolrQuery solrQuery) {
        solrQuery.setFacet(true);

        String[] facetArr = new String[facets.size()];
        facetArr = facets.toArray(facetArr);
        solrQuery.addFacetField(facetArr);

        solrQuery.setFacetLimit(facetConfig.getLimit());
        solrQuery.setFacetMinCount(facetConfig.getMincount());
    }

    private void setQueryOperator(SolrQuery solrQuery) {
        solrQuery.set(QUERY_OPERATOR, defaultQueryOperator.asQueryStringRepresentation());
    }

    private void setFilterQueries(SolrQuery solrQuery) {
        String[] filterQueryArr = new String[filterQueries.size()];
        filterQueryArr = filterQueries.toArray(filterQueryArr);
        solrQuery.setFilterQueries(filterQueryArr);
    }

    private void setSort(SolrQuery solrQuery) {
        if (nonNull(sort)) {
            for (Sort.Order order : sort) {
                solrQuery
                        .addSort(order.getProperty(), order.isAscending() ? SolrQuery.ORDER.asc : SolrQuery.ORDER.desc);
            }
        }
    }

    // setting default field values in a builder following instructions here:
    // https://www.baeldung.com/lombok-builder-default-value
    public static class SolrRequestBuilder {
        private Query.Operator defaultQueryOperator = DEFAULT_OPERATOR;
        private Sort sort;

        public SolrRequestBuilder addSort(Sort sort) {
            if (this.sort == null) {
                this.sort = sort;
            } else {
                this.sort.and(sort);
            }
            return this;
        }
    }
}
