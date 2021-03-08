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
import org.uniprot.api.idmapping.model.UniProtKBEntryPair;
import org.uniprot.api.idmapping.service.BasicIdService;
import org.uniprot.api.idmapping.service.store.impl.UniProtKBBatchStoreEntryPairIterable;
import org.uniprot.api.rest.respository.facet.impl.UniprotKBFacetConfig;
import org.uniprot.core.uniprotkb.UniProtKBEntry;
import org.uniprot.store.config.UniProtDataType;
import org.uniprot.store.config.searchfield.factory.SearchFieldConfigFactory;
import org.uniprot.store.datastore.UniProtStoreClient;

/**
 * @author sahmad
 * @created 16/02/2021
 */
@Service
public class UniProtKBIdService extends BasicIdService<UniProtKBEntry, UniProtKBEntryPair> {

    private final UniProtStoreClient<UniProtKBEntry> storeClient;

    private final RetryPolicy<Object> storeFetchRetryPolicy;

    private final StreamerConfigProperties streamConfig;

    public UniProtKBIdService(
            @Qualifier("uniProtKBEntryStoreStreamer") StoreStreamer<UniProtKBEntry> storeStreamer,
            @Qualifier("uniproKBfacetTupleStreamTemplate") FacetTupleStreamTemplate tupleStream,
            @Qualifier("uniProtKBStoreRetryPolicy") RetryPolicy<Object> storeFetchRetryPolicy,
            @Qualifier("uniProtKBStreamerConfigProperties") StreamerConfigProperties streamConfig,
            UniprotKBFacetConfig facetConfig,
            RDFStreamer uniProtKBRDFStreamer,
            UniProtStoreClient<UniProtKBEntry> storeClient,
            SolrQueryConfig uniProtKBSolrQueryConf) {
        super(
                storeStreamer,
                tupleStream,
                facetConfig,
                uniProtKBRDFStreamer,
                uniProtKBSolrQueryConf);
        this.streamConfig = streamConfig;
        this.storeClient = storeClient;
        this.storeFetchRetryPolicy = storeFetchRetryPolicy;
    }

    @Override
    protected UniProtKBEntryPair convertToPair(
            IdMappingStringPair mId, Map<String, UniProtKBEntry> idEntryMap) {
        return UniProtKBEntryPair.builder()
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
        return SearchFieldConfigFactory.getSearchFieldConfig(UniProtDataType.UNIPROTKB)
                .getSearchFieldItemByName("accession_id")
                .getFieldName();
    }

    @Override
    public UniProtDataType getUniProtDataType() {
        return UniProtDataType.UNIPROTKB;
    }

    @Override
    protected Stream<UniProtKBEntryPair> streamEntries(List<IdMappingStringPair> mappedIds) {
        UniProtKBBatchStoreEntryPairIterable batchIterable =
                new UniProtKBBatchStoreEntryPairIterable(
                        mappedIds,
                        streamConfig.getStoreBatchSize(),
                        storeClient,
                        storeFetchRetryPolicy);
        return StreamSupport.stream(batchIterable.spliterator(), false).flatMap(Collection::stream);
    }
}
