package org.uniprot.api.common.repository.stream.store.uniprotkb;

import org.uniprot.api.common.repository.stream.store.BatchStoreIterable;
import org.uniprot.api.common.repository.stream.store.StoreRequest;
import org.uniprot.api.common.repository.stream.store.StoreStreamer;
import org.uniprot.api.common.repository.stream.store.StoreStreamerConfig;
import org.uniprot.core.uniprotkb.UniProtKBEntry;

public class UniProtKBStoreStreamer extends StoreStreamer<UniProtKBEntry> {

    private final TaxonomyLineageService lineageService;

    public UniProtKBStoreStreamer(
            StoreStreamerConfig<UniProtKBEntry> config, TaxonomyLineageService lineageService) {
        super(config);
        this.lineageService = lineageService;
    }

    @Override
    protected BatchStoreIterable<UniProtKBEntry> getBatchStoreIterable(
            Iterable<String> iterableIds, StoreRequest storeRequest) {
        return new UniProtKBBatchStoreIterable(
                iterableIds,
                config.getStoreClient(),
                config.getStoreFetchRetryPolicy(),
                config.getStreamConfig().getStoreBatchSize(),
                lineageService,
                storeRequest.isAddLineage());
    }
}
