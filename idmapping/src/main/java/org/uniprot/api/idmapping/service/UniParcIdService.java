package org.uniprot.api.idmapping.service;

import org.uniprot.api.common.repository.search.QueryResult;
import org.uniprot.api.common.repository.solrstream.FacetTupleStreamTemplate;
import org.uniprot.api.common.repository.stream.store.StoreStreamer;
import org.uniprot.api.idmapping.controller.request.UniProtKBIdMappingSearchRequest;
import org.uniprot.api.idmapping.model.StringUniProtKBEntryPair;
import org.uniprot.api.rest.respository.facet.impl.UniParcFacetConfig;
import org.uniprot.core.uniparc.UniParcEntry;

/**
 * @author sahmad
 * @created 16/02/2021
 */
public class UniParcIdService extends BasicIdService<UniParcEntry, StringUniProtKBEntryPair> {
    public UniParcIdService(
            IDMappingPIRService idMappingService,
            StoreStreamer<UniParcEntry> storeStreamer,
            FacetTupleStreamTemplate tupleStream,
            UniParcFacetConfig facetConfig) {
        super(idMappingService, storeStreamer, tupleStream, facetConfig);
    }

    @Override
    public QueryResult<StringUniProtKBEntryPair> getMappedEntries(
            UniProtKBIdMappingSearchRequest searchRequest) {
        return null;
    }

    @Override
    public String getFacetIdField() {
        return "upi";
    }
}
