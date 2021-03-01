package org.uniprot.api.idmapping.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import lombok.extern.slf4j.Slf4j;

import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.io.stream.TupleStream;
import org.springframework.beans.factory.annotation.Value;
import org.uniprot.api.common.repository.search.QueryResult;
import org.uniprot.api.common.repository.search.facet.Facet;
import org.uniprot.api.common.repository.search.facet.FacetConfig;
import org.uniprot.api.common.repository.search.facet.FacetTupleStreamConverter;
import org.uniprot.api.common.repository.search.facet.SolrStreamFacetResponse;
import org.uniprot.api.common.repository.search.page.impl.CursorPage;
import org.uniprot.api.common.repository.solrstream.FacetTupleStreamTemplate;
import org.uniprot.api.common.repository.solrstream.SolrStreamFacetRequest;
import org.uniprot.api.common.repository.stream.store.StoreStreamer;
import org.uniprot.api.idmapping.controller.request.IdMappingStreamRequest;
import org.uniprot.api.idmapping.model.IdMappingResult;
import org.uniprot.api.idmapping.model.IdMappingStringPair;
import org.uniprot.api.rest.request.SearchRequest;
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

    @Value("${search.default.page.size:#{null}}")
    private Integer defaultPageSize;

    protected BasicIdService(
            StoreStreamer<T> storeStreamer,
            FacetTupleStreamTemplate tupleStream,
            FacetConfig facetConfig) {
        this.storeStreamer = storeStreamer;
        this.tupleStream = tupleStream;
        this.facetTupleStreamConverter = new FacetTupleStreamConverter(facetConfig);
    }

    public QueryResult<U> getMappedEntries(
            SearchRequest searchRequest, IdMappingResult mappingResult) {
        List<IdMappingStringPair> mappedIds = mappingResult.getMappedIds();
        List<Facet> facets = null;
        if (needSearchInSolr(searchRequest)) {
            List<String> toIds = getMappedToIds(mappedIds);

            long start = System.currentTimeMillis();
            SolrStreamFacetResponse solrStreamResponse = searchBySolrStream(toIds, searchRequest);
            long end = System.currentTimeMillis();
            log.info("Time taken to search solr in ms {}", (end - start));

            facets = solrStreamResponse.getFacets();

            List<String> solrToIds = solrStreamResponse.getAccessions();
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
        log.info("Total time taken to call voldemort in ms {}", (end - start));

        return QueryResult.of(result, cursor, facets, null, mappingResult.getUnmappedIds());
    }

    public List<Object> streamEntries(IdMappingStreamRequest streamRequest) {
        return Collections.emptyList(); // TODO fill code
    }

    protected abstract U convertToPair(IdMappingStringPair mId, Map<String, T> idEntryMap);

    protected abstract String getEntryId(T entry);

    protected abstract String getSolrIdField();

    protected abstract UniProtDataType getUniProtDataType();

    protected Stream<T> getEntries(List<String> toIds) {
        return this.storeStreamer.streamEntries(toIds);
    }

    private SolrStreamFacetResponse searchBySolrStream(
            List<String> ids, SearchRequest searchRequest) {
        SolrStreamFacetRequest solrStreamRequest = createSolrStreamRequest(ids, searchRequest);
        TupleStream facetTupleStream = this.tupleStream.create(solrStreamRequest);
        return this.facetTupleStreamConverter.convert(facetTupleStream);
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
        // append the facet filter query in the accession query
        if (Utils.notNullNotEmpty(searchRequest.getQuery())) {
            qb.append(" AND (").append(searchRequest.getQuery()).append(")");
            solrRequestBuilder.searchAccession(Boolean.TRUE);
        }

        if (Utils.notNullNotEmpty(searchRequest.getSort())) {
            List<SolrQuery.SortClause> sort =
                    SortUtils.parseSortClause(getUniProtDataType(), searchRequest.getSort());
            solrRequestBuilder.searchSort(getSearchSort(sort));
            solrRequestBuilder.searchFieldList(getFieldList(sort));
            solrRequestBuilder.searchAccession(Boolean.TRUE);
        }

        return solrRequestBuilder.query(qb.toString()).facets(searchRequest.getFacetList()).build();
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
}
