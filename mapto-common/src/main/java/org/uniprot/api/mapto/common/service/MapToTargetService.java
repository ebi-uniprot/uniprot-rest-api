package org.uniprot.api.mapto.common.service;

import org.uniprot.api.common.repository.search.ProblemPair;
import org.uniprot.api.common.repository.search.QueryResult;
import org.uniprot.api.common.repository.search.facet.Facet;
import org.uniprot.api.common.repository.search.facet.FacetConfig;
import org.uniprot.api.common.repository.search.facet.SolrStreamFacetResponse;
import org.uniprot.api.common.repository.search.page.impl.CursorPage;
import org.uniprot.api.common.repository.solrstream.FacetTupleStreamTemplate;
import org.uniprot.api.common.repository.stream.rdf.RdfStreamer;
import org.uniprot.api.common.repository.stream.store.StoreRequest;
import org.uniprot.api.common.repository.stream.store.StoreStreamer;
import org.uniprot.api.idmapping.common.service.AbstractIdService;
import org.uniprot.api.mapto.common.model.MapToJob;
import org.uniprot.api.rest.request.SearchRequest;
import org.uniprot.api.rest.request.StreamRequest;
import org.uniprot.api.rest.service.request.RequestConverter;
import org.uniprot.api.uniref.common.service.light.request.UniRefStreamRequest;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

public abstract class MapToTargetService<T> extends AbstractIdService<T> {
    private final MapToJobService mapToJobService;
    private final RdfStreamer rdfStreamer;

    protected MapToTargetService(
            StoreStreamer<T> storeStreamer,
            FacetTupleStreamTemplate tupleStream,
            FacetConfig facetConfig,
            RequestConverter requestConverter, MapToJobService mapToJobService, RdfStreamer rdfStreamer) {
        super(storeStreamer, tupleStream, facetConfig, requestConverter);
        this.mapToJobService = mapToJobService;
        this.rdfStreamer = rdfStreamer;
    }

    public QueryResult<T> getMappedEntries(String jobId, SearchRequest searchRequest) {
        MapToJob mapToJob = mapToJobService.findMapToJob(jobId);
        return getMappedEntries(searchRequest, mapToJob.getTargetIds());
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

    public Stream<T> streamEntries(String jobId, StreamRequest streamRequest) {
        MapToJob mapToJob = mapToJobService.findMapToJob(jobId);
        return streamEntries(streamRequest, mapToJob.getTargetIds());
    }

    public Stream<String> streamRdf(
            String jobId, UniRefStreamRequest streamRequest, String dataType, String format) {
        List<String> entryIds = streamFilterAndSortEntries(streamRequest, mapToJobService.findMapToJob(jobId).getTargetIds());
        return rdfStreamer.stream(entryIds.stream(), dataType, format);
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
