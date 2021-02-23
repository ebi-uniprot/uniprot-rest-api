package org.uniprot.api.idmapping.service;

import java.util.Map;

import org.uniprot.api.common.repository.solrstream.FacetTupleStreamTemplate;
import org.uniprot.api.common.repository.stream.store.StoreStreamer;
import org.uniprot.api.idmapping.model.IdMappingStringPair;
import org.uniprot.api.idmapping.model.UniParcEntryPair;
import org.uniprot.api.rest.respository.facet.impl.UniParcFacetConfig;
import org.uniprot.core.uniparc.UniParcEntry;
import org.uniprot.store.config.UniProtDataType;

/**
 * @author sahmad
 * @created 16/02/2021
 */
public class UniParcIdService extends BasicIdService<UniParcEntry, UniParcEntryPair> {
    public UniParcIdService(
            IDMappingPIRService idMappingService,
            StoreStreamer<UniParcEntry> storeStreamer,
            FacetTupleStreamTemplate tupleStream,
            UniParcFacetConfig facetConfig) {
        super(idMappingService, storeStreamer, tupleStream, facetConfig);
    }

    @Override
    protected UniParcEntryPair convertToPair(
            IdMappingStringPair mId, Map<String, UniParcEntry> idEntryMap) {
        return UniParcEntryPair.builder()
                .from(mId.getFrom())
                .to(idEntryMap.get(mId.getTo()))
                .build();
    }

    @Override
    protected String getEntryId(UniParcEntry entry) {
        return entry.getUniParcId().getValue();
    }

    @Override
    protected String getSolrIdField() {
        return "upi";
    }

    @Override
    public UniProtDataType getUniProtDataType() {
        return UniProtDataType.UNIPARC;
    }
}
