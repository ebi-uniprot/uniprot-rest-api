package org.uniprot.api.async.download.refactor.consumer.streamer.batch.idmapping;

import net.jodah.failsafe.RetryPolicy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.uniprot.api.async.download.messaging.listener.idmapping.IdMappingHeartbeatProducer;
import org.uniprot.api.async.download.refactor.consumer.streamer.batch.IdMappingBatchResultStreamerTest;
import org.uniprot.api.async.download.refactor.service.idmapping.IdMappingJobService;
import org.uniprot.api.common.repository.stream.store.StoreStreamerConfig;
import org.uniprot.api.common.repository.stream.store.StreamerConfigProperties;
import org.uniprot.api.idmapping.common.response.model.UniRefEntryPair;
import org.uniprot.core.uniref.UniRefEntryLight;
import org.uniprot.core.uniref.impl.UniRefEntryIdBuilder;
import org.uniprot.store.datastore.UniProtStoreClient;

import java.util.List;
import java.util.stream.StreamSupport;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UniRefIdMappingBatchResultStreamerTest extends IdMappingBatchResultStreamerTest<UniRefEntryLight, UniRefEntryPair> {
    public static final int BATCH_SIZE = 2;
    @Mock
    private StoreStreamerConfig<UniRefEntryLight> storeStreamerConfig;
    @Mock
    private UniRefEntryLight uniRef1;
    @Mock
    private UniRefEntryLight uniRef2;
    @Mock
    private UniRefEntryLight uniRef3;
    @Mock
    private StreamerConfigProperties streamerConfigProperties;
    @Mock
    private UniProtStoreClient<UniRefEntryLight> storeClient;
    @Mock
    protected IdMappingJobService idMappingJobService;
    @Mock
    protected IdMappingHeartbeatProducer idMappingHeartbeatProducer;

    @BeforeEach
    void setUp() {
        heartbeatProducer = idMappingHeartbeatProducer;
        jobService = idMappingJobService;
        idMappingBatchResultStreamer =
                new UniRefIdMappingBatchResultStreamer(
                        idMappingHeartbeatProducer, idMappingJobService, storeStreamerConfig);
    }

    @Override
    protected Iterable<UniRefEntryPair> getEntryList() {
        return List.of(UniRefEntryPair.builder().from("from1").to(uniRef1).build(), UniRefEntryPair.builder().from("from2").to(uniRef2).build(), UniRefEntryPair.builder().from("from3").to(uniRef3).build());
    }

    @Override
    protected void mockBatch() {
        when(uniRef1.getId()).thenReturn(new UniRefEntryIdBuilder("to1").build());
        when(uniRef2.getId()).thenReturn(new UniRefEntryIdBuilder("to2").build());
        when(uniRef3.getId()).thenReturn(new UniRefEntryIdBuilder("to3").build());
        when(storeStreamerConfig.getStreamConfig()).thenReturn(streamerConfigProperties);
        when(streamerConfigProperties.getStoreBatchSize()).thenReturn(BATCH_SIZE);
        when(storeStreamerConfig.getStoreFetchRetryPolicy()).thenReturn(new RetryPolicy<>());
        when(storeStreamerConfig.getStoreClient()).thenReturn(storeClient);
        when(storeClient.getEntries(any()))
                .thenAnswer(
                        inv -> {
                            Iterable<String> strings = inv.getArgument(0);
                            List<String> args = StreamSupport.stream(strings.spliterator(), false).toList();
                            if (List.of("to1", "to2").containsAll(args) && args.size() == 2) {
                                return List.of(uniRef1, uniRef2);
                            }
                            if (List.of("to3").containsAll(args) && args.size() == 1) {
                                return List.of(uniRef3);
                            }
                            throw new IllegalArgumentException();
                        });
    }
}
