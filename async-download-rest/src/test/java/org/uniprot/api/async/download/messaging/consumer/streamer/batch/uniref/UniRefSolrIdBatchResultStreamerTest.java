package org.uniprot.api.async.download.messaging.consumer.streamer.batch.uniref;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.uniprot.api.async.download.messaging.consumer.heartbeat.uniref.UniRefHeartbeatProducer;
import org.uniprot.api.async.download.messaging.consumer.streamer.batch.SolrIdBatchResultStreamerTest;
import org.uniprot.api.async.download.model.job.uniref.UniRefDownloadJob;
import org.uniprot.api.async.download.model.request.uniref.UniRefDownloadRequest;
import org.uniprot.api.async.download.service.uniref.UniRefJobService;
import org.uniprot.api.common.repository.stream.store.StoreStreamerConfig;
import org.uniprot.api.common.repository.stream.store.StreamerConfigProperties;
import org.uniprot.api.uniref.common.repository.store.UniRefLightStoreClient;
import org.uniprot.core.uniref.UniRefEntryLight;

import net.jodah.failsafe.RetryPolicy;

@ExtendWith(MockitoExtension.class)
public class UniRefSolrIdBatchResultStreamerTest
        extends SolrIdBatchResultStreamerTest<
                UniRefDownloadRequest, UniRefDownloadJob, UniRefEntryLight> {
    private static final int BATCH_SIZE = 2;
    @Mock private UniRefDownloadRequest uniRefDownloadRequest;
    @Mock private UniRefDownloadJob uniRefDownloadJob;
    @Mock private UniRefHeartbeatProducer uniRefHeartbeatProducer;
    @Mock private UniRefJobService uniRefJobService;
    @Mock private StoreStreamerConfig<UniRefEntryLight> uniRefEntryStoreStreamerConfig;
    @Mock private UniRefLightStoreClient uniRefStoreClient;
    @Mock private StreamerConfigProperties streamerConfig;
    @Mock private UniRefEntryLight uniRef1;
    @Mock private UniRefEntryLight uniRef2;
    @Mock private UniRefEntryLight uniRef3;

    @BeforeEach
    void setUp() {
        request = uniRefDownloadRequest;
        job = uniRefDownloadJob;
        heartbeatProducer = uniRefHeartbeatProducer;
        jobService = uniRefJobService;
        solrIdBatchResultStreamer =
                new UniRefSolrIdBatchResultStreamer(
                        uniRefHeartbeatProducer, uniRefJobService, uniRefEntryStoreStreamerConfig);
    }

    @Override
    protected void mockBatch() {
        when(uniRefEntryStoreStreamerConfig.getStoreClient()).thenReturn(uniRefStoreClient);
        when(uniRefEntryStoreStreamerConfig.getStoreFetchRetryPolicy())
                .thenReturn(new RetryPolicy<>());
        when(uniRefEntryStoreStreamerConfig.getStreamConfig()).thenReturn(streamerConfig);
        when(streamerConfig.getStoreBatchSize()).thenReturn(BATCH_SIZE);
        when(uniRefStoreClient.getEntries(any()))
                .thenAnswer(
                        inv -> {
                            Iterable<String> strings = inv.getArgument(0);
                            if (List.of("id1", "id2")
                                    .equals(
                                            StreamSupport.stream(strings.spliterator(), false)
                                                    .collect(Collectors.toList()))) {
                                return List.of(uniRef1, uniRef2);
                            }
                            if (List.of("id3")
                                    .equals(
                                            StreamSupport.stream(strings.spliterator(), false)
                                                    .collect(Collectors.toList()))) {
                                return List.of(uniRef3);
                            }
                            throw new IllegalArgumentException();
                        });
    }

    @Override
    protected Iterable<UniRefEntryLight> getEntryList() {
        return List.of(uniRef1, uniRef2, uniRef3);
    }
}
