package org.uniprot.api.idmapping.service;

import org.uniprot.api.common.repository.search.facet.FacetConfig;
import org.uniprot.api.common.repository.solrstream.FacetTupleStreamTemplate;
import org.uniprot.api.common.repository.stream.store.StoreStreamer;
import org.uniprot.core.uniprotkb.UniProtKBEntry;

/**
 * @author sahmad
 * @created 16/02/2021
 */
public class UniProtKBIdService extends BasicIdService<UniProtKBEntry> {

    public UniProtKBIdService(
            IDMappingPIRService idMappingService,
            StoreStreamer<UniProtKBEntry> storeStreamer,
            FacetTupleStreamTemplate tupleStream,
            FacetConfig facetConfig) { // TODO Use UniprotKBFacetConfig
        super(idMappingService, storeStreamer, tupleStream, facetConfig);
    }
}
