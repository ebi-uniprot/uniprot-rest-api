package org.uniprot.api.async.download.messaging.consumer.streamer.batch.map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.uniprot.api.async.download.model.request.mapto.UniProtKBToUniRefDownloadRequest;
import org.uniprot.core.uniref.UniRefEntryLight;

import net.jodah.failsafe.RetryPolicy;

@ExtendWith(MockitoExtension.class)
public class UniProtKBToUniRefSolrIdBatchResultStreamerTest
        extends MapSolrIdBatchResultStreamerTest<
                UniProtKBToUniRefDownloadRequest, UniRefEntryLight> {
    @Mock private UniProtKBToUniRefDownloadRequest uniParcDownloadRequest;
    @Mock private UniRefEntryLight uniRef1;
    @Mock private UniRefEntryLight uniRef2;
    @Mock private UniRefEntryLight uniRef3;

    @BeforeEach
    void setUp() {
        init();
        request = uniParcDownloadRequest;
        job = mapToDownloadJob;
        solrIdBatchResultStreamer =
                new UniProtKBToUniRefSolrIdBatchResultStreamer(
                        mapToHeartbeatProducer, mapToJobService, uniRefEntryStoreStreamerConfig);
    }

    @Override
    protected void mockBatch() {
        when(uniRefEntryStoreStreamerConfig.getStoreClient()).thenReturn(uniRefLightStoreClient);
        when(uniRefEntryStoreStreamerConfig.getStoreFetchRetryPolicy())
                .thenReturn(new RetryPolicy<>());
        when(uniRefEntryStoreStreamerConfig.getStreamConfig()).thenReturn(streamerConfig);
        when(streamerConfig.getStoreBatchSize()).thenReturn(BATCH_SIZE);
        when(uniRefLightStoreClient.getEntries(any()))
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
