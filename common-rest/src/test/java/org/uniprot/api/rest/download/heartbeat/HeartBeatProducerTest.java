package org.uniprot.api.rest.download.heartbeat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.uniprot.api.rest.download.configuration.AsyncDownloadHeartBeatConfiguration;
import org.uniprot.api.rest.download.model.DownloadJob;
import org.uniprot.api.rest.download.repository.DownloadJobRepository;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HeartBeatProducerTest {
    @Mock
    private AsyncDownloadHeartBeatConfiguration asyncDownloadHeartBeatConfiguration;
    @Mock
    private DownloadJobRepository jobRepository;
    @Mock
    private DownloadJob downloadJob;
    @Captor
    private ArgumentCaptor<DownloadJob> downloadJobArgumentCaptor;
    @InjectMocks
    private HeartBeatProducer heartBeatProducer;

    @BeforeEach
    void setUp() {

    }

    @Test
    void updateEntriesProcessed_whenHeartBeatEnabled() {
        when(asyncDownloadHeartBeatConfiguration.isEnabled()).thenReturn(true);
        when(asyncDownloadHeartBeatConfiguration.getInterval()).thenReturn(100L);
        when(downloadJob.getEntriesProcessed()).thenReturn(0L);
        when(downloadJob.getTotalEntries()).thenReturn(1000L);

        heartBeatProducer.updateEntriesProcessed(downloadJob, 70);
        heartBeatProducer.updateEntriesProcessed(downloadJob, 70);

        verify(downloadJob).setUpdated(any(LocalDateTime.class));
        verify(jobRepository).save(downloadJobArgumentCaptor.capture());
        assertEquals(140L, downloadJobArgumentCaptor.capture().getEntriesProcessed());

    }

    @Test
    void stopHeartBeat() {
    }
}