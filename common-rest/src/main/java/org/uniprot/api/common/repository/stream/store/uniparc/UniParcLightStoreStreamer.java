package org.uniprot.api.common.repository.stream.store.uniparc;

import org.uniprot.api.common.repository.stream.store.BatchStoreIterable;
import org.uniprot.api.common.repository.stream.store.StoreRequest;
import org.uniprot.api.common.repository.stream.store.StoreStreamer;
import org.uniprot.api.common.repository.stream.store.StoreStreamerConfig;
import org.uniprot.core.uniparc.UniParcEntryLight;

public class UniParcLightStoreStreamer extends StoreStreamer<UniParcEntryLight> {

    private final UniParcCrossReferenceLazyLoader lazyLoader;

    public UniParcLightStoreStreamer(
            StoreStreamerConfig<UniParcEntryLight> config,
            UniParcCrossReferenceLazyLoader uniParcCrossReferenceLazyLoader) {
        super(config);
        this.lazyLoader = uniParcCrossReferenceLazyLoader;
    }

    @Override
    protected BatchStoreIterable<UniParcEntryLight> getBatchStoreIterable(
            Iterable<String> iterableIds, StoreRequest storeRequest) {
        return new UniParcLightBatchStoreIterable(
                iterableIds,
                config.getStoreClient(),
                config.getStoreFetchRetryPolicy(),
                config.getStreamConfig().getStoreBatchSize(),
                lazyLoader,
                storeRequest.getFields());
    }
}
