package org.uniprot.api.idmapping.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import lombok.Builder;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.io.stream.TupleStream;
import org.springframework.beans.factory.annotation.Value;
import org.uniprot.api.common.repository.search.QueryResult;
import org.uniprot.api.common.repository.search.SolrQueryConfig;
import org.uniprot.api.common.repository.search.facet.Facet;
import org.uniprot.api.common.repository.search.facet.FacetConfig;
import org.uniprot.api.common.repository.search.facet.FacetTupleStreamConverter;
import org.uniprot.api.common.repository.search.facet.SolrStreamFacetResponse;
import org.uniprot.api.common.repository.search.page.impl.CursorPage;
import org.uniprot.api.common.repository.solrstream.FacetTupleStreamTemplate;
import org.uniprot.api.common.repository.solrstream.SolrStreamFacetRequest;
import org.uniprot.api.common.repository.stream.rdf.RDFStreamer;
import org.uniprot.api.common.repository.stream.store.StoreStreamer;
import org.uniprot.api.idmapping.model.IdMappingResult;
import org.uniprot.api.idmapping.model.IdMappingStringPair;
import org.uniprot.api.rest.request.SearchRequest;
import org.uniprot.api.rest.request.StreamRequest;
import org.uniprot.api.rest.search.SortUtils;
import org.uniprot.core.util.Utils;
import org.uniprot.store.config.UniProtDataType;

/**
 * @author sahmad
 * @created 16/02/2021
 */
@Slf4j
public abstract class BasicIdService<T, U> {
    private final StoreStreamer<T> storeStreamer;
    private final FacetTupleStreamTemplate tupleStream;
    private final FacetTupleStreamConverter facetTupleStreamConverter;
    private final RDFStreamer rdfStreamer;
    private final SolrQueryConfig queryConfig;
    private final FacetConfig facetConfig;

    @Value("${search.default.page.size:#{null}}")
    private Integer defaultPageSize;

    // the maximum number of ids allowed in `to` field after mapped by `from` fields
    @Value("${id.mapping.max.to.ids.count:#{null}}")// value to 500k
    private Integer maxIdMappingToIdsCount;

    // Maximum number of `to` ids supported to enrich result with uniprot data
    // Greater than maxIdMappingToIdsCountEnriched and less than maxIdMappingToIdsCount, the API should return only `to` ids
    @Value("${id.mapping.max.to.ids.enrich.count:#{null}}")// value to 100k
    private Integer maxIdMappingToIdsCountEnriched;

    // Maximum number of `to` ids supported with faceting query
    @Value("${id.mapping.max.to.ids.with.facets.count:#{null}}")// value to 10k
    private Integer maxIdMappingToIdsCountWithFacets;

