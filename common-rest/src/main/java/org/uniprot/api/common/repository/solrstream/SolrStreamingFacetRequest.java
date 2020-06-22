package org.uniprot.api.common.repository.solrstream;

import java.util.List;

import lombok.Builder;
import lombok.Data;

/**
 * Represents a request object containing the details to create a Solr streaming expressions.
 *
 * @author sahmad
 */
@Data
@Builder(builderClassName = "SolrStreamingFacetRequestBuilder", toBuilder = true)
public class SolrStreamingFacetRequest {
    private String query;
    private List<String> facets;
    private String bucketSorts; // comma separated list of sorts
    private String metrics; // comma separated list of metrics to compute for buckets
    private Integer bucketSizeLimit; // the number of facets/buckets

    // default values
    public static class SolrStreamingFacetRequestBuilder {
        private String bucketSorts = "count(*) desc";
        private Integer bucketSizeLimit = 1000;
        private String metrics = "count(*)";
    }
}
