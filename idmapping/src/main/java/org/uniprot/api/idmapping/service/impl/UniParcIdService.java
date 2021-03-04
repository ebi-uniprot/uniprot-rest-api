package org.uniprot.api.idmapping.service.impl;

import java.util.Map;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.uniprot.api.common.repository.solrstream.FacetTupleStreamTemplate;
import org.uniprot.api.common.repository.stream.rdf.RDFStreamer;
import org.uniprot.api.common.repository.stream.store.StoreStreamer;
import org.uniprot.api.idmapping.model.IdMappingStringPair;
import org.uniprot.api.idmapping.model.UniParcEntryPair;
import org.uniprot.api.idmapping.service.BasicIdService;
import org.uniprot.api.rest.respository.facet.impl.UniParcFacetConfig;
import org.uniprot.core.uniparc.UniParcEntry;
import org.uniprot.store.config.UniProtDataType;
import org.uniprot.store.config.searchfield.factory.SearchFieldConfigFactory;

/**
 * @author sahmad
 * @created 16/02/2021
 */
@Service
public class UniParcIdService extends BasicIdService<UniParcEntry, UniParcEntryPair> {
    public UniParcIdService(
            @Qualifier("uniParcEntryStoreStreamer") StoreStreamer<UniParcEntry> storeStreamer,
            @Qualifier("uniParcFacetTupleStreamTemplate") FacetTupleStreamTemplate tupleStream,
            UniParcFacetConfig facetConfig) {
        super(storeStreamer, tupleStream, facetConfig);
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
        return SearchFieldConfigFactory.getSearchFieldConfig(UniProtDataType.UNIPARC)
                .getSearchFieldItemByName("upi")
                .getFieldName();
    }

    @Override
    public UniProtDataType getUniProtDataType() {
        return UniProtDataType.UNIPARC;
    }

    @Override // TODO
    protected RDFStreamer getRDFStreamer() {
        return null;
    }
}