    protected BasicIdService(
            StoreStreamer<T> storeStreamer,
            FacetTupleStreamTemplate tupleStream,
            FacetConfig facetConfig,
            RDFStreamer rdfStreamer,
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
            SearchRequest searchRequest, IdMappingResult mappingResult) {
        List<IdMappingStringPair> mappedIds = mappingResult.getMappedIds();
        List<Facet> facets = null;

        // validate the limits
        validateRequest(searchRequest, mappedIds);

        if (needSearchInSolr(searchRequest)) {
            List<String> toIds = getMappedToIds(mappedIds);

            long start = System.currentTimeMillis();
            SolrStreamFacetResponse solrStreamResponse = searchBySolrStream(toIds, searchRequest);
            long end = System.currentTimeMillis();
            log.debug("Time taken to search solr in ms {}", (end - start));

            facets = solrStreamResponse.getFacets();

            List<String> solrToIds = solrStreamResponse.getIds();
            if (Utils.notNullNotEmpty(searchRequest.getQuery())) {
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
        Stream<U> result = getPagedEntries(mappedIds, cursor);
        long end = System.currentTimeMillis();
        log.debug("Total time taken to call voldemort in ms {}", (end - start));

        return QueryResult.of(result, cursor, facets, null, mappingResult.getUnmappedIds());
    }

    public Stream<U> streamEntries(StreamRequest streamRequest, IdMappingResult mappingResult) {
        List<IdMappingStringPair> mappedIds =
                streamFilterAndSortEntries(streamRequest, mappingResult.getMappedIds());
        return streamEntries(mappedIds);
    }

    public Stream<String> streamRDF(StreamRequest streamRequest, IdMappingResult mappingResult) {
        List<IdMappingStringPair> fromToPairs =
                streamFilterAndSortEntries(streamRequest, mappingResult.getMappedIds());
        // get unique entry ids
        List<String> entryIds = new ArrayList<>();
        fromToPairs.stream()
                .filter(ft -> !entryIds.contains(ft.getTo()))
                .forEach(ft -> entryIds.add(ft.getTo()));

        return this.rdfStreamer.streamRDFXML(entryIds.stream());
    }

    protected abstract U convertToPair(IdMappingStringPair mId, Map<String, T> idEntryMap);

    protected abstract String getEntryId(T entry);

    protected abstract String getSolrIdField();

    protected abstract UniProtDataType getUniProtDataType();

    protected abstract Stream<U> streamEntries(List<IdMappingStringPair> mappedIds);

    protected Stream<T> getEntries(List<String> toIds) {
        return this.storeStreamer.streamEntries(toIds);
    }

    private List<IdMappingStringPair> streamFilterAndSortEntries(
            StreamRequest streamRequest, List<IdMappingStringPair> mappedIds) {
        if (Utils.notNull(streamRequest.getQuery()) || Utils.notNull(streamRequest.getSort())) {
            List<String> toIds = getMappedToIds(mappedIds);

            long start = System.currentTimeMillis();

            SearchRequest searchRequest = SearchStreamRequest.from(streamRequest);
            SolrStreamFacetResponse solrStreamResponse = searchBySolrStream(toIds, searchRequest);
            long end = System.currentTimeMillis();
            log.debug("Time taken to search solr in ms {}", (end - start));

            List<String> solrToIds = solrStreamResponse.getIds();
            if (Utils.notNullNotEmpty(streamRequest.getQuery())) {
                // Apply Filter in PIR result
                mappedIds = applyQueryFilter(mappedIds, solrToIds);
            }

            if (Utils.notNullNotEmpty(streamRequest.getSort())) {
                mappedIds = applySort(mappedIds, solrToIds);
            }
        }
        return mappedIds;
    }

    private SolrStreamFacetResponse searchBySolrStream(
            List<String> ids, SearchRequest searchRequest) {
        SolrStreamFacetRequest solrStreamRequest = createSolrStreamRequest(ids, searchRequest);
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
            List<IdMappingStringPair> mappedIdPairs, CursorPage cursorPage) {
        List<IdMappingStringPair> mappedIdsInPage =
                mappedIdPairs.subList(
                        cursorPage.getOffset().intValue(), CursorPage.getNextOffset(cursorPage));

        // extract ids to get entries from store
        Set<String> toIds =
                mappedIdsInPage.stream()
                        .map(IdMappingStringPair::getTo)
                        .collect(Collectors.toSet());
        Stream<T> entries = getEntries(new ArrayList<>(toIds));
        // accession -> entry map
        Map<String, T> idEntryMap = constructIdEntryMap(entries);
        // from -> uniprot entry
        return mappedIdsInPage.stream()
                .filter(mId -> idEntryMap.containsKey(mId.getTo()))
                .map(mId -> convertToPair(mId, idEntryMap));
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
                                        toMap.get(to).stream()
                                                .map(from -> new IdMappingStringPair(from, to)))
                        .collect(Collectors.toList());
        return mappedIdPairs;
    }

    private List<IdMappingStringPair> applyQueryFilter(
            List<IdMappingStringPair> mappedIdPairs, List<String> solrToIds) {
        return mappedIdPairs.stream()
                .filter(idPair -> solrToIds.contains(idPair.getTo()))
                .collect(Collectors.toList());
    }

    private List<String> getMappedToIds(List<IdMappingStringPair> mappedIdPairs) {
        return mappedIdPairs.stream().map(IdMappingStringPair::getTo).collect(Collectors.toList());
    }

    private SolrStreamFacetRequest createSolrStreamRequest(
            List<String> ids, SearchRequest searchRequest) {
        SolrStreamFacetRequest.SolrStreamFacetRequestBuilder solrRequestBuilder =
                SolrStreamFacetRequest.builder();

        // construct the query for tuple stream
        StringBuilder qb = new StringBuilder();
        qb.append("({!terms f=")
                .append(getSolrIdField())
                .append("}")
                .append(String.join(",", ids))
                .append(")");
        String termQuery = qb.toString();

        // append the facet filter query in the accession query
        if (Utils.notNullNotEmpty(searchRequest.getQuery())) {
            solrRequestBuilder.query(searchRequest.getQuery());
            solrRequestBuilder.filteredQuery(termQuery);
            solrRequestBuilder.searchAccession(Boolean.TRUE);
            solrRequestBuilder.searchSort(getSolrIdField() + " asc");
            solrRequestBuilder.searchFieldList(getSolrIdField());
        } else {
            solrRequestBuilder.query(termQuery);
        }

        if (Utils.notNullNotEmpty(searchRequest.getSort())) {
            List<SolrQuery.SortClause> sort =
                    SortUtils.parseSortClause(getUniProtDataType(), searchRequest.getSort());
            solrRequestBuilder.searchSort(getSearchSort(sort));
            solrRequestBuilder.searchFieldList(getFieldList(sort));
            solrRequestBuilder.searchAccession(Boolean.TRUE);
        }

        return solrRequestBuilder
                .queryConfig(this.queryConfig)
                .facets(searchRequest.getFacetList())
                .build();
    }

    private String getSearchSort(List<SolrQuery.SortClause> sort) {
        return sort.stream()
                .map(clause -> clause.getItem() + " " + clause.getOrder().name())
                .collect(Collectors.joining(","));
    }

    private String getFieldList(List<SolrQuery.SortClause> sort) {
        Set<String> fieldList =
                sort.stream().map(SolrQuery.SortClause::getItem).collect(Collectors.toSet());
        fieldList.add(getSolrIdField());
        return String.join(",", fieldList);
    }

    private Map<String, T> constructIdEntryMap(Stream<T> entries) {
        return entries.collect(Collectors.toMap(this::getEntryId, Function.identity()));
    }

    private boolean needSearchInSolr(SearchRequest searchRequest) {
        return Utils.notNullNotEmpty(searchRequest.getQuery())
                || Utils.notNullNotEmpty(searchRequest.getFacets())
                || Utils.notNullNotEmpty(searchRequest.getSort());
    }

    private void validateRequest(SearchRequest searchRequest, List<IdMappingStringPair> mappedIds) {
        if(mappedIds.size() > this.maxIdMappingToIdsCount){
            throw new IllegalArgumentException("Maximum number of mapped ids supported "
                    + this.maxIdMappingToIdsCount);
        }

        if(Utils.notNullNotEmpty(searchRequest.getFacets())
                        && mappedIds.size() > this.maxIdMappingToIdsCountWithFacets){
            throw new IllegalArgumentException("facets are supported for less than "
                    + this.maxIdMappingToIdsCountWithFacets + " mapped ids.");
        }
    }

    @Builder
    @Getter
    private static class SearchStreamRequest implements SearchRequest {
        private final String facets;
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
