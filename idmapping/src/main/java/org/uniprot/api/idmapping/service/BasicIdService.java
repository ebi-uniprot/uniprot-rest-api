package org.uniprot.api.idmapping.service;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.uniprot.api.common.repository.search.facet.FacetConfig;
import org.uniprot.api.common.repository.solrstream.FacetTupleStreamTemplate;
import org.uniprot.api.common.repository.stream.store.StoreStreamer;
import org.uniprot.api.idmapping.controller.request.IdMappingSearchRequest;
import org.uniprot.api.idmapping.controller.request.IdMappingStreamRequest;

/**
 * @author sahmad
 * @created 16/02/2021
 */
public abstract class BasicIdService<T> {
    private final IDMappingService idMappingService;
    private final StoreStreamer<T> storeStreamer;
    private final FacetTupleStreamTemplate tupleStream;
    private final FacetConfig facetConfig;

    protected BasicIdService(
            IDMappingService idMappingService,
            StoreStreamer<T> storeStreamer,
            FacetTupleStreamTemplate tupleStream,
            FacetConfig facetConfig) {
        this.idMappingService = idMappingService;
        this.storeStreamer = storeStreamer;
        this.tupleStream = tupleStream;
        this.facetConfig = facetConfig;
    }

    // TODO define type of Object, may Pair<FromId, Entry>
    public List<Object> getEntries(IdMappingSearchRequest searchRequest) {
        // Fill code
        List<String> ids = List.of(searchRequest.getIds().split(","));
        Stream<T> entries = this.storeStreamer.streamEntries(ids);
        return entries.collect(Collectors.toList());
    }

    public List<Object> streamEntries(IdMappingStreamRequest streamRequest) {
        return null; // TODO fill code
    }
}
