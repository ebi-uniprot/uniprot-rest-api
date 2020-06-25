package org.uniprot.api.common.repository.solrstream;

import java.util.List;

import lombok.Builder;
import lombok.Getter;

/**
 * Represents a request object containing the details to create a Solr streaming expressions for
 * facet function call
 *
 * @author sahmad
 */
@Getter
public class SolrStreamingFacetRequest {
    private static final Integer BUCKET_SIZE = 1000;
    private static final String BUCKET_SORTS = "count(*) desc";
    private static final String METRICS = "count(*)";
    private String query;
    private List<String> facets;
    private String bucketSorts; // comma separated list of sorts
    private String metrics; // comma separated list of metrics to compute for buckets
    private Integer bucketSizeLimit; // the number of facets/buckets

    @Builder
    SolrStreamingFacetRequest(String query, List<String> facets) {
        this.query = query;
        this.facets = facets;
        this.bucketSizeLimit = BUCKET_SIZE;
        this.bucketSorts = BUCKET_SORTS;
        this.metrics = METRICS;
    }
}
