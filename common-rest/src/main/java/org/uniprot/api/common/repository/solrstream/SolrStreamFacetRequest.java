package org.uniprot.api.common.repository.solrstream;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import lombok.Builder;
import lombok.Getter;

import org.apache.solr.client.solrj.SolrQuery;
import org.uniprot.api.common.repository.search.SolrQueryConfig;
import org.uniprot.api.rest.request.SearchRequest;
import org.uniprot.api.rest.search.SortUtils;
import org.uniprot.core.util.Utils;
import org.uniprot.store.config.UniProtDataType;

/**
 * Represents a request object containing the details to create a Solr streaming expressions for a
 * facets function call and/or search function
 *
 * @author sahmad
 */
@Getter
public class SolrStreamFacetRequest {
    private static final Integer BUCKET_SIZE = 1000;
    private static final String BUCKET_SORTS = "count(*) desc";
    private static final String DEFAULT_METRICS = "count(*)";
    private String query;
    private List<String> facets;
    private String bucketSorts; // comma separated list of sorts
    private String metrics; // comma separated list of metrics to compute for buckets
    private Integer bucketSizeLimit; // the number of facets/buckets
    private boolean searchAccession;
    // fields related to search function. we need this when user wants to filter by facet value(s)
    private String searchFieldList = "accession_id";
    private String searchSort = "accession_id asc";
    private String requestHandler = "/export";
    private String filteredQuery;
    private SolrQueryConfig queryConfig;

    @Builder
    SolrStreamFacetRequest(
            String query,
            List<String> facets,
            boolean searchAccession,
            String searchSort,
            String searchFieldList,
            String filteredQuery,
            SolrQueryConfig queryConfig,
            Integer bucketSizeLimit,
            String metrics,
            String bucketSorts) {
        this.query = query;
        this.facets = facets;
        this.searchAccession = searchAccession;
        this.filteredQuery = filteredQuery;
        this.queryConfig = queryConfig;
        if (Utils.notNullNotEmpty(searchSort)) {
            this.searchSort = searchSort;
        }

        if (Utils.notNullNotEmpty(searchFieldList)) {
            this.searchFieldList = searchFieldList;
        }

        if (Objects.isNull(bucketSizeLimit)) {
            this.bucketSizeLimit = BUCKET_SIZE;
        } else {
            this.bucketSizeLimit = bucketSizeLimit;
        }

        if (Utils.nullOrEmpty(metrics)) {
            this.metrics = DEFAULT_METRICS;
        } else {
            this.metrics = metrics;
        }

        if (Utils.nullOrEmpty(bucketSorts)) {
            this.bucketSorts = BUCKET_SORTS;
        } else {
            this.bucketSorts = bucketSorts;
        }
    }

    public static SolrStreamFacetRequest createSolrStreamFacetRequest(
            SolrQueryConfig solrQueryConfig,
            UniProtDataType uniProtDataType,
            String solrIdField,
            String termsQueryField,
            List<String> ids,
            SearchRequest searchRequest,
            boolean includeIsoform) {

        SolrStreamFacetRequest.SolrStreamFacetRequestBuilder solrRequestBuilder =
                SolrStreamFacetRequest.builder();

        // construct the query for tuple stream
        StringBuilder qb = new StringBuilder();
        qb.append("({!terms f=")
                .append(termsQueryField)
                .append("}")
                .append(String.join(",", ids))
                .append(")");
        String termQuery = qb.toString();

        // append the facet filter query in the accession query
        if (Utils.notNullNotEmpty(searchRequest.getQuery())) {
            solrRequestBuilder.query(searchRequest.getQuery());
            solrRequestBuilder.filteredQuery(termQuery);
            solrRequestBuilder.searchAccession(Boolean.TRUE);
            solrRequestBuilder.searchSort(solrIdField + " asc");
            solrRequestBuilder.searchFieldList(solrIdField);
        } else {
            solrRequestBuilder.query(termQuery);
        }

        if (Utils.notNullNotEmpty(searchRequest.getSort())) {
            List<SolrQuery.SortClause> sort =
                    SortUtils.parseSortClause(uniProtDataType, searchRequest.getSort());
            solrRequestBuilder.searchSort(constructSortQuery(sort));
            solrRequestBuilder.searchFieldList(constructFieldList(solrIdField, sort));
            solrRequestBuilder.searchAccession(Boolean.TRUE);
        }

        if (includeIsoform) {
            solrRequestBuilder.searchAccession(Boolean.TRUE);
        }

        List<String> facets = searchRequest.getFacetList();

        return solrRequestBuilder.queryConfig(solrQueryConfig).facets(facets).build();
    }

    private static String constructSortQuery(List<SolrQuery.SortClause> sort) {
        return sort.stream()
                .map(clause -> clause.getItem() + " " + clause.getOrder().name())
                .collect(Collectors.joining(","));
    }

    private static String constructFieldList(String solrIdField, List<SolrQuery.SortClause> sort) {
        Set<String> fieldList =
                sort.stream().map(SolrQuery.SortClause::getItem).collect(Collectors.toSet());
        fieldList.add(solrIdField);
        return String.join(",", fieldList);
    }
}
