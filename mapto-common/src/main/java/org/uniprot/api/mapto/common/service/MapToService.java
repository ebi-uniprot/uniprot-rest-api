package org.uniprot.api.mapto.common.service;

import java.util.List;

import org.uniprot.api.common.repository.search.QueryResult;
import org.uniprot.api.common.repository.search.facet.FacetConfig;
import org.uniprot.api.common.repository.solrstream.FacetTupleStreamTemplate;
import org.uniprot.api.common.repository.stream.store.StoreStreamer;
import org.uniprot.api.idmapping.common.service.AbstractIdService;
import org.uniprot.api.rest.request.SearchRequest;
import org.uniprot.api.rest.service.request.RequestConverter;

public abstract class MapToService<T> extends AbstractIdService<T> {
    private final StoreStreamer<T> storeStreamer;
    private final RequestConverter requestConverter;

    protected MapToService(
            StoreStreamer<T> storeStreamer,
            FacetTupleStreamTemplate tupleStream,
            FacetConfig facetConfig,
            RequestConverter requestConverter) {
        // TODO
        super(null, null, null, null);
        this.storeStreamer = storeStreamer;
        this.requestConverter = requestConverter;
    }

    public QueryResult<T> getTargetEntries(List<String> ids, SearchRequest request) {
        return null;
    }
}
