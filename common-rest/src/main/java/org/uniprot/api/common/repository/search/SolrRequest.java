package org.uniprot.api.common.repository.search;

import java.util.ArrayList;
import java.util.List;

import lombok.*;

import org.apache.solr.client.solrj.SolrQuery;
import org.uniprot.api.common.repository.search.facet.FacetConfig;

/**
 * Represents a request object containing the details to create a query to send to Solr.
 *
 * @author Edd
 */
@Data
@Builder(builderClassName = "SolrRequestBuilder", toBuilder = true)
public class SolrRequest {
    public static final QueryOperator DEFAULT_OPERATOR = QueryOperator.AND;
    private String query;

    @Setter(AccessLevel.NONE)
    private QueryOperator defaultQueryOperator;

    private FacetConfig facetConfig;
    private String termQuery;
    private SolrQueryConfig queryConfig;
    private String defaultField;
    // Batch size of rows in solr request. In case of search api request rows and totalRows will be
    // same.
    private int rows;
    // Total rows requested by user
    private int totalRows;

    @Singular private List<String> termFields = new ArrayList<>();
    @Singular private List<String> filterQueries = new ArrayList<>();
    @Singular private List<String> facets = new ArrayList<>();
    @Singular private List<SolrQuery.SortClause> sorts = new ArrayList<>();

    // setting default field values in a builder following instructions here:
    // https://www.baeldung.com/lombok-builder-default-value
    public static class SolrRequestBuilder {
        private QueryOperator defaultQueryOperator = DEFAULT_OPERATOR;
    }
}
