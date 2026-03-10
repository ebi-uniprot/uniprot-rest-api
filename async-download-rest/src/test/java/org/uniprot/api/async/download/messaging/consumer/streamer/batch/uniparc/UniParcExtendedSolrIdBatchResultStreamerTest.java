package org.uniprot.api.async.download.messaging.consumer.streamer.batch.uniparc;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.uniprot.core.uniparc.UniParcCrossReference.PROPERTY_SOURCES;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.uniprot.api.async.download.messaging.consumer.heartbeat.uniparc.UniParcHeartbeatProducer;
import org.uniprot.api.async.download.messaging.consumer.streamer.batch.SolrIdBatchResultStreamerTest;
import org.uniprot.api.async.download.model.job.uniparc.UniParcDownloadJob;
import org.uniprot.api.async.download.model.request.uniparc.UniParcDownloadRequest;
import org.uniprot.api.async.download.service.uniparc.UniParcJobService;
import org.uniprot.api.common.repository.stream.common.BatchIterable;
import org.uniprot.api.common.repository.stream.store.StoreStreamerConfig;
import org.uniprot.api.common.repository.stream.store.StreamerConfigProperties;
import org.uniprot.api.uniparc.common.service.light.UniParcCrossReferenceService;
import org.uniprot.core.Property;
import org.uniprot.core.uniparc.*;
import org.uniprot.store.datastore.UniProtStoreClient;

import net.jodah.failsafe.RetryPolicy;

@ExtendWith(MockitoExtension.class)
class UniParcExtendedSolrIdBatchResultStreamerTest
        extends SolrIdBatchResultStreamerTest<
                UniParcDownloadRequest, UniParcDownloadJob, UniParcEntry> {
    private static final String PROTEOME_ID = "UP12345";
    private static final int BATCH_SIZE = 2;
    @Mock private UniParcDownloadRequest uniParcDownloadRequest;
    @Mock private UniParcDownloadJob uniParcDownloadJob;
    @Mock private UniParcHeartbeatProducer uniParcHeartbeatProducer;
    @Mock private UniParcJobService uniParcJobService;
    @Mock private StoreStreamerConfig<UniParcEntryLight> uniParcEntryStoreStreamerConfig;
    @Mock private UniProtStoreClient<UniParcEntryLight> uniParcStoreClient;
    @Mock private StreamerConfigProperties streamerConfig;
    @Mock private UniParcEntryLight uniParcLight1;
    @Mock private UniParcEntryLight uniParcLight2;
    @Mock private UniParcEntryLight uniParcLight3;
    @Mock private UniParcEntry uniParc1;
    @Mock private UniParcEntry uniParc2;
    @Mock private UniParcEntry uniParc3;
    @Mock private UniParcCrossReferenceService uniParcCrossReferenceService;
    @Mock private UniParcSolrIdBatchResultStreamer uniParcSolrIdBatchResultStreamer;
    @Mock private UniParcCrossReference xRef1;
    @Mock private UniParcCrossReference xRef2;
    @Mock private UniParcCrossReference xRef3;
    @Mock private UniParcCrossReference xRef4;
    @Mock private Proteome proteome1;
    @Mock private BatchIterable<UniParcEntry> batchStoreIterable;
    private final Property property1 = new Property(PROPERTY_SOURCES, PROTEOME_ID);
    private final Property property2 = new Property(PROPERTY_SOURCES, "Random");

    @BeforeEach
    void setUp() {
        request = uniParcDownloadRequest;
        job = uniParcDownloadJob;
        heartbeatProducer = uniParcHeartbeatProducer;
        jobService = uniParcJobService;
        lenient().when(request.getFormat()).thenReturn("text/plain;format=fastax");
        lenient().when(request.getQuery()).thenReturn("proteome:" + PROTEOME_ID);
        lenient().when(xRef2.getProteomes()).thenReturn(List.of(proteome1));
        lenient().when(xRef4.getProteomes()).thenReturn(List.of(proteome1));
        lenient().when(proteome1.getId()).thenReturn("Random");
        lenient().when(xRef1.getProperties()).thenReturn(List.of(property1));
        lenient().when(xRef2.getProperties()).thenReturn(List.of(property2));
        lenient().when(xRef3.getProperties()).thenReturn(List.of(property1));
        lenient().when(xRef4.getProperties()).thenReturn(List.of(property2));
        lenient()
                .when(
                        uniParcCrossReferenceService.getCrossReferencesWithBatchSize(
                                uniParcLight1, true))
                .thenReturn(Stream.of(xRef1));
        lenient()
                .when(
                        uniParcCrossReferenceService.getCrossReferencesWithBatchSize(
                                uniParcLight2, true))
                .thenReturn(Stream.of(xRef2));
        lenient()
                .when(
                        uniParcCrossReferenceService.getCrossReferencesWithBatchSize(
                                uniParcLight3, true))
                .thenReturn(Stream.of(xRef3, xRef4));
        solrIdBatchResultStreamer =
                new UniParcExtendedSolrIdBatchResultStreamer(
                        uniParcHeartbeatProducer,
                        uniParcJobService,
                        uniParcEntryStoreStreamerConfig,
                        uniParcCrossReferenceService,
                        uniParcSolrIdBatchResultStreamer);
    }

    @Override
    protected void mockBatch() {
        lenient()
                .when(uniParcEntryStoreStreamerConfig.getStoreClient())
                .thenReturn(uniParcStoreClient);
        lenient()
                .when(uniParcEntryStoreStreamerConfig.getStoreFetchRetryPolicy())
                .thenReturn(new RetryPolicy<>());
        lenient()
                .when(uniParcEntryStoreStreamerConfig.getStreamConfig())
                .thenReturn(streamerConfig);
        lenient().when(streamerConfig.getStoreBatchSize()).thenReturn(BATCH_SIZE);
        lenient().when(uniParcLight1.getUniParcId()).thenReturn("id1");
        lenient().when(uniParcLight2.getUniParcId()).thenReturn("id2");
        lenient().when(uniParcLight3.getUniParcId()).thenReturn("id3");
        lenient()
                .when(uniParcStoreClient.getEntries(any()))
                .thenAnswer(
                        inv -> {
                            Iterable<String> strings = inv.getArgument(0);
                            if (List.of("id1", "id2")
                                    .equals(
                                            StreamSupport.stream(strings.spliterator(), false)
                                                    .collect(Collectors.toList()))) {
                                return List.of(uniParcLight1, uniParcLight2);
                            }
                            if (List.of("id3")
                                    .equals(
                                            StreamSupport.stream(strings.spliterator(), false)
                                                    .collect(Collectors.toList()))) {
                                return List.of(uniParcLight3);
                            }
                            throw new IllegalArgumentException();
                        });
    }

    @Override
    protected void assertResults(Stream<UniParcEntry> result) {
        List<UniParcEntry> results = result.toList();
        assertThat(
                results.stream().map(UniParcEntry::getUniParcId).map(UniParcId::getValue).toList(),
                Matchers.contains("id1", "id2", "id3"));
        assertEquals(1, results.get(0).getUniParcCrossReferences().size());
        assertEquals(0, results.get(1).getUniParcCrossReferences().size());
        assertEquals(1, results.get(2).getUniParcCrossReferences().size());
    }

    @Override
    protected Iterable<UniParcEntry> getEntryList() {
        return List.of(uniParc1, uniParc2, uniParc3);
    }
}
