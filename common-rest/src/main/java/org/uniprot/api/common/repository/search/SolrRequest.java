package org.uniprot.api.common.repository.search;

import java.util.ArrayList;
import java.util.List;

import org.apache.solr.client.solrj.SolrQuery;

import lombok.*;

/**
 * Represents a request object containing the details to create a query to send to Solr.
 *
 * @author Edd
 */
@Getter
@AllArgsConstructor
@Builder(builderClassName = "SolrRequestBuilder", toBuilder = true)
public class SolrRequest {
    public static final QueryOperator DEFAULT_OPERATOR = QueryOperator.AND;
    private String query;

    private QueryOperator defaultQueryOperator;

    private String termQuery;

    private String queryField;

    private String defaultField;

    private String idField;
    private String idsQuery;
    // Batch size of rows in solr request. In case of search api request rows and totalRows will be
    // same.
    private int rows;
    // Total rows requested by user
    private int totalRows;

    private boolean largeSolrStreamRestricted = true;
    private String highlightFields;

    private String boostFunctions;
    @Singular private List<String> fieldBoosts = new ArrayList<>();
    @Singular private List<String> staticBoosts = new ArrayList<>();

    @Singular private List<String> termFields = new ArrayList<>();
    @Singular private List<String> filterQueries = new ArrayList<>();
    @Singular private List<SolrFacetRequest> facets = new ArrayList<>();
    @Singular private List<SolrQuery.SortClause> sorts = new ArrayList<>();

    // setting default field values in a builder following instructions here:
    // https://www.baeldung.com/lombok-builder-default-value
    public static class SolrRequestBuilder {
        private QueryOperator defaultQueryOperator = DEFAULT_OPERATOR;

        public String getQuery() {
            return query;
        }
    }

    // TODO add a method to create solrRequest with batch of ids and other info like facet, length, sort etc
    // createBatchFacetSolrRequest

    @Override
    public String toString() {
        return "SolrRequest{"
                + "query='"
                + query
                + '\''
                + ", termQuery='"
                + termQuery
                + '\''
                + ", queryField='"
                + queryField
                + '\''
                + ", defaultField='"
                + defaultField
                + '\''
                + ", idField='"
                + idField
                + '\''
                + ", idsQuery='"
                + idsQuery
                + '\''
                + ", rows="
                + rows
                + ", totalRows="
                + totalRows
                + ", termFields="
                + termFields
                + ", filterQueries="
                + filterQueries
                + ", facets="
                + facets
                + ", sorts="
                + sorts
                + '}';
    }
}
