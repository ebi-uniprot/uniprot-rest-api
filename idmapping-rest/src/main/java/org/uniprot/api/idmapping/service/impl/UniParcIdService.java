package org.uniprot.api.idmapping.service.impl;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import net.jodah.failsafe.RetryPolicy;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.uniprot.api.common.repository.search.SolrQueryConfig;
import org.uniprot.api.common.repository.solrstream.FacetTupleStreamTemplate;
import org.uniprot.api.common.repository.stream.rdf.RDFStreamer;
import org.uniprot.api.common.repository.stream.store.StoreStreamer;
import org.uniprot.api.common.repository.stream.store.StreamerConfigProperties;
import org.uniprot.api.idmapping.model.IdMappingStringPair;
import org.uniprot.api.idmapping.model.UniParcEntryPair;
import org.uniprot.api.idmapping.service.BasicIdService;
import org.uniprot.api.idmapping.service.store.impl.UniParcBatchStoreEntryPairIterable;
import org.uniprot.api.rest.respository.facet.impl.UniParcFacetConfig;
import org.uniprot.core.uniparc.UniParcEntry;
import org.uniprot.store.config.UniProtDataType;
import org.uniprot.store.config.searchfield.factory.SearchFieldConfigFactory;
import org.uniprot.store.datastore.UniProtStoreClient;

/**
 * @author sahmad
 * @created 16/02/2021
 */
@Service
public class UniParcIdService extends BasicIdService<UniParcEntry, UniParcEntryPair> {

    private final UniProtStoreClient<UniParcEntry> storeClient;

    private final RetryPolicy<Object> storeFetchRetryPolicy;

    private final StreamerConfigProperties streamConfig;

    public UniParcIdService(
            @Qualifier("uniParcEntryStoreStreamer") StoreStreamer<UniParcEntry> storeStreamer,
            @Qualifier("uniParcFacetTupleStreamTemplate") FacetTupleStreamTemplate tupleStream,
            @Qualifier("uniParcStreamerConfigProperties") StreamerConfigProperties streamConfig,
            @Qualifier("uniParcStoreRetryPolicy") RetryPolicy<Object> storeFetchRetryPolicy,
            UniParcFacetConfig facetConfig,
            RDFStreamer uniParcRDFStreamer,
            UniProtStoreClient<UniParcEntry> storeClient,
            SolrQueryConfig uniParcSolrQueryConf) {
        super(storeStreamer, tupleStream, facetConfig, uniParcRDFStreamer, uniParcSolrQueryConf);
        this.streamConfig = streamConfig;
        this.storeClient = storeClient;
        this.storeFetchRetryPolicy = storeFetchRetryPolicy;
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

    @Override
    protected Stream<UniParcEntryPair> streamEntries(List<IdMappingStringPair> mappedIds) {
        UniParcBatchStoreEntryPairIterable batchIterable =
                new UniParcBatchStoreEntryPairIterable(
                        mappedIds,
                        streamConfig.getStoreBatchSize(),
                        storeClient,
                        storeFetchRetryPolicy);
        return StreamSupport.stream(batchIterable.spliterator(), false).flatMap(Collection::stream);
    }
}
