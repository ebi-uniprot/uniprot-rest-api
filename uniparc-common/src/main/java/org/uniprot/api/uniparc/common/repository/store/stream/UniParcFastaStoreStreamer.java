package org.uniprot.api.uniparc.common.repository.store.stream;

import org.uniprot.api.common.repository.stream.store.BatchStoreIterable;
import org.uniprot.api.common.repository.stream.store.StoreRequest;
import org.uniprot.api.common.repository.stream.store.StoreStreamer;
import org.uniprot.api.common.repository.stream.store.StoreStreamerConfig;
import org.uniprot.api.common.repository.stream.store.uniparc.UniParcCrossReferenceStoreConfigProperties;
import org.uniprot.core.uniparc.UniParcEntry;
import org.uniprot.core.uniparc.UniParcEntryLight;
import org.uniprot.core.uniparc.impl.UniParcCrossReferencePair;
import org.uniprot.store.datastore.UniProtStoreClient;

public class UniParcFastaStoreStreamer extends StoreStreamer<UniParcEntry> {

    private final StoreStreamerConfig<UniParcEntryLight> lightConfig;

    private final UniProtStoreClient<UniParcCrossReferencePair> xrefClient;

    private final UniParcCrossReferenceStoreConfigProperties storeConfigProperties;

    public UniParcFastaStoreStreamer(
            StoreStreamerConfig<UniParcEntry> config,
            StoreStreamerConfig<UniParcEntryLight> lightConfig,
            UniProtStoreClient<UniParcCrossReferencePair> xrefClient,
            UniParcCrossReferenceStoreConfigProperties storeConfigProperties) {
        super(config);
        this.lightConfig = lightConfig;
        this.xrefClient = xrefClient;
        this.storeConfigProperties = storeConfigProperties;
    }

    @Override
    protected BatchStoreIterable<UniParcEntry> getBatchStoreIterable(
            Iterable<String> iterableIds, StoreRequest storeRequest) {
        return new UniParcFastaBatchStoreIterable(
                iterableIds,
                lightConfig.getStoreClient(),
                xrefClient,
                storeConfigProperties,
                lightConfig.getStoreFetchRetryPolicy(),
                lightConfig.getStreamConfig().getStoreBatchSize(),
                storeRequest.getProteomeId());
    }
}
