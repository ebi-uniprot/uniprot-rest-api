package org.uniprot.api.async.download.messaging.consumer.streamer.batch.uniparc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.uniprot.api.async.download.messaging.consumer.heartbeat.uniparc.UniParcHeartbeatProducer;
import org.uniprot.api.async.download.messaging.consumer.streamer.batch.SolrIdBatchResultStreamerTest;
import org.uniprot.api.async.download.model.job.uniparc.UniParcDownloadJob;
import org.uniprot.api.async.download.model.request.uniparc.UniParcDownloadRequest;
import org.uniprot.api.async.download.service.uniparc.UniParcJobService;
import org.uniprot.api.common.repository.stream.store.StoreStreamerConfig;
import org.uniprot.api.common.repository.stream.store.StreamerConfigProperties;
import org.uniprot.api.uniparc.common.repository.store.entry.UniParcStoreClient;
import org.uniprot.core.uniparc.UniParcEntry;

import net.jodah.failsafe.RetryPolicy;

@ExtendWith(MockitoExtension.class)
public class UniParcSolrIdBatchResultStreamerTest
        extends SolrIdBatchResultStreamerTest<
                UniParcDownloadRequest, UniParcDownloadJob, UniParcEntry> {
    private static final int BATCH_SIZE = 2;
    @Mock private UniParcDownloadRequest uniParcDownloadRequest;
    @Mock private UniParcDownloadJob uniParcDownloadJob;
    @Mock private UniParcHeartbeatProducer uniParcHeartbeatProducer;
    @Mock private UniParcJobService uniParcJobService;
    @Mock private StoreStreamerConfig<UniParcEntry> uniParcEntryStoreStreamerConfig;
    @Mock private UniParcStoreClient uniParcStoreClient;
    @Mock private StreamerConfigProperties streamerConfig;
    @Mock private UniParcEntry uniParc1;
    @Mock private UniParcEntry uniParc2;
    @Mock private UniParcEntry uniParc3;

    @BeforeEach
    void setUp() {
        request = uniParcDownloadRequest;
        job = uniParcDownloadJob;
        heartbeatProducer = uniParcHeartbeatProducer;
        jobService = uniParcJobService;
        solrIdBatchResultStreamer =
                new UniParcSolrIdBatchResultStreamer(
                        uniParcHeartbeatProducer,
                        uniParcJobService,
                        uniParcEntryStoreStreamerConfig);
    }

    @Override
    protected void mockBatch() {
        when(uniParcEntryStoreStreamerConfig.getStoreClient()).thenReturn(uniParcStoreClient);
        when(uniParcEntryStoreStreamerConfig.getStoreFetchRetryPolicy())
                .thenReturn(new RetryPolicy<>());
        when(uniParcEntryStoreStreamerConfig.getStreamConfig()).thenReturn(streamerConfig);
        when(streamerConfig.getStoreBatchSize()).thenReturn(BATCH_SIZE);
        when(uniParcStoreClient.getEntries(any()))
                .thenAnswer(
                        inv -> {
                            Iterable<String> strings = inv.getArgument(0);
                            if (List.of("id1", "id2")
                                    .equals(
                                            StreamSupport.stream(strings.spliterator(), false)
                                                    .collect(Collectors.toList()))) {
                                return List.of(uniParc1, uniParc2);
                            }
                            if (List.of("id3")
                                    .equals(
                                            StreamSupport.stream(strings.spliterator(), false)
                                                    .collect(Collectors.toList()))) {
                                return List.of(uniParc3);
                            }
                            throw new IllegalArgumentException();
                        });
    }

    @Override
    protected Iterable<UniParcEntry> getEntryList() {
        return List.of(uniParc1, uniParc2, uniParc3);
    }
}
