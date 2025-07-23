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
import org.uniprot.api.common.repository.stream.rdf.RdfStreamer;
import org.uniprot.api.common.repository.stream.store.StoreRequest;
import org.uniprot.api.common.repository.stream.store.StoreStreamer;
import org.uniprot.api.idmapping.common.service.AbstractIdService;
import org.uniprot.api.mapto.common.model.MapToJob;
import org.uniprot.api.rest.request.SearchRequest;
import org.uniprot.api.rest.request.StreamRequest;
import org.uniprot.api.rest.service.request.RequestConverter;
import org.uniprot.api.uniref.common.service.light.request.UniRefStreamRequest;

public abstract class MapToTargetService<T> extends AbstractIdService<T> {
    private final MapToJobService mapToJobService;
    private final MapToResultService mapToResultService;
    private final RdfStreamer rdfStreamer;

    protected MapToTargetService(
            StoreStreamer<T> storeStreamer,
            FacetTupleStreamTemplate tupleStream,
            FacetConfig facetConfig,
            RequestConverter requestConverter,
            MapToJobService mapToJobService,
            MapToResultService mapToResultService,
            RdfStreamer rdfStreamer) {
        super(storeStreamer, tupleStream, facetConfig, requestConverter);
        this.mapToJobService = mapToJobService;
        this.mapToResultService = mapToResultService;
        this.rdfStreamer = rdfStreamer;
    }

    public QueryResult<T> getMappedEntries(String jobId, SearchRequest searchRequest) {
        MapToJob mapToJob = mapToJobService.findMapToJob(jobId);
        Long totalTargetIds = mapToJob.getTotalTargetIds();
        validateMappedIdsEnrichmentLimit(totalTargetIds);
        List<Facet> facets = null;
        List<ProblemPair> warnings = new ArrayList<>();
        int pageSize = getPageSize(searchRequest);
        CursorPage cursor = CursorPage.of(searchRequest.getCursor(), pageSize, totalTargetIds);
        List<String> idPage;

        if (solrSearchNeededBySearchRequest(searchRequest, false)) {
            List<String> targetIds = mapToResultService.findAllTargetIdsByMapToJob(mapToJob);
            // unset facets if mapped to ids exceeds the allowed limit
            // and set the warning
            if (facetingDisallowed(searchRequest, totalTargetIds)) {
                ProblemPair facetWarning = removeFacetsAndGetFacetWarning(searchRequest);
                warnings.add(facetWarning);
            }
            SolrStreamFacetResponse solrStreamResponse =
                    searchMappedIdsFacetsBySearchRequest(searchRequest, targetIds);

            totalTargetIds = (long) solrStreamResponse.getIds().size();
            cursor = CursorPage.of(searchRequest.getCursor(), pageSize, totalTargetIds);
            targetIds = solrStreamResponse.getIds();
            idPage =
                    targetIds.subList(
                            cursor.getOffset().intValue(), CursorPage.getNextOffset(cursor));
            facets = solrStreamResponse.getFacets();
        } else {
            // compute the cursor and get subset of accessions as per cursor
            idPage = mapToResultService.findTargetIdsByMapToJob(mapToJob, cursor);
        }
        // get entries from voldemort
        Stream<T> entries = getEntries(idPage, searchRequest.getFields());
        return QueryResult.<T>builder()
                .content(entries)
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
        List<String> allTargetIdsByMapToJob =
                mapToResultService.findAllTargetIdsByMapToJob(mapToJob);
        return streamEntries(streamRequest, allTargetIdsByMapToJob);
    }

    public Stream<String> streamRdf(
            String jobId, UniRefStreamRequest streamRequest, String dataType, String format) {
        MapToJob mapToJob = mapToJobService.findMapToJob(jobId);
        List<String> allTargetIdsByMapToJob =
                mapToResultService.findAllTargetIdsByMapToJob(mapToJob);
        List<String> entryIds = streamFilterAndSortEntries(streamRequest, allTargetIdsByMapToJob);
        return rdfStreamer.stream(entryIds, dataType, format);
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
