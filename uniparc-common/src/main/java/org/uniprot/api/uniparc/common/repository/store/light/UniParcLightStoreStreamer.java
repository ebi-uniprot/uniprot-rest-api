package org.uniprot.api.uniparc.common.repository.store.light;

import org.uniprot.api.common.repository.stream.store.BatchStoreIterable;
import org.uniprot.api.common.repository.stream.store.StoreRequest;
import org.uniprot.api.common.repository.stream.store.StoreStreamer;
import org.uniprot.api.common.repository.stream.store.StoreStreamerConfig;
import org.uniprot.api.uniparc.common.repository.store.crossref.UniParcCrossReferenceLazyLoader;
import org.uniprot.api.uniparc.common.repository.store.stream.UniParcLightBatchStoreIterable;
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
