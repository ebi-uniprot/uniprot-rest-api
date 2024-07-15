package org.uniprot.api.uniparc.common.repository.store.light;

import org.uniprot.api.common.repository.stream.store.BatchStoreIterable;
import org.uniprot.api.common.repository.stream.store.StoreRequest;
import org.uniprot.api.common.repository.stream.store.StoreStreamer;
import org.uniprot.api.common.repository.stream.store.StoreStreamerConfig;
import org.uniprot.api.uniparc.common.repository.store.stream.UniParcLightBatchStoreIterable;
import org.uniprot.core.uniparc.UniParcCrossReference;
import org.uniprot.core.uniparc.UniParcEntryLight;

public class UniParcLightStoreStreamer extends StoreStreamer<UniParcEntryLight> {

    private final StoreStreamerConfig<UniParcCrossReference> crossRefenceConfig;

    public UniParcLightStoreStreamer(
            StoreStreamerConfig<UniParcEntryLight> config,
            StoreStreamerConfig<UniParcCrossReference> crossRefenceConfig) {
        super(config);
        this.crossRefenceConfig = crossRefenceConfig;
    }

    @Override
    protected BatchStoreIterable<UniParcEntryLight> getBatchStoreIterable(
            Iterable<String> iterableIds, StoreRequest storeRequest) {
        return new UniParcLightBatchStoreIterable(
                iterableIds,
                config.getStoreClient(),
                config.getStoreFetchRetryPolicy(),
                config.getStreamConfig().getStoreBatchSize(),
                crossRefenceConfig.getStoreClient(),
                storeRequest.getFields());
    }
}
