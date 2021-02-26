package org.uniprot.api.idmapping.service.impl;

import java.util.Map;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.uniprot.api.common.repository.solrstream.FacetTupleStreamTemplate;
import org.uniprot.api.common.repository.stream.store.StoreStreamer;
import org.uniprot.api.idmapping.model.IdMappingStringPair;
import org.uniprot.api.idmapping.model.UniRefEntryPair;
import org.uniprot.api.idmapping.service.BasicIdService;
import org.uniprot.api.idmapping.service.IdMappingJobCacheService;
import org.uniprot.api.rest.respository.facet.impl.UniRefFacetConfig;
import org.uniprot.core.uniref.UniRefEntryLight;
import org.uniprot.store.config.UniProtDataType;

/**
 * @author sahmad
 * @created 16/02/2021
 */
@Service
public class UniRefIdService extends BasicIdService<UniRefEntryLight, UniRefEntryPair> {
    public UniRefIdService(
            IdMappingJobCacheService idMappingJobCacheService,
            @Qualifier("uniRefEntryStoreStreamer") StoreStreamer<UniRefEntryLight> storeStreamer,
            @Qualifier("uniRefFacetTupleStreamTemplate") FacetTupleStreamTemplate tupleStream,
            UniRefFacetConfig facetConfig) {
        super(storeStreamer, tupleStream, facetConfig);
    }

    @Override
    protected UniRefEntryPair convertToPair(
            IdMappingStringPair mId, Map<String, UniRefEntryLight> idEntryMap) {
        return UniRefEntryPair.builder()
                .from(mId.getFrom())
                .to(idEntryMap.get(mId.getTo()))
                .build();
    }

    @Override
    protected String getEntryId(UniRefEntryLight entry) {
        return entry.getId().getValue();
    }

    @Override
    protected String getSolrIdField() {
        // SearchFieldConfigFactory.getSearchFieldConfig(UniProtDataType.UNIPROTKB).getFieldTypeBySearchFieldName("accession_id").name();
        return "id";
    }

    @Override
    protected UniProtDataType getUniProtDataType() {
        return UniProtDataType.UNIREF;
    }
}
