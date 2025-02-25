package org.uniprot.api.idmapping.common.service;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.solr.client.solrj.io.stream.TupleStream;
import org.springframework.beans.factory.annotation.Value;
import org.uniprot.api.common.exception.InvalidRequestException;
import org.uniprot.api.common.repository.search.*;
import org.uniprot.api.common.repository.search.facet.Facet;
import org.uniprot.api.common.repository.search.facet.FacetConfig;
import org.uniprot.api.common.repository.search.facet.FacetTupleStreamConverter;
import org.uniprot.api.common.repository.search.facet.SolrStreamFacetResponse;
import org.uniprot.api.common.repository.search.page.impl.CursorPage;
import org.uniprot.api.common.repository.solrstream.FacetTupleStreamTemplate;
import org.uniprot.api.common.repository.stream.rdf.RdfStreamer;
import org.uniprot.api.common.repository.stream.store.StoreRequest;
import org.uniprot.api.common.repository.stream.store.StoreStreamer;
import org.uniprot.api.idmapping.common.model.IdMappingResult;
import org.uniprot.api.idmapping.common.response.model.IdMappingStringPair;
import org.uniprot.api.rest.output.PredefinedAPIStatus;
import org.uniprot.api.rest.request.SearchRequest;
import org.uniprot.api.rest.request.StreamRequest;
import org.uniprot.api.rest.service.request.RequestConverter;
import org.uniprot.core.util.Utils;
import org.uniprot.store.config.UniProtDataType;

import lombok.extern.slf4j.Slf4j;

/**
 * @author sahmad
 * @created 16/02/2021
 */
@Slf4j
public abstract class BasicIdService<T, U> {
    protected final StoreStreamer<T> storeStreamer;
    private final FacetTupleStreamTemplate tupleStream;
    private final FacetTupleStreamConverter facetTupleStreamConverter;
    private final RdfStreamer rdfStreamer;
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

    protected BasicIdService(
            StoreStreamer<T> storeStreamer,
            FacetTupleStreamTemplate tupleStream,
            FacetConfig facetConfig,
            RdfStreamer rdfStreamer,
            RequestConverter requestConverter) {
        this.storeStreamer = storeStreamer;
        this.tupleStream = tupleStream;
        this.facetTupleStreamConverter =
                new FacetTupleStreamConverter(getSolrIdField(), facetConfig);
        this.rdfStreamer = rdfStreamer;
        this.requestConverter = requestConverter;
    }

    public QueryResult<U> getMappedEntries(
            SearchRequest searchRequest, IdMappingResult mappingResult, String jobId) {
        return getMappedEntries(searchRequest, mappingResult, false, jobId);
    }

    public QueryResult<U> getMappedEntries(
            SearchRequest searchRequest,
            IdMappingResult mappingResult,
            boolean includeIsoform,
            String jobId) {
        List<IdMappingStringPair> mappedIds = mappingResult.getMappedIds();
        List<Facet> facets = null;

        validateMappedIdsEnrichmentLimit(mappedIds);

        List<ProblemPair> warnings = new ArrayList<>();
        if (needSearchInSolr(searchRequest, includeIsoform)) {
            List<String> toIds = getMappedToIds(mappedIds);

            long start = System.currentTimeMillis();

            // unset facets if mapped to ids exceeds the allowed limit and set the warning
            if (facetingDisallowed(searchRequest, mappedIds)) {
                searchRequest.removeFacets();
                warnings.add(
                        new ProblemPair(
                                PredefinedAPIStatus.FACET_WARNING.getCode(),
                                PredefinedAPIStatus.FACET_WARNING.getErrorMessage(
                                        this.maxIdMappingToIdsCountWithFacets)));
            }

            SolrRequest solrRequest =
                    requestConverter.createSearchIdsSolrRequest(
                            searchRequest, toIds, getSolrIdField());
            SolrStreamFacetResponse solrStreamResponse = searchBySolrStream(solrRequest);
            long end = System.currentTimeMillis();
            log.debug(
                    "Time taken to search solr in ms {} for jobId {} in getMappedEntries",
                    (end - start),
                    jobId);

            facets = solrStreamResponse.getFacets();

            List<String> solrToIds = solrStreamResponse.getIds();
            if (Utils.notNullNotEmpty(searchRequest.getQuery()) || includeIsoform) {
                // Apply Filter in PIR result
                mappedIds = applyQueryFilter(mappedIds, solrToIds);
            }

            if (Utils.notNullNotEmpty(searchRequest.getSort())) {
                mappedIds = applySort(mappedIds, solrToIds);
            }
        }

        // compute the cursor and get subset of accessions as per cursor
        int pageSize = getPageSize(searchRequest);
        CursorPage cursor = CursorPage.of(searchRequest.getCursor(), pageSize, mappedIds.size());
        long start = System.currentTimeMillis();
        Stream<U> result = getPagedEntries(mappedIds, cursor, searchRequest);
        long end = System.currentTimeMillis();
        log.debug(
                "Total time taken to call voldemort in ms {} for jobId {} in getMappedEntries",
                (end - start),
                jobId);
        ExtraOptions extraOptions = IdMappingServiceUtils.getExtraOptions(mappingResult);

        return QueryResult.<U>builder()
                .content(result)
                .page(cursor)
                .facets(facets)
                .extraOptions(extraOptions)
                .warnings(warnings)
                .build();
    }

