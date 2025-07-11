package org.uniprot.api.idmapping.common.service;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.uniprot.api.common.repository.search.ExtraOptions;
import org.uniprot.api.common.repository.search.ProblemPair;
import org.uniprot.api.common.repository.search.QueryResult;
import org.uniprot.api.common.repository.search.facet.Facet;
import org.uniprot.api.common.repository.search.facet.FacetConfig;
import org.uniprot.api.common.repository.search.facet.SolrStreamFacetResponse;
import org.uniprot.api.common.repository.search.page.impl.CursorPage;
import org.uniprot.api.common.repository.solrstream.FacetTupleStreamTemplate;
import org.uniprot.api.common.repository.stream.rdf.RdfStreamer;
import org.uniprot.api.common.repository.stream.store.StoreStreamer;
import org.uniprot.api.idmapping.common.model.IdMappingResult;
import org.uniprot.api.idmapping.common.response.model.IdMappingStringPair;
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
public abstract class BasicIdService<T, U> extends AbstractIdService<T> {
    private final RdfStreamer rdfStreamer;

    protected BasicIdService(
            StoreStreamer<T> storeStreamer,
            FacetTupleStreamTemplate tupleStream,
            FacetConfig facetConfig,
            RdfStreamer rdfStreamer,
            RequestConverter requestConverter) {
        super(storeStreamer, tupleStream, facetConfig, requestConverter);
        this.rdfStreamer = rdfStreamer;
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
        validateMappedIdsEnrichmentLimit(mappedIds.size());
        List<ProblemPair> warnings = new ArrayList<>();
        if (solrSearchNeededBySearchRequest(searchRequest, includeIsoform)) {
            List<String> toIds = getMappedToIds(mappedIds);
            long start = System.currentTimeMillis();
            // unset facets if mapped to ids exceeds the allowed limit and set the warning
            if (facetingDisallowed(searchRequest, toIds)) {
                warnings.add(removeFacetsAndGetFacetWarning(searchRequest));
            }

            SolrStreamFacetResponse solrStreamResponse =
                    searchMappedIdsFacetsBySearchRequest(searchRequest, toIds);

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

        return this.rdfStreamer.stream(entryIds, dataType, format);
    }

    protected abstract U convertToPair(IdMappingStringPair mId, Map<String, T> idEntryMap);

    protected abstract String getEntryId(T entry);

    protected abstract UniProtDataType getUniProtDataType();

    protected abstract Stream<U> streamEntries(
            List<IdMappingStringPair> mappedIds, StreamRequest streamRequest);

    protected List<IdMappingStringPair> streamFilterAndSortEntries(
            StreamRequest streamRequest, List<IdMappingStringPair> mappedIds, String jobId) {
        return streamFilterAndSortEntries(streamRequest, mappedIds, false, jobId);
    }

    protected List<IdMappingStringPair> streamFilterAndSortEntries(
            StreamRequest streamRequest,
            List<IdMappingStringPair> mappedIds,
            boolean includeIsoform,
            String jobId) {
        if (solrSearchNeededByStreamRequest(streamRequest, includeIsoform)) {
            List<String> toIds = getMappedToIds(mappedIds);

            long start = System.currentTimeMillis();

            SolrStreamFacetResponse solrStreamResponse =
                    searchMappedIdsFacetsByStreamRequest(streamRequest, toIds);

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
}
