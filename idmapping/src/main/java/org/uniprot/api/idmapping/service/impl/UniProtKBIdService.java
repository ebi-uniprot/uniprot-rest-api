package org.uniprot.api.idmapping.service.impl;

import java.util.Map;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.uniprot.api.common.repository.solrstream.FacetTupleStreamTemplate;
import org.uniprot.api.common.repository.stream.store.StoreStreamer;
import org.uniprot.api.idmapping.model.IdMappingStringPair;
import org.uniprot.api.idmapping.model.UniProtKbEntryPair;
import org.uniprot.api.idmapping.service.BasicIdService;
import org.uniprot.api.idmapping.service.IdMappingJobCacheService;
import org.uniprot.api.rest.respository.facet.impl.UniprotKBFacetConfig;
import org.uniprot.core.uniprotkb.UniProtKBEntry;
import org.uniprot.store.config.UniProtDataType;

/**
 * @author sahmad
 * @created 16/02/2021
 */
@Service
public class UniProtKBIdService extends BasicIdService<UniProtKBEntry, UniProtKbEntryPair> {

    public UniProtKBIdService(
            IdMappingJobCacheService idMappingJobCacheService,
            @Qualifier("uniProtKBEntryStoreStreamer") StoreStreamer<UniProtKBEntry> storeStreamer,
            @Qualifier("uniproKBfacetTupleStreamTemplate") FacetTupleStreamTemplate tupleStream,
            UniprotKBFacetConfig facetConfig) {
        super(storeStreamer, tupleStream, facetConfig);
    }

    @Override
    protected UniProtKbEntryPair convertToPair(
            IdMappingStringPair mId, Map<String, UniProtKBEntry> idEntryMap) {
        return UniProtKbEntryPair.builder()
                .from(mId.getFrom())
                .to(idEntryMap.get(mId.getTo()))
                .build();
    }

    @Override
    protected String getEntryId(UniProtKBEntry entry) {
        return entry.getPrimaryAccession().getValue();
    }

    @Override
    public String getSolrIdField() {
        // SearchFieldConfigFactory.getSearchFieldConfig(UniProtDataType.UNIPROTKB).getFieldTypeBySearchFieldName("accession_id").name();
        return "accession_id";
    }

    @Override
    public UniProtDataType getUniProtDataType() {
        return UniProtDataType.UNIPROTKB;
    }
}