    public Stream<U> streamEntries(
            StreamRequest streamRequest, IdMappingResult mappingResult, String jobId) {
        List<IdMappingStringPair> mappedIds =
                streamFilterAndSortEntries(streamRequest, mappingResult.getMappedIds(), jobId);
        return streamEntries(mappedIds, streamRequest);
    }

    public Stream<String> streamRdf(
            StreamRequest streamRequest,
            IdMappingResult mappingResult,
            String jobId,
            String dataType,
            String format) {
        List<IdMappingStringPair> fromToPairs =
                streamFilterAndSortEntries(streamRequest, mappingResult.getMappedIds(), jobId);
        // get unique entry ids
        List<String> entryIds = new ArrayList<>();
        fromToPairs.stream()
                .filter(ft -> !entryIds.contains(ft.getTo()))
                .forEach(ft -> entryIds.add(ft.getTo()));

        return this.rdfStreamer.stream(entryIds.stream(), dataType, format);
    }

    protected abstract U convertToPair(IdMappingStringPair mId, Map<String, T> idEntryMap);

    protected abstract String getEntryId(T entry);

    protected abstract String getSolrIdField();

    protected abstract UniProtDataType getUniProtDataType();

    protected abstract Stream<U> streamEntries(
            List<IdMappingStringPair> mappedIds, StreamRequest streamRequest);

    protected Stream<T> getEntries(List<String> toIds, String fields) {
        StoreRequest storeRequest = StoreRequest.builder().fields(fields).build();
        return this.storeStreamer.streamEntries(toIds, storeRequest);
    }

    protected List<IdMappingStringPair> streamFilterAndSortEntries(
            StreamRequest streamRequest, List<IdMappingStringPair> mappedIds, String jobId) {
        return streamFilterAndSortEntries(streamRequest, mappedIds, false, jobId);
    }

    protected List<IdMappingStringPair> streamFilterAndSortEntries(
            StreamRequest streamRequest,
            List<IdMappingStringPair> mappedIds,
            boolean includeIsoform,
            String jobId) {
        if (Utils.notNull(streamRequest.getQuery())
                || Utils.notNull(streamRequest.getSort())
                || includeIsoform) {
            List<String> toIds = getMappedToIds(mappedIds);

            long start = System.currentTimeMillis();

            SolrRequest solrRequest =
                    requestConverter.createStreamIdsSolrRequest(
                            streamRequest, toIds, getSolrIdField());
            SolrStreamFacetResponse solrStreamResponse = searchBySolrStream(solrRequest);
            long end = System.currentTimeMillis();
            log.debug(
                    "Time taken to search solr in ms {} for jobId {} in streamFilterAndSortEntries",
                    (end - start),
                    jobId);

            List<String> solrToIds = solrStreamResponse.getIds();
            if (Utils.notNullNotEmpty(streamRequest.getQuery()) || includeIsoform) {
                // Apply Filter in PIR result
                mappedIds = applyQueryFilter(mappedIds, solrToIds);
            }

            if (Utils.notNullNotEmpty(streamRequest.getSort())) {
                mappedIds = applySort(mappedIds, solrToIds);
            }
        }
        return mappedIds;
    }

    protected SolrStreamFacetResponse searchBySolrStream(SolrRequest solrRequest) {
        if (!solrRequest.getFacets().isEmpty()) {
            // request without facets. Normally search and facets requests are mutually exclusive.
            SolrRequest searchRequest = solrRequest.createSearchRequest();
            TupleStream tupleStream = this.tupleStream.create(searchRequest);
            SolrStreamFacetResponse idsResponse =
                    this.facetTupleStreamConverter.convert(tupleStream, List.of());
            // get facets in batches
            List<SolrStreamFacetResponse> facetsInBatches = getFacetsInBatches(solrRequest);
            // merge them
            SolrStreamFacetResponse mergedResponse =
                    SolrStreamFacetResponse.merge(solrRequest, facetsInBatches, idsResponse);
            return mergedResponse;
        } else { // request without facets
            TupleStream facetTupleStream = this.tupleStream.create(solrRequest);
            SolrStreamFacetResponse singleResponse =
                    this.facetTupleStreamConverter.convert(facetTupleStream, List.of());
            return singleResponse;
        }
    }

    private Integer getPageSize(SearchRequest searchRequest) {
        Integer pageSize = this.defaultPageSize;
        if (Utils.notNull(searchRequest.getSize())) {
            pageSize = searchRequest.getSize();
        }
        return pageSize;
    }

