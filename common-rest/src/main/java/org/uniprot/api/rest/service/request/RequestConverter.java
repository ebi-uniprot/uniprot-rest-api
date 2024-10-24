package org.uniprot.api.rest.service.request;

import org.uniprot.api.common.repository.search.SolrRequest;
import org.uniprot.api.rest.request.SearchRequest;
import org.uniprot.api.rest.request.StreamRequest;

public interface RequestConverter {

    SolrRequest createSearchSolrRequest(SearchRequest request);

    SolrRequest createStreamSolrRequest(StreamRequest request);

    int getDefaultPageSize();
}
