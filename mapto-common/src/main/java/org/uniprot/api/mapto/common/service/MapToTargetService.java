package org.uniprot.api.mapto.common.service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import org.uniprot.api.common.repository.search.ProblemPair;
import org.uniprot.api.common.repository.search.QueryResult;
import org.uniprot.api.common.repository.search.facet.Facet;
import org.uniprot.api.common.repository.search.facet.FacetConfig;
import org.uniprot.api.common.repository.search.facet.SolrStreamFacetResponse;
import org.uniprot.api.common.repository.search.page.impl.CursorPage;
import org.uniprot.api.common.repository.solrstream.FacetTupleStreamTemplate;
import org.uniprot.api.common.repository.stream.store.StoreRequest;
import org.uniprot.api.common.repository.stream.store.StoreStreamer;
import org.uniprot.api.idmapping.common.service.AbstractIdService;
import org.uniprot.api.rest.request.SearchRequest;
import org.uniprot.api.rest.request.StreamRequest;
import org.uniprot.api.rest.service.request.RequestConverter;

public abstract class MapToTargetService<T> extends AbstractIdService<T> {
    private final StoreStreamer<T> storeStreamer;

    protected MapToTargetService(
            StoreStreamer<T> storeStreamer,
            FacetTupleStreamTemplate tupleStream,
            FacetConfig facetConfig,
            RequestConverter requestConverter) {
        super(storeStreamer, tupleStream, facetConfig, requestConverter);
        this.storeStreamer = storeStreamer;
    }

    public QueryResult<T> getMappedEntries(SearchRequest searchRequest, List<String> ids) {
        List<Facet> facets = null;
        validateMappedIdsEnrichmentLimit(ids.size());
        List<ProblemPair> warnings = new ArrayList<>();
        if (solrSearchNeededBySearchRequest(searchRequest, false)) {
            // unset facets if mapped to ids exceeds the allowed limit
            // and set the warning
            if (facetingDisallowed(searchRequest, ids)) {
                warnings.add(removeFacetsAndGetFacetWarning(searchRequest));
            }
            SolrStreamFacetResponse solrStreamResponse =
                    searchMappedIdsFacetsBySearchRequest(searchRequest, ids);

            facets = solrStreamResponse.getFacets();

            ids = solrStreamResponse.getIds();
        }

        // compute the cursor and get subset of accessions as per cursor
        int pageSize = getPageSize(searchRequest);
        CursorPage cursor = CursorPage.of(searchRequest.getCursor(), pageSize, ids.size());
        List<String> idsInPage =
                ids.subList(cursor.getOffset().intValue(), CursorPage.getNextOffset(cursor));
        // get entries from voldemort
        Stream<T> result = getEntries(idsInPage, searchRequest.getFields());
        return QueryResult.<T>builder()
                .content(result)
                .page(cursor)
                .facets(facets)
                .warnings(warnings)
                .build();
    }

    public Stream<T> streamEntries(StreamRequest streamRequest, List<String> ids) {
        List<String> filterAndSortEntries = streamFilterAndSortEntries(streamRequest, ids);
        StoreRequest storeRequest =
                StoreRequest.builder().fields(streamRequest.getFields()).build();
        return this.storeStreamer.streamEntries(filterAndSortEntries, storeRequest);
    }

    protected List<String> streamFilterAndSortEntries(
            StreamRequest streamRequest, List<String> toIds) {
        if (solrSearchNeededByStreamRequest(streamRequest, false)) {
            SolrStreamFacetResponse solrStreamResponse =
                    searchMappedIdsFacetsByStreamRequest(streamRequest, toIds);
            return solrStreamResponse.getIds();
        }
        return toIds;
    }
}