    protected Stream<U> getPagedEntries(
            List<IdMappingStringPair> mappedIdPairs,
            CursorPage cursorPage,
            SearchRequest searchRequest) {
        List<IdMappingStringPair> mappedIdsInPage =
                mappedIdPairs.subList(
                        cursorPage.getOffset().intValue(), CursorPage.getNextOffset(cursorPage));

        // extract ids to get entries from store
        Set<String> toIds =
                mappedIdsInPage.stream()
                        .map(IdMappingStringPair::getTo)
                        .collect(Collectors.toSet());
        Stream<T> entries = getEntries(new ArrayList<>(toIds), searchRequest.getFields());
        // accession -> entry map
        Map<String, T> idEntryMap = constructIdEntryMap(entries);
        // from -> uniprot entry
        return mappedIdsInPage.stream().map(mId -> convertToPair(mId, idEntryMap));
    }

    /**
     * This method is responsible to sort mappedIdPairs by solrToIds.
     *
     * <p>Initially we create a Map<To,List<From>> to expose our order attribute "to" Then we
     * iterate over solrToIds (that is our sort reference) and map to all possible "from" that was
     * previously created (Map<To,List<From>>).
     *
     * @param mappedIdPairs Mapped Ids returned by PIR service
     * @param solrToIds Sorted Ids returned by Solr
     * @return mappedIdPairs sorted by solrToIds
     */
    private List<IdMappingStringPair> applySort(
            List<IdMappingStringPair> mappedIdPairs, List<String> solrToIds) {
        // create a Map<To,List<From>>
        Map<String, List<IdMappingStringPair>> toFromMap = new HashMap<>();
        for (IdMappingStringPair mappedIdPair : mappedIdPairs) {
            String from = mappedIdPair.getFrom();
            String to = mappedIdPair.getTo();
            toFromMap.putIfAbsent(to, new ArrayList<>());
            toFromMap.get(to).add(new IdMappingStringPair(from, to));
        }

        List<IdMappingStringPair> sortedPairs = new ArrayList<>();
        for (String solrToId : solrToIds) {
            List<IdMappingStringPair> idPairs = toFromMap.get(solrToId);
            sortedPairs.addAll(idPairs);
        }
        return sortedPairs;
    }

    private List<IdMappingStringPair> applyQueryFilter(
            List<IdMappingStringPair> mappedIdPairs, List<String> solrToIds) {
        List<IdMappingStringPair> filteredMappedIdPairs = new ArrayList<>();
        for (String solrTo : solrToIds) {
            String from = getFrom(solrTo, mappedIdPairs);
            filteredMappedIdPairs.add(new IdMappingStringPair(from, solrTo));
        }
        return filteredMappedIdPairs;
    }

    private String getFrom(String solrTo, List<IdMappingStringPair> mappedIdPairs) {
        return mappedIdPairs.stream()
                .filter(idPair -> solrTo.contains(idPair.getTo()))
                .map(IdMappingStringPair::getFrom)
                .findFirst()
                .orElseThrow();
    }

    private List<String> getMappedToIds(List<IdMappingStringPair> mappedIdPairs) {
        return mappedIdPairs.stream().map(IdMappingStringPair::getTo).collect(Collectors.toList());
    }

    private Map<String, T> constructIdEntryMap(Stream<T> entries) {
        return entries.collect(Collectors.toMap(this::getEntryId, Function.identity()));
    }

    private boolean needSearchInSolr(SearchRequest searchRequest, boolean includeIsoform) {
        return Utils.notNullNotEmpty(searchRequest.getQuery())
                || Utils.notNullNotEmpty(searchRequest.getFacets())
                || Utils.notNullNotEmpty(searchRequest.getSort())
                || includeIsoform;
    }

    public void validateMappedIdsEnrichmentLimit(List<IdMappingStringPair> mappedIds) {
        if (mappedIds.size() > this.maxIdMappingToIdsCountEnriched) {
            throw new InvalidRequestException(
                    PredefinedAPIStatus.ENRICHMENT_WARNING.getErrorMessage(
                            this.maxIdMappingToIdsCountEnriched));
        }
    }

    private boolean facetingDisallowed(
            SearchRequest searchRequest, List<IdMappingStringPair> mappedIds) {
        return Utils.notNullNotEmpty(searchRequest.getFacets())
                && mappedIds.size() > this.maxIdMappingToIdsCountWithFacets;
    }

    private List<SolrStreamFacetResponse> getFacetsInBatches(SolrRequest solrRequest) {
        List<String> ids = solrRequest.getIds();
        List<SolrStreamFacetResponse> facetResponses = new ArrayList<>();
        boolean ignoreLimit =
                true; // for batch faceting, get all facets and return "limit" number of facets
        // during merge
        for (int i = 0; i < ids.size(); i += this.idBatchSize) {
            List<String> idsBatch = ids.subList(i, Math.min(i + this.idBatchSize, ids.size()));
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
