package org.uniprot.api.async.download.refactor.consumer.streamer.batch.uniprotkb;

import org.springframework.stereotype.Component;
import org.uniprot.api.async.download.messaging.listener.uniprotkb.UniProtKBHeartbeatProducer;
import org.uniprot.api.async.download.model.uniprotkb.UniProtKBDownloadJob;
import org.uniprot.api.async.download.refactor.request.uniprotkb.UniProtKBDownloadRequest;
import org.uniprot.api.async.download.refactor.service.uniprotkb.UniProtKBJobService;
import org.uniprot.api.async.download.refactor.consumer.streamer.batch.BatchResultStreamer;
import org.uniprot.api.common.repository.stream.store.BatchStoreIterable;
import org.uniprot.api.common.repository.stream.store.StoreRequest;
import org.uniprot.api.common.repository.stream.store.StoreStreamerConfig;
import org.uniprot.api.common.repository.stream.store.uniprotkb.TaxonomyLineageService;
import org.uniprot.api.common.repository.stream.store.uniprotkb.UniProtKBBatchStoreIterable;
import org.uniprot.api.uniprotkb.common.service.uniprotkb.UniProtEntryService;
import org.uniprot.core.uniprotkb.UniProtKBEntry;

import java.util.Iterator;

@Component
public class UniProtKBBatchResultStreamer extends BatchResultStreamer<UniProtKBDownloadRequest, UniProtKBDownloadJob, UniProtKBEntry> {
    private final UniProtEntryService service;
    private final TaxonomyLineageService lineageService;
    private final StoreStreamerConfig<UniProtKBEntry> storeStreamerConfig;

    public UniProtKBBatchResultStreamer(UniProtKBHeartbeatProducer heartbeatProducer, UniProtKBJobService jobService, UniProtEntryService service, TaxonomyLineageService lineageService, StoreStreamerConfig<UniProtKBEntry> storeStreamerConfig) {
        super(heartbeatProducer, jobService);
        this.service = service;
        this.lineageService = lineageService;
        this.storeStreamerConfig = storeStreamerConfig;
    }

    @Override
    protected BatchStoreIterable<UniProtKBEntry> getBatchStoreIterable(
            Iterator<String> idsIterator, UniProtKBDownloadRequest request) {
        return new UniProtKBBatchStoreIterable(
                idsIterator,
                storeStreamerConfig.getStoreClient(),
                storeStreamerConfig.getStoreFetchRetryPolicy(),
                storeStreamerConfig.getStreamConfig().getStoreBatchSize(),
                lineageService, service.buildStoreRequest(request).isAddLineage());
    }
}