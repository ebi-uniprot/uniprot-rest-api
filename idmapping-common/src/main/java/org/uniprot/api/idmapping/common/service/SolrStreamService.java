package org.uniprot.api.idmapping.common.service;

import java.util.ArrayList;
import java.util.List;

import org.apache.solr.client.solrj.io.stream.TupleStream;
import org.uniprot.api.common.repository.search.SolrFacetRequest;
import org.uniprot.api.common.repository.search.SolrRequest;
import org.uniprot.api.common.repository.search.facet.FacetTupleStreamConverter;
import org.uniprot.api.common.repository.search.facet.SolrStreamFacetResponse;
import org.uniprot.api.common.repository.solrstream.FacetTupleStreamTemplate;

public class SolrStreamService {

    private final FacetTupleStreamTemplate tupleStream;
    private final FacetTupleStreamConverter facetTupleStreamConverter;

    public SolrStreamService(
            FacetTupleStreamTemplate tupleStream,
            FacetTupleStreamConverter facetTupleStreamConverter) {
        this.tupleStream = tupleStream;
        this.facetTupleStreamConverter = facetTupleStreamConverter;
    }

    public SolrStreamFacetResponse searchBySolrStream(SolrRequest solrRequest, int batchSize) {
        // request without facets. Normally search and facets requests are mutually exclusive.
        SolrRequest searchRequest = solrRequest.createSearchRequest();
        TupleStream tupleStream = this.tupleStream.create(searchRequest);
        SolrStreamFacetResponse idsResponse =
                this.facetTupleStreamConverter.convert(tupleStream, List.of());
        // get facets in batches
        List<SolrStreamFacetResponse> facetsInBatches = getFacetsInBatches(solrRequest, batchSize);
        // merge them
        SolrStreamFacetResponse mergedResponse =
                SolrStreamFacetResponse.merge(solrRequest, facetsInBatches, idsResponse);
        return mergedResponse;
    }

    private List<SolrStreamFacetResponse> getFacetsInBatches(
            SolrRequest solrRequest, int idBatchSize) {
        List<String> ids = solrRequest.getIds();
        List<SolrStreamFacetResponse> facetResponses =
                new ArrayList<>(ids.size() / idBatchSize + 1);
        boolean ignoreLimit =
                true; // for batch faceting, get all facets and return "limit" number of facets
        // during merge
        for (int i = 0; solrRequest.getFacets().size() > 0 && i < ids.size(); i += idBatchSize) {
            List<String> idsBatch = ids.subList(i, Math.min(i + idBatchSize, ids.size()));
            SolrRequest solrFacetRequest = solrRequest.createBatchFacetSolrRequest(idsBatch);
            TupleStream facetTupleStream = this.tupleStream.create(solrFacetRequest);
            SolrStreamFacetResponse response =
                    this.facetTupleStreamConverter.convert(
                            facetTupleStream,
                            solrFacetRequest.getFacets().stream()
                                    .map(SolrFacetRequest::getName)
                                    .toList(),
                            ignoreLimit);
            facetResponses.add(response);
        }
        return facetResponses;
    }
}
