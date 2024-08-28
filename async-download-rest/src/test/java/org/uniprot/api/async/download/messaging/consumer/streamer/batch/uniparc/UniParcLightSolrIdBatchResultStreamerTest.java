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
import org.uniprot.api.common.repository.stream.store.uniparc.UniParcCrossReferenceLazyLoader;
import org.uniprot.api.uniparc.common.repository.store.light.UniParcLightStoreClient;
import org.uniprot.core.uniparc.UniParcEntryLight;

import net.jodah.failsafe.RetryPolicy;

@ExtendWith(MockitoExtension.class)
public class UniParcLightSolrIdBatchResultStreamerTest
        extends SolrIdBatchResultStreamerTest<
                UniParcDownloadRequest, UniParcDownloadJob, UniParcEntryLight> {
    private static final int BATCH_SIZE = 2;
    @Mock private UniParcDownloadRequest uniParcDownloadRequest;
    @Mock private UniParcDownloadJob uniParcDownloadJob;
    @Mock private UniParcHeartbeatProducer uniParcHeartbeatProducer;
    @Mock private UniParcJobService uniParcJobService;
    @Mock private StoreStreamerConfig<UniParcEntryLight> uniParcEntryStoreStreamerConfig;
    @Mock private UniParcLightStoreClient uniParcStoreClient;
    @Mock private StreamerConfigProperties streamerConfig;
    @Mock private UniParcEntryLight uniParc1;
    @Mock private UniParcEntryLight uniParc2;
    @Mock private UniParcEntryLight uniParc3;
    @Mock private UniParcCrossReferenceLazyLoader lazyLoader;

    @BeforeEach
    void setUp() {
        request = uniParcDownloadRequest;
        job = uniParcDownloadJob;
        heartbeatProducer = uniParcHeartbeatProducer;
        jobService = uniParcJobService;
        solrIdBatchResultStreamer =
                new UniParcLightSolrIdBatchResultStreamer(
                        uniParcHeartbeatProducer,
                        uniParcJobService,
                        uniParcEntryStoreStreamerConfig,
                        lazyLoader);
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
    protected Iterable<UniParcEntryLight> getEntryList() {
        return List.of(uniParc1, uniParc2, uniParc3);
    }
}
