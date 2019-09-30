package org.uniprot.api.common.repository.search;

import java.util.ArrayList;
import java.util.List;

import lombok.Builder;
import lombok.Data;
import lombok.Singular;

import org.springframework.data.domain.Sort;
import org.springframework.data.solr.core.query.Query;
import org.uniprot.api.common.repository.search.facet.FacetConfig;

/**
 * Represents a request object containing the details to create a query to send to Solr.
 *
 * @author Edd
 */
@Data
@Builder(builderClassName = "SolrRequestBuilder")
public class SolrRequest {
    private static final Query.Operator DEFAULT_OPERATOR = Query.Operator.AND;

    private String query;
    private Query.Operator defaultQueryOperator;
    private Sort sort;
    private FacetConfig facetConfig;
    private String termQuery;
    private QueryBoosts queryBoosts;
    private String defaultField;

    @Singular private List<String> termFields = new ArrayList<>();
    @Singular private List<String> filterQueries = new ArrayList<>();
    @Singular private List<String> facets = new ArrayList<>();

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
