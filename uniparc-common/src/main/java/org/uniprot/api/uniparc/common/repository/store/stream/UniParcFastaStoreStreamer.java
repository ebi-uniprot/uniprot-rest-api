package org.uniprot.api.uniparc.common.repository.store.stream;

import org.uniprot.api.common.repository.stream.store.BatchStoreIterable;
import org.uniprot.api.common.repository.stream.store.StoreRequest;
import org.uniprot.api.common.repository.stream.store.StoreStreamer;
import org.uniprot.api.common.repository.stream.store.StoreStreamerConfig;
import org.uniprot.api.uniparc.common.service.light.UniParcCrossReferenceService;
import org.uniprot.core.uniparc.UniParcEntry;
import org.uniprot.core.uniparc.UniParcEntryLight;

public class UniParcFastaStoreStreamer extends StoreStreamer<UniParcEntry> {

    private final StoreStreamerConfig<UniParcEntryLight> lightConfig;

    private final UniParcCrossReferenceService uniParcCrossReferenceService;

    public UniParcFastaStoreStreamer(
            StoreStreamerConfig<UniParcEntry> config,
            StoreStreamerConfig<UniParcEntryLight> lightConfig,
            UniParcCrossReferenceService uniParcCrossReferenceService) {
        super(config);
        this.lightConfig = lightConfig;
        this.uniParcCrossReferenceService = uniParcCrossReferenceService;
    }

    @Override
    protected BatchStoreIterable<UniParcEntry> getBatchStoreIterable(
            Iterable<String> iterableIds, StoreRequest storeRequest) {
        return new UniParcFastaBatchStoreIterable(
                iterableIds,
                lightConfig.getStoreClient(),
                uniParcCrossReferenceService,
                lightConfig.getStoreFetchRetryPolicy(),
                lightConfig.getStreamConfig().getStoreBatchSize(),
                storeRequest.getProteomeId());
    }
}
