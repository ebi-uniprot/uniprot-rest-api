package org.uniprot.api.async.download.messaging.consumer.streamer.batch.uniparc;

import static org.uniprot.api.rest.output.UniProtMediaType.EXTENDED_FASTA_MEDIA_TYPE;

import java.util.Iterator;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.uniprot.api.async.download.messaging.consumer.heartbeat.uniparc.UniParcHeartbeatProducer;
import org.uniprot.api.async.download.messaging.consumer.streamer.batch.SolrIdBatchResultStreamer;
import org.uniprot.api.async.download.model.job.uniparc.UniParcDownloadJob;
import org.uniprot.api.async.download.model.request.uniparc.UniParcDownloadRequest;
import org.uniprot.api.async.download.service.uniparc.UniParcJobService;
import org.uniprot.api.common.repository.stream.common.BatchIterable;
import org.uniprot.api.common.repository.stream.store.StoreStreamerConfig;
import org.uniprot.api.rest.output.UniProtMediaType;
import org.uniprot.api.uniparc.common.repository.store.stream.UniParcFastaBatchStoreIterable;
import org.uniprot.api.uniparc.common.service.light.UniParcCrossReferenceService;
import org.uniprot.core.uniparc.UniParcEntry;
import org.uniprot.core.uniparc.UniParcEntryLight;

@Component
public class UniParcExtendedSolrIdBatchResultStreamer
        extends SolrIdBatchResultStreamer<
                UniParcDownloadRequest, UniParcDownloadJob, UniParcEntry> {
    private final StoreStreamerConfig<UniParcEntryLight> storeStreamerConfig;
    private final UniParcCrossReferenceService uniParcCrossReferenceService;
    private final UniParcSolrIdBatchResultStreamer uniParcSolrIdBatchResultStreamer;

    public UniParcExtendedSolrIdBatchResultStreamer(
            UniParcHeartbeatProducer heartbeatProducer,
            UniParcJobService jobService,
            StoreStreamerConfig<UniParcEntryLight> storeLightStreamerConfig,
            UniParcCrossReferenceService uniParcCrossReferenceService,
            UniParcSolrIdBatchResultStreamer uniParcSolrIdBatchResultStreamer) {
        super(heartbeatProducer, jobService);
        this.storeStreamerConfig = storeLightStreamerConfig;
        this.uniParcCrossReferenceService = uniParcCrossReferenceService;
        this.uniParcSolrIdBatchResultStreamer = uniParcSolrIdBatchResultStreamer;
    }

    @Override
    public BatchIterable<UniParcEntry> getBatchStoreIterable(
            Iterator<String> idsIterator, UniParcDownloadRequest request) {
        MediaType contentType = UniProtMediaType.valueOf(request.getFormat());
        if (EXTENDED_FASTA_MEDIA_TYPE.equals(contentType)) {
            return new UniParcFastaBatchStoreIterable(
                    () -> idsIterator,
                    this.storeStreamerConfig.getStoreClient(),
                    this.uniParcCrossReferenceService,
                    this.storeStreamerConfig.getStoreFetchRetryPolicy(),
                    this.storeStreamerConfig.getStreamConfig().getStoreBatchSize(),
                    getProteomeId(request));
        }
        return uniParcSolrIdBatchResultStreamer.getBatchStoreIterable(idsIterator, request);
    }

    private static String getProteomeId(UniParcDownloadRequest request) {
        return request.getQuery().split(":")[1];
    }
}
