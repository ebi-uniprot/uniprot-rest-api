package org.uniprot.api.idmapping.service;

import lombok.Builder;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.solr.client.solrj.io.stream.TupleStream;
import org.springframework.beans.factory.annotation.Value;
import org.uniprot.api.common.exception.InvalidRequestException;
import org.uniprot.api.common.repository.search.ProblemPair;
import org.uniprot.api.common.repository.search.QueryResult;
import org.uniprot.api.common.repository.search.SolrQueryConfig;
import org.uniprot.api.common.repository.search.facet.Facet;
import org.uniprot.api.common.repository.search.facet.FacetConfig;
import org.uniprot.api.common.repository.search.facet.FacetTupleStreamConverter;
import org.uniprot.api.common.repository.search.facet.SolrStreamFacetResponse;
import org.uniprot.api.common.repository.search.page.impl.CursorPage;
import org.uniprot.api.common.repository.solrstream.FacetTupleStreamTemplate;
import org.uniprot.api.common.repository.solrstream.SolrStreamFacetRequest;
import org.uniprot.api.common.repository.stream.rdf.RdfStreamer;
import org.uniprot.api.common.repository.stream.store.StoreStreamer;
import org.uniprot.api.idmapping.model.IdMappingResult;
import org.uniprot.api.idmapping.model.IdMappingStringPair;
import org.uniprot.api.idmapping.service.impl.UniProtKBIdService;
import org.uniprot.api.rest.request.SearchRequest;
import org.uniprot.api.rest.request.StreamRequest;
import org.uniprot.api.rest.request.UniProtKBRequestUtil;
import org.uniprot.core.util.Utils;
import org.uniprot.store.config.UniProtDataType;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.uniprot.api.rest.output.PredefinedAPIStatus.ENRICHMENT_WARNING;
import static org.uniprot.api.rest.output.PredefinedAPIStatus.FACET_WARNING;

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
    private final SolrQueryConfig queryConfig;
    private final FacetConfig facetConfig;

    @Value("${search.default.page.size:#{null}}")
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

    protected BasicIdService(
            StoreStreamer<T> storeStreamer,
            FacetTupleStreamTemplate tupleStream,
            FacetConfig facetConfig,
            RdfStreamer rdfStreamer,
            SolrQueryConfig queryConfig) {
        this.storeStreamer = storeStreamer;
        this.tupleStream = tupleStream;
        this.facetConfig = facetConfig;
        this.facetTupleStreamConverter =
                new FacetTupleStreamConverter(getSolrIdField(), facetConfig);
        this.rdfStreamer = rdfStreamer;
        this.queryConfig = queryConfig;
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
                                FACET_WARNING.getCode(),
                                FACET_WARNING.getErrorMessage(
                                        this.maxIdMappingToIdsCountWithFacets)));
            }

            SolrStreamFacetResponse solrStreamResponse =
                    searchBySolrStream(toIds, searchRequest, includeIsoform);
            long end = System.currentTimeMillis();
            log.info(
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
        Stream<U> result = getPagedEntries(mappedIds, cursor, searchRequest.getFields());
        long end = System.currentTimeMillis();
        log.info(
                "Total time taken to call voldemort in ms {} for jobId {} in getMappedEntries",
                (end - start),
                jobId);

        return QueryResult.of(result, cursor, facets, mappingResult.getUnmappedIds(), warnings);
    }

    public Stream<U> streamEntries(
            StreamRequest streamRequest, IdMappingResult mappingResult, String jobId) {
        List<IdMappingStringPair> mappedIds =
                streamFilterAndSortEntries(streamRequest, mappingResult.getMappedIds(), jobId);
        return streamEntries(mappedIds, streamRequest.getFields());
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

    protected String getTermsQueryField() {
        return getSolrIdField();
    }

    protected abstract UniProtDataType getUniProtDataType();

    protected abstract Stream<U> streamEntries(List<IdMappingStringPair> mappedIds, String fields);

    protected Stream<T> getEntries(List<String> toIds, String fields) {
        return this.storeStreamer.streamEntries(toIds);
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

            SearchRequest searchRequest = SearchStreamRequest.from(streamRequest);
            SolrStreamFacetResponse solrStreamResponse =
                    searchBySolrStream(toIds, searchRequest, includeIsoform);
            long end = System.currentTimeMillis();
            log.info(
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

    protected SolrStreamFacetResponse searchBySolrStream(
            List<String> ids, SearchRequest searchRequest, boolean includeIsoform) {
        String termsField = getSolrIdField();
        // use accession field to get isoform also if asked for kb mapping
        boolean filterIsoform =
                UniProtKBRequestUtil.needsToFilterIsoform(
                        UniProtKBIdService.ACCESSION,
                        UniProtKBIdService.IS_ISOFORM,
                        searchRequest.getQuery(),
                        includeIsoform);
        if (!filterIsoform) {
            termsField = getTermsQueryField();
        }
        SolrStreamFacetRequest solrStreamRequest =
                SolrStreamFacetRequest.createSolrStreamFacetRequest(
                        queryConfig,
                        getUniProtDataType(),
                        getSolrIdField(),
                        termsField,
                        ids,
                        searchRequest,
                        includeIsoform);
        TupleStream facetTupleStream = this.tupleStream.create(solrStreamRequest, facetConfig);
        return this.facetTupleStreamConverter.convert(
                facetTupleStream, searchRequest.getFacetList());
    }

    private Integer getPageSize(SearchRequest searchRequest) {
        Integer pageSize = this.defaultPageSize;
        if (Utils.notNull(searchRequest.getSize())) {
            pageSize = searchRequest.getSize();
        }
        return pageSize;
    }

    private Stream<U> getPagedEntries(
            List<IdMappingStringPair> mappedIdPairs, CursorPage cursorPage, String fields) {
        List<IdMappingStringPair> mappedIdsInPage =
                mappedIdPairs.subList(
                        cursorPage.getOffset().intValue(), CursorPage.getNextOffset(cursorPage));

        // extract ids to get entries from store
        Set<String> toIds =
                mappedIdsInPage.stream()
                        .map(IdMappingStringPair::getTo)
                        .collect(Collectors.toSet());
        Stream<T> entries = getEntries(new ArrayList<>(toIds), fields);
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
        Map<String, List<String>> toMap =
                mappedIdPairs.stream()
                        .collect(
                                Collectors.groupingBy(
                                        IdMappingStringPair::getTo,
                                        Collectors.mapping(
                                                IdMappingStringPair::getFrom,
                                                Collectors.toList())));
        mappedIdPairs =
                solrToIds.stream()
                        .flatMap(
                                to ->
                                        getToList(to, toMap).stream()
                                                .map(from -> new IdMappingStringPair(from, to)))
                        .collect(Collectors.toList());
        return mappedIdPairs;
    }

    private List<String> getToList(String lookupKey, Map<String, List<String>> toMap) {
        return toMap.entrySet().stream()
                .filter(entry -> lookupKey.contains(entry.getKey()))
                .map(Map.Entry::getValue)
                .findFirst()
                .orElseThrow();
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
                    ENRICHMENT_WARNING.getErrorMessage(this.maxIdMappingToIdsCountEnriched));
        }
    }

    private boolean facetingDisallowed(
            SearchRequest searchRequest, List<IdMappingStringPair> mappedIds) {
        return Utils.notNullNotEmpty(searchRequest.getFacets())
                && mappedIds.size() > this.maxIdMappingToIdsCountWithFacets;
    }

    @Builder
    @Getter
    private static class SearchStreamRequest implements SearchRequest {
        private String facets;
        private final String cursor;
        private final String query;
        private final String sort;
        private final String fields;
        private Integer size;

        @Override
        public void setSize(Integer size) {
            this.size = size;
        }

        static SearchRequest from(StreamRequest streamRequest) {
            return SearchStreamRequest.builder()
                    .fields(streamRequest.getFields())
                    .query(streamRequest.getQuery())
                    .sort(streamRequest.getSort())
                    .build();
        }
    }
}
