package org.uniprot.api.async.download.messaging.consumer.streamer.batch.uniprotkb;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.uniprot.api.async.download.messaging.consumer.heartbeat.uniprotkb.UniProtKBHeartbeatProducer;
import org.uniprot.api.async.download.messaging.consumer.streamer.batch.SolrIdBatchResultStreamerTest;
import org.uniprot.api.async.download.model.job.uniprotkb.UniProtKBDownloadJob;
import org.uniprot.api.async.download.model.request.uniprotkb.UniProtKBDownloadRequest;
import org.uniprot.api.async.download.service.uniprotkb.UniProtKBJobService;
import org.uniprot.api.common.repository.stream.store.StoreRequest;
import org.uniprot.api.common.repository.stream.store.StoreStreamerConfig;
import org.uniprot.api.common.repository.stream.store.StreamerConfigProperties;
import org.uniprot.api.common.repository.stream.store.uniprotkb.TaxonomyLineageService;
import org.uniprot.api.uniprotkb.common.repository.store.UniProtKBStoreClient;
import org.uniprot.api.uniprotkb.common.service.uniprotkb.UniProtEntryService;
import org.uniprot.core.uniprotkb.UniProtKBEntry;

import net.jodah.failsafe.RetryPolicy;

@ExtendWith(MockitoExtension.class)
public class UniProtKBSolrIdBatchResultStreamerTest
        extends SolrIdBatchResultStreamerTest<
                UniProtKBDownloadRequest, UniProtKBDownloadJob, UniProtKBEntry> {
    private static final int BATCH_SIZE = 2;
    public static final boolean ADD_LINEAGE = false;
    @Mock private UniProtKBDownloadRequest uniProtKBDownloadRequest;
    @Mock private UniProtKBDownloadJob uniProtKBDownloadJob;
    @Mock private UniProtKBHeartbeatProducer uniProtKBHeartbeatProducer;
    @Mock private UniProtKBJobService uniProtKBJobService;
    @Mock private UniProtEntryService uniProtEntryService;
    @Mock private TaxonomyLineageService lineageService;
    @Mock private StoreStreamerConfig<UniProtKBEntry> uniProtKBEntryStoreStreamerConfig;
    @Mock private StoreRequest uniprotKBStoreRequest;
    @Mock private UniProtKBStoreClient uniProtKBStoreClient;
    @Mock private StreamerConfigProperties streamerConfig;
    @Mock private UniProtKBEntry uniProtKB1;
    @Mock private UniProtKBEntry uniProtKB2;
    @Mock private UniProtKBEntry uniProtKB3;

    @BeforeEach
    void setUp() {
        request = uniProtKBDownloadRequest;
        job = uniProtKBDownloadJob;
        heartbeatProducer = uniProtKBHeartbeatProducer;
        jobService = uniProtKBJobService;
        solrIdBatchResultStreamer =
                new UniProtKBSolrIdBatchResultStreamer(
                        uniProtKBHeartbeatProducer,
                        uniProtKBJobService,
                        uniProtEntryService,
                        lineageService,
                        uniProtKBEntryStoreStreamerConfig);
    }

    @Override
    protected void mockBatch() {
        when(uniProtEntryService.getStoreRequest(uniProtKBDownloadRequest))
                .thenReturn(uniprotKBStoreRequest);
        when(uniprotKBStoreRequest.isAddLineage()).thenReturn(ADD_LINEAGE);
        when(uniProtKBEntryStoreStreamerConfig.getStoreClient()).thenReturn(uniProtKBStoreClient);
        when(uniProtKBEntryStoreStreamerConfig.getStoreFetchRetryPolicy())
                .thenReturn(new RetryPolicy<>());
        when(uniProtKBEntryStoreStreamerConfig.getStreamConfig()).thenReturn(streamerConfig);
        when(streamerConfig.getStoreBatchSize()).thenReturn(BATCH_SIZE);
        when(uniProtKBStoreClient.getEntries(any()))
                .thenAnswer(
                        inv -> {
                            Iterable<String> strings = inv.getArgument(0);
                            if (List.of("id1", "id2")
                                    .equals(
                                            StreamSupport.stream(strings.spliterator(), false)
                                                    .collect(Collectors.toList()))) {
                                return List.of(uniProtKB1, uniProtKB2);
                            }
                            if (List.of("id3")
                                    .equals(
                                            StreamSupport.stream(strings.spliterator(), false)
                                                    .collect(Collectors.toList()))) {
                                return List.of(uniProtKB3);
                            }
                            throw new IllegalArgumentException();
                        });
    }

    @Override
    protected Iterable<UniProtKBEntry> getEntryList() {
        return List.of(uniProtKB1, uniProtKB2, uniProtKB3);
    }
}
