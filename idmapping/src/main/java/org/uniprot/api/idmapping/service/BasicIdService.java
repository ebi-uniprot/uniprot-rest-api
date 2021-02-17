package org.uniprot.api.idmapping.service;

import java.util.List;
import java.util.stream.Stream;

import lombok.*;

import org.uniprot.api.common.repository.search.QueryResult;
import org.uniprot.api.common.repository.search.facet.FacetConfig;
import org.uniprot.api.common.repository.solrstream.FacetTupleStreamTemplate;
import org.uniprot.api.common.repository.stream.store.StoreStreamer;
import org.uniprot.api.idmapping.controller.request.IdMappingSearchRequest;
import org.uniprot.api.idmapping.controller.request.IdMappingStreamRequest;
import org.uniprot.core.util.Pair;

/**
 * @author sahmad
 * @created 16/02/2021
 */
public abstract class BasicIdService<T> {
    private final IDMappingPIRService idMappingService;
    private final StoreStreamer<T> storeStreamer;
    private final FacetTupleStreamTemplate tupleStream;
    private final FacetConfig facetConfig;
    protected final PIRResponseConverter pirResponseConverter;

    protected BasicIdService(
            IDMappingPIRService idMappingService,
            StoreStreamer<T> storeStreamer,
            FacetTupleStreamTemplate tupleStream,
            FacetConfig facetConfig) {
        this.idMappingService = idMappingService;
        this.storeStreamer = storeStreamer;
        this.tupleStream = tupleStream;
        this.facetConfig = facetConfig;
        this.pirResponseConverter = new PIRResponseConverter();
    }

    public abstract QueryResult<Pair<String, T>> getMappedEntries(
            IdMappingSearchRequest searchRequest);

    protected Stream<T> getEntries(List<String> toIds) {
        return this.storeStreamer.streamEntries(toIds);
    }

    public List<Object> streamEntries(IdMappingStreamRequest streamRequest) {
        return null; // TODO fill code
    }
}
