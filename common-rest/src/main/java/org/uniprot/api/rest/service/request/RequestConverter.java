package org.uniprot.api.rest.service.request;

import org.uniprot.api.common.repository.search.SolrRequest;
import org.uniprot.api.rest.request.SearchRequest;
import org.uniprot.api.rest.request.StreamRequest;

/**
 * This interface is responsible to convert REST requests into SolrRequest. SolrRequests acts as a
 * DTO that are used to construct solr requests in StoreStreamer or search in SolrRepository
 */
public interface RequestConverter {

    SolrRequest createSearchSolrRequest(SearchRequest request);

    SolrRequest createStreamSolrRequest(StreamRequest request);

    int getDefaultPageSize();
}
