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
import org.uniprot.api.idmapping.common.response.model.UniParcEntryPair;
import org.uniprot.core.uniparc.UniParcEntry;
import org.uniprot.core.uniparc.impl.UniParcIdBuilder;
import org.uniprot.store.datastore.UniProtStoreClient;

import java.util.List;
import java.util.stream.StreamSupport;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UniParcIdMappingBatchResultStreamerTest extends IdMappingBatchResultStreamerTest<UniParcEntry, UniParcEntryPair> {
    public static final int BATCH_SIZE = 2;
    @Mock
    private StoreStreamerConfig<UniParcEntry> storeStreamerConfig;
    @Mock
    private UniParcEntry uniParc1;
    @Mock
    private UniParcEntry uniParc2;
    @Mock
    private UniParcEntry uniParc3;
    @Mock
    private StreamerConfigProperties streamerConfigProperties;
    @Mock
    private UniProtStoreClient<UniParcEntry> storeClient;
    @Mock
    protected IdMappingJobService idMappingJobService;
    @Mock
    protected IdMappingHeartbeatProducer idMappingHeartbeatProducer;

    @BeforeEach
    void setUp() {
        heartbeatProducer = idMappingHeartbeatProducer;
        jobService = idMappingJobService;
        idMappingBatchResultStreamer =
                new UniParcIdMappingBatchResultStreamer(
                        idMappingHeartbeatProducer, idMappingJobService, storeStreamerConfig);
    }

    @Override
    protected Iterable<UniParcEntryPair> getEntryList() {
        return List.of(UniParcEntryPair.builder().from("from1").to(uniParc1).build(), UniParcEntryPair.builder().from("from2").to(uniParc2).build(), UniParcEntryPair.builder().from("from3").to(uniParc3).build());
    }

    @Override
    protected void mockBatch() {
        when(uniParc1.getUniParcId()).thenReturn(new UniParcIdBuilder("to1").build());
        when(uniParc2.getUniParcId()).thenReturn(new UniParcIdBuilder("to2").build());
        when(uniParc3.getUniParcId()).thenReturn(new UniParcIdBuilder("to3").build());
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
                                return List.of(uniParc1, uniParc2);
                            }
                            if (List.of("to3").containsAll(args) && args.size() == 1) {
                                return List.of(uniParc3);
                            }
                            throw new IllegalArgumentException();
                        });
    }
}
