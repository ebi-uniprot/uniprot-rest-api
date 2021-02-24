package org.uniprot.api.idmapping.service;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
import org.uniprot.api.idmapping.controller.request.UniProtKBIdMappingSearchRequest;
import org.uniprot.api.idmapping.controller.request.UniProtKBIdMappingStreamRequest;
import org.uniprot.api.idmapping.model.IdMappingResult;
import org.uniprot.api.idmapping.model.IdMappingStringPair;
import org.uniprot.api.rest.search.SortUtils;
import org.uniprot.core.util.Utils;
import org.uniprot.store.config.UniProtDataType;

/**
 * @author sahmad
 * @created 16/02/2021
 */
public abstract class BasicIdService<T, U> {
    private final IDMappingPIRService idMappingService;
    private final StoreStreamer<T> storeStreamer;
    private final FacetTupleStreamTemplate tupleStream;
    private final FacetTupleStreamConverter facetTupleStreamConverter;

    @Value("${search.default.page.size:#{null}}")
    private Integer defaultPageSize;

    protected BasicIdService(
            IDMappingPIRService idMappingService,
            StoreStreamer<T> storeStreamer,
            FacetTupleStreamTemplate tupleStream,
            FacetConfig facetConfig) {
        this.idMappingService = idMappingService;
        this.storeStreamer = storeStreamer;
        this.tupleStream = tupleStream;
        this.facetTupleStreamConverter = new FacetTupleStreamConverter(facetConfig);
    }

    public QueryResult<U> getMappedEntries(UniProtKBIdMappingSearchRequest searchRequest) {
        // get the mapped ids from PIR
        IdMappingResult mappingResult = idMappingService.mapIds(searchRequest);
        List<IdMappingStringPair> mappedIdPairs = mappingResult.getMappedIds();
        List<Facet> facets = null;
        if (needSearchInSolr(searchRequest)) {

            List<String> toIds =
                    mappedIdPairs.stream()
                            .map(IdMappingStringPair::getTo)
                            .collect(Collectors.toList());

            SolrStreamFacetResponse solrStreamResponse = searchBySolrStream(toIds, searchRequest);

            facets = solrStreamResponse.getFacets();

            List<String> solrToIds = solrStreamResponse.getAccessions();
            if (Utils.notNullNotEmpty(searchRequest.getQuery())) {
                // Apply Filter in PIR result
                mappedIdPairs =
                        mappedIdPairs.stream()
                                .filter(idPair -> solrToIds.contains(idPair.getTo()))
                                .collect(Collectors.toList());
            }

            if (Utils.notNullNotEmpty(searchRequest.getSort())) {
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
                                                        .map(
                                                                from ->
                                                                        new IdMappingStringPair(
                                                                                from, to)))
                                .collect(Collectors.toList());
            }
        }

        int pageSize =
                Objects.isNull(searchRequest.getSize())
                        ? getDefaultPageSize()
                        : searchRequest.getSize();

        // compute the cursor and get subset of accessions as per cursor
        CursorPage cursorPage =
                CursorPage.of(searchRequest.getCursor(), pageSize, mappedIdPairs.size());

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
        Stream<U> result =
                mappedIdsInPage.stream()
                        .filter(mId -> idEntryMap.containsKey(mId.getTo()))
                        .map(mId -> convertToPair(mId, idEntryMap));

        return QueryResult.of(result, cursorPage, facets, null, mappingResult.getUnmappedIds());
    }

    public List<Object> streamEntries(UniProtKBIdMappingStreamRequest streamRequest) {
        return Collections.emptyList(); // TODO fill code
    }

    protected abstract U convertToPair(IdMappingStringPair mId, Map<String, T> idEntryMap);

    protected abstract String getEntryId(T entry);

    protected abstract String getSolrIdField();

    protected abstract UniProtDataType getUniProtDataType();

    protected Stream<T> getEntries(List<String> toIds) {
        return this.storeStreamer.streamEntries(toIds);
    }

    protected SolrStreamFacetResponse searchBySolrStream(
            List<String> ids, UniProtKBIdMappingSearchRequest searchRequest) {
        SolrStreamFacetRequest solrStreamRequest = createSolrStreamRequest(ids, searchRequest);
        TupleStream facetTupleStream = this.tupleStream.create(solrStreamRequest);
        return this.facetTupleStreamConverter.convert(facetTupleStream);
    }

    protected Integer getDefaultPageSize() {
        return this.defaultPageSize;
    }

    private SolrStreamFacetRequest createSolrStreamRequest(
            List<String> ids, UniProtKBIdMappingSearchRequest searchRequest) {
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

    private boolean needSearchInSolr(UniProtKBIdMappingSearchRequest searchRequest) {
        return Utils.notNullNotEmpty(searchRequest.getQuery())
                || Utils.notNullNotEmpty(searchRequest.getFacets())
                || Utils.notNullNotEmpty(searchRequest.getSort());
    }
}
