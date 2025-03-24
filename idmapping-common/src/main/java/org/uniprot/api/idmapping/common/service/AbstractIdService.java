package org.uniprot.api.idmapping.common.service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import org.apache.solr.client.solrj.io.stream.TupleStream;
import org.springframework.beans.factory.annotation.Value;
import org.uniprot.api.common.exception.InvalidRequestException;
import org.uniprot.api.common.repository.search.ProblemPair;
import org.uniprot.api.common.repository.search.SolrFacetRequest;
import org.uniprot.api.common.repository.search.SolrRequest;
import org.uniprot.api.common.repository.search.facet.FacetConfig;
import org.uniprot.api.common.repository.search.facet.FacetTupleStreamConverter;
import org.uniprot.api.common.repository.search.facet.SolrStreamFacetResponse;
import org.uniprot.api.common.repository.solrstream.FacetTupleStreamTemplate;
import org.uniprot.api.common.repository.stream.store.StoreRequest;
import org.uniprot.api.common.repository.stream.store.StoreStreamer;
import org.uniprot.api.rest.output.PredefinedAPIStatus;
import org.uniprot.api.rest.request.SearchRequest;
import org.uniprot.api.rest.request.StreamRequest;
import org.uniprot.api.rest.service.request.RequestConverter;
import org.uniprot.core.util.Utils;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract class AbstractIdService<T> {
    protected final StoreStreamer<T> storeStreamer;
    private final FacetTupleStreamTemplate tupleStreamTemplate;
    private final FacetTupleStreamConverter facetTupleStreamConverter;
    private final RequestConverter requestConverter;

    @Value("${search.request.converter.defaultRestPageSize:#{null}}")
    private Integer defaultPageSize;
    // the maximum number of ids allowed in `to` field after mapped by `from` fields
    @Value("${id.mapping.max.to.ids.count:#{null}}") // value to 500k
    private Integer maxIdMappingToIdsCount;

    // Maximum number of `to` ids supported to enrich result with uniprot data
    // Greater than maxIdMappingToIdsCountEnriched and less than maxIdMappingToIdsCount, the API
    // should return only `to` ids
    @Value("${id.mapping.max.to.ids.enrich.count:#{null}}") // value to 100k
    private Integer maxIdMappingToIdsCountEnriched;
    // Maximum number of `to` ids supported with faceting query
    @Value("${id.mapping.max.to.ids.with.facets.count:#{null}}") // value to 10k
    private Integer maxIdMappingToIdsCountWithFacets;

    // config related to faceting
    @Value("${id.mapping.facet.ids.batch.size:5000}")
    private int idBatchSize;

    public AbstractIdService(
            StoreStreamer<T> storeStreamer,
            FacetTupleStreamTemplate tupleStreamTemplate,
            FacetConfig facetConfig,
            RequestConverter requestConverter) {
        this.storeStreamer = storeStreamer;
        this.tupleStreamTemplate = tupleStreamTemplate;
        this.facetTupleStreamConverter =
                new FacetTupleStreamConverter(getSolrIdField(), facetConfig);
        this.requestConverter = requestConverter;
    }

    public void validateMappedIdsEnrichmentLimit(int mappedIdsSize) {
        if (mappedIdsSize > this.maxIdMappingToIdsCountEnriched) {
            throw new InvalidRequestException(
                    PredefinedAPIStatus.ENRICHMENT_WARNING.getErrorMessage(
                            this.maxIdMappingToIdsCountEnriched));
        }
    }

    protected SolrStreamFacetResponse searchMappedIdsFacetsBySearchRequest(
            SearchRequest searchRequest, List<String> toIds) {
        SolrRequest solrRequest =
                this.requestConverter.createSearchIdsSolrRequest(
                        searchRequest, toIds, getSolrIdField());
        SolrStreamFacetResponse solrStreamResponse =
                searchBySolrStream(solrRequest, this.idBatchSize);
        return solrStreamResponse;
    }

    protected SolrStreamFacetResponse searchMappedIdsFacetsByStreamRequest(
            StreamRequest streamRequest, List<String> toIds) {
        SolrRequest solrRequest =
                requestConverter.createStreamIdsSolrRequest(streamRequest, toIds, getSolrIdField());
        SolrStreamFacetResponse solrStreamResponse =
                searchBySolrStream(solrRequest, this.idBatchSize);
        return solrStreamResponse;
    }

    protected Stream<T> getEntries(List<String> toIds, String fields) {
        StoreRequest storeRequest = StoreRequest.builder().fields(fields).build();
        return this.storeStreamer.streamEntries(toIds, storeRequest);
    }

    protected SolrStreamFacetResponse searchBySolrStream(SolrRequest solrRequest, int batchSize) {
        // request without facets. Normally search and facets requests are mutually exclusive.
        SolrRequest searchRequest = solrRequest.createSearchRequest();
        TupleStream tupleStream = this.tupleStreamTemplate.create(searchRequest);
        SolrStreamFacetResponse idsResponse =
                this.facetTupleStreamConverter.convert(tupleStream, List.of());
        // get facets in batches
        List<SolrStreamFacetResponse> facetsInBatches = getFacetsInBatches(solrRequest, batchSize);
        // merge them
        SolrStreamFacetResponse mergedResponse =
                SolrStreamFacetResponse.merge(solrRequest, facetsInBatches, idsResponse);
        return mergedResponse;
    }

    protected abstract String getSolrIdField();

    protected static boolean solrSearchNeededByStreamRequest(
            StreamRequest streamRequest, boolean includeIsoform) {
        return Utils.notNull(streamRequest.getQuery())
                || Utils.notNull(streamRequest.getSort())
                || includeIsoform;
    }

    protected boolean solrSearchNeededBySearchRequest(
            SearchRequest searchRequest, boolean includeIsoform) {
        return Utils.notNullNotEmpty(searchRequest.getQuery())
                || Utils.notNullNotEmpty(searchRequest.getFacets())
                || Utils.notNullNotEmpty(searchRequest.getSort())
                || includeIsoform;
    }

    protected boolean facetingDisallowed(SearchRequest searchRequest, List<String> mappedIds) {
        return Utils.notNullNotEmpty(searchRequest.getFacets())
                && mappedIds.size() > this.maxIdMappingToIdsCountWithFacets;
    }

    protected ProblemPair removeFacetsAndGetFacetWarning(SearchRequest searchRequest) {
        searchRequest.removeFacets();
        return new ProblemPair(
                PredefinedAPIStatus.FACET_WARNING.getCode(),
                PredefinedAPIStatus.FACET_WARNING.getErrorMessage(
                        this.maxIdMappingToIdsCountWithFacets));
    }

    protected Integer getPageSize(SearchRequest searchRequest) {
        Integer pageSize = this.defaultPageSize;
        if (Utils.notNull(searchRequest.getSize())) {
            pageSize = searchRequest.getSize();
        }
        return pageSize;
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
            TupleStream facetTupleStream = this.tupleStreamTemplate.create(solrFacetRequest);
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
