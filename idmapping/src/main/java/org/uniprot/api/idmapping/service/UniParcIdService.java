package org.uniprot.api.idmapping.service;

import org.uniprot.api.common.repository.search.facet.FacetConfig;
import org.uniprot.api.common.repository.solrstream.FacetTupleStreamTemplate;
import org.uniprot.api.common.repository.stream.store.StoreStreamer;
import org.uniprot.core.uniparc.UniParcEntry;

/**
 * @author sahmad
 * @created 16/02/2021
 */
public class UniParcIdService extends BasicIdService<UniParcEntry> {
    public UniParcIdService(
            IDMappingPIRService idMappingService,
            StoreStreamer<UniParcEntry> storeStreamer,
            FacetTupleStreamTemplate tupleStream,
            FacetConfig facetConfig) { // TODO specific FacetConfig e.g. UniParcFacetConfig,
        // we may need to move UniParcFacetConfig to common
        super(idMappingService, storeStreamer, tupleStream, facetConfig);
    }
}
