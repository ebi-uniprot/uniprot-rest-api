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
import org.uniprot.api.common.repository.stream.store.uniprotkb.TaxonomyLineageService;
import org.uniprot.api.idmapping.common.repository.UniprotKBMappingRepository;
import org.uniprot.api.idmapping.common.response.model.UniProtKBEntryPair;
import org.uniprot.core.uniprotkb.UniProtKBEntry;
import org.uniprot.core.uniprotkb.impl.UniProtKBAccessionBuilder;
import org.uniprot.store.datastore.UniProtStoreClient;

import java.util.List;
import java.util.stream.StreamSupport;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UniProtKBIdMappingBatchResultStreamerTest extends IdMappingBatchResultStreamerTest<UniProtKBEntry, UniProtKBEntryPair> {
    public static final int BATCH_SIZE = 2;
    @Mock
    private StoreStreamerConfig<UniProtKBEntry> storeStreamerConfig;
    @Mock
    private UniProtKBEntry uniProtKB1;
    @Mock
    private UniProtKBEntry uniProtKB2;
    @Mock
    private UniProtKBEntry uniProtKB3;
    @Mock
    private StreamerConfigProperties streamerConfigProperties;
    @Mock
    private UniProtStoreClient<UniProtKBEntry> storeClient;
    @Mock
    protected IdMappingJobService idMappingJobService;
    @Mock
    protected IdMappingHeartbeatProducer idMappingHeartbeatProducer;
    @Mock
    private TaxonomyLineageService lineageService;
    @Mock
    private UniprotKBMappingRepository uniprotKBMappingRepository;

    @BeforeEach
    void setUp() {
        heartbeatProducer = idMappingHeartbeatProducer;
        jobService = idMappingJobService;
        idMappingBatchResultStreamer =
                new UniProtKBIdMappingBatchResultStreamer(
                        idMappingHeartbeatProducer, idMappingJobService, storeStreamerConfig, lineageService, uniprotKBMappingRepository);
    }

    @Override
    protected Iterable<UniProtKBEntryPair> getEntryList() {
        return List.of(UniProtKBEntryPair.builder().from("from1").to(uniProtKB1).build(), UniProtKBEntryPair.builder().from("from2").to(uniProtKB2).build(), UniProtKBEntryPair.builder().from("from3").to(uniProtKB3).build());
    }

    @Override
    protected void mockBatch() {
        when(uniProtKB1.getPrimaryAccession()).thenReturn(new UniProtKBAccessionBuilder("to1").build());
        when(uniProtKB2.getPrimaryAccession()).thenReturn(new UniProtKBAccessionBuilder("to2").build());
        when(uniProtKB3.getPrimaryAccession()).thenReturn(new UniProtKBAccessionBuilder("to3").build());
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
                                return List.of(uniProtKB1, uniProtKB2);
                            }
                            if (List.of("to3").containsAll(args) && args.size() == 1) {
                                return List.of(uniProtKB3);
                            }
                            throw new IllegalArgumentException();
                        });
    }
}
