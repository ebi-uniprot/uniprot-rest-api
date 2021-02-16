package org.uniprot.api.idmapping.service;

import org.uniprot.api.common.repository.search.facet.FacetConfig;
import org.uniprot.api.common.repository.solrstream.FacetTupleStreamTemplate;
import org.uniprot.api.common.repository.stream.store.StoreStreamer;
import org.uniprot.core.uniref.UniRefEntry;

/**
 * @author sahmad
 * @created 16/02/2021
 */
public class UniRefIdService extends BasicIdService<UniRefEntry> {
    public UniRefIdService(
            IDMappingService idMappingService,
            StoreStreamer<UniRefEntry> storeStreamer,
            FacetTupleStreamTemplate tupleStream,
            FacetConfig facetConfig) { // TODO use UniRefFacetConfig
        super(idMappingService, storeStreamer, tupleStream, facetConfig);
    }
}
