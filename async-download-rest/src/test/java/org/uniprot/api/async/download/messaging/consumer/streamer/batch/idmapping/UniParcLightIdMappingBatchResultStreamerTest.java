package org.uniprot.api.async.download.messaging.consumer.streamer.batch.idmapping;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.stream.StreamSupport;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.uniprot.api.async.download.messaging.consumer.heartbeat.idmapping.IdMappingHeartbeatProducer;
import org.uniprot.api.async.download.messaging.consumer.streamer.batch.IdMappingBatchResultStreamerTest;
import org.uniprot.api.async.download.service.idmapping.IdMappingJobService;
import org.uniprot.api.common.repository.stream.store.StoreStreamerConfig;
import org.uniprot.api.common.repository.stream.store.StreamerConfigProperties;
import org.uniprot.api.common.repository.stream.store.uniparc.UniParcCrossReferenceLazyLoader;
import org.uniprot.api.idmapping.common.response.model.UniParcEntryLightPair;
import org.uniprot.core.uniparc.UniParcEntryLight;
import org.uniprot.store.datastore.UniProtStoreClient;

import net.jodah.failsafe.RetryPolicy;

@ExtendWith(MockitoExtension.class)
class UniParcLightIdMappingBatchResultStreamerTest
        extends IdMappingBatchResultStreamerTest<UniParcEntryLight, UniParcEntryLightPair> {
    public static final int BATCH_SIZE = 2;
    @Mock private StoreStreamerConfig<UniParcEntryLight> storeStreamerConfig;
    @Mock private UniParcEntryLight uniParc1;
    @Mock private UniParcEntryLight uniParc2;
    @Mock private UniParcEntryLight uniParc3;
    @Mock private StreamerConfigProperties streamerConfigProperties;
    @Mock private UniProtStoreClient<UniParcEntryLight> storeClient;
    @Mock protected IdMappingJobService idMappingJobService;
    @Mock protected IdMappingHeartbeatProducer idMappingHeartbeatProducer;
    @Mock protected UniParcCrossReferenceLazyLoader lazyLoader;

    @BeforeEach
    void setUp() {
        heartbeatProducer = idMappingHeartbeatProducer;
        jobService = idMappingJobService;
        idMappingBatchResultStreamer =
                new UniParcLightIdMappingBatchResultStreamer(
                        idMappingHeartbeatProducer,
                        idMappingJobService,
                        storeStreamerConfig,
                        lazyLoader);
    }

    @Override
    protected Iterable<UniParcEntryLightPair> getEntryList() {
        return List.of(
                UniParcEntryLightPair.builder().from("from1").to(uniParc1).build(),
                UniParcEntryLightPair.builder().from("from2").to(uniParc2).build(),
                UniParcEntryLightPair.builder().from("from3").to(uniParc3).build());
    }

    @Override
    protected void mockBatch() {
        when(uniParc1.getUniParcId()).thenReturn("to1");
        when(uniParc2.getUniParcId()).thenReturn("to2");
        when(uniParc3.getUniParcId()).thenReturn("to3");
        when(storeStreamerConfig.getStreamConfig()).thenReturn(streamerConfigProperties);
        when(streamerConfigProperties.getStoreBatchSize()).thenReturn(BATCH_SIZE);
        when(storeStreamerConfig.getStoreFetchRetryPolicy()).thenReturn(new RetryPolicy<>());
        when(storeStreamerConfig.getStoreClient()).thenReturn(storeClient);
        when(storeClient.getEntries(any())).thenAnswer(this::getUniParcEntries);
    }

    @NotNull
    private List<UniParcEntryLight> getUniParcEntries(InvocationOnMock inv) {
        Iterable<String> strings = inv.getArgument(0);
        List<String> args = StreamSupport.stream(strings.spliterator(), false).toList();
        if (List.of("to1", "to2").containsAll(args) && args.size() == 2) {
            return List.of(uniParc1, uniParc2);
        }
        if (List.of("to3").containsAll(args) && args.size() == 1) {
            return List.of(uniParc3);
        }
        throw new IllegalArgumentException();
    }
}
