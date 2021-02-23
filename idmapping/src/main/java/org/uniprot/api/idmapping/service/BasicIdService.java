package org.uniprot.api.idmapping.service;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.io.stream.TupleStream;
import org.springframework.beans.factory.annotation.Value;
import org.uniprot.api.common.repository.search.QueryResult;
import org.uniprot.api.common.repository.search.facet.FacetConfig;
import org.uniprot.api.common.repository.search.facet.FacetTupleStreamConverter;
import org.uniprot.api.common.repository.search.facet.SolrStreamFacetResponse;
import org.uniprot.api.common.repository.solrstream.FacetTupleStreamTemplate;
import org.uniprot.api.common.repository.solrstream.SolrStreamFacetRequest;
import org.uniprot.api.common.repository.stream.store.StoreStreamer;
import org.uniprot.api.idmapping.controller.request.UniProtKBIdMappingSearchRequest;
import org.uniprot.api.idmapping.controller.request.UniProtKBIdMappingStreamRequest;
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

    public abstract QueryResult<U> getMappedEntries(UniProtKBIdMappingSearchRequest searchRequest);

    protected Stream<T> getEntries(List<String> toIds) {
        return this.storeStreamer.streamEntries(toIds);
    }

    public List<Object> streamEntries(UniProtKBIdMappingStreamRequest streamRequest) {
        return Collections.emptyList(); // TODO fill code
    }

    protected SolrStreamFacetResponse searchBySolrStream(
            List<String> ids, UniProtKBIdMappingSearchRequest searchRequest) {
        SolrStreamFacetRequest solrStreamRequest = createSolrStreamRequest(ids, searchRequest);
        TupleStream facetTupleStream = this.tupleStream.create(solrStreamRequest);
        return this.facetTupleStreamConverter.convert(facetTupleStream);
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

    public abstract String getSolrIdField();

    public abstract UniProtDataType getUniProtDataType();

    protected Integer getDefaultPageSize() {
        return this.defaultPageSize;
    }
}
