package org.uniprot.api.rest.download.heartbeat;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.time.LocalDateTime;
import java.util.List;

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

@ExtendWith(MockitoExtension.class)
class HeartBeatProducerTest {
    public static final String JOB_ID = "downloadJobId";
    private final DownloadJob downloadJob = DownloadJob.builder().id(JOB_ID).build();
    @Mock private AsyncDownloadHeartBeatConfiguration asyncDownloadHeartBeatConfiguration;
    @Mock private DownloadJobRepository jobRepository;
    @Captor private ArgumentCaptor<DownloadJob> downloadJobArgumentCaptor;
    @InjectMocks private HeartBeatProducer heartBeatProducer;

    @Test
    void create_whenHeartBeatEnabledAndIntervalIsBiggerThanBatchSize() {
        when(asyncDownloadHeartBeatConfiguration.isEnabled()).thenReturn(true);
        when(asyncDownloadHeartBeatConfiguration.getInterval()).thenReturn(100L);
        downloadJob.setTotalEntries(1000L);

        heartBeatProducer.create(downloadJob, 70);
        heartBeatProducer.create(downloadJob, 70);

        verifySavedJob(140L);
    }

    private void verifySavedJob(long expected) {
        verify(jobRepository).save(downloadJobArgumentCaptor.capture());
        DownloadJob savedJob = downloadJobArgumentCaptor.getValue();
        assertEquals(expected, savedJob.getEntriesProcessed());
        assertTrue(savedJob.getUpdated().isAfter(LocalDateTime.now().minusMinutes(2)));
    }

    @Test
    void create_whenHeartBeatEnabledAndIntervalIsSmallerThanBatchSize() {
        when(asyncDownloadHeartBeatConfiguration.isEnabled()).thenReturn(true);
        when(asyncDownloadHeartBeatConfiguration.getInterval()).thenReturn(50L);
        downloadJob.setTotalEntries(1000L);

        heartBeatProducer.create(downloadJob, 70);

        verifySavedJob(70L);

        heartBeatProducer.create(downloadJob, 70);

        verify(jobRepository, times(2)).save(downloadJobArgumentCaptor.capture());
        List<DownloadJob> savedJobs = downloadJobArgumentCaptor.getAllValues();
        assertEquals(140L, savedJobs.get(1).getEntriesProcessed());
        assertTrue(savedJobs.get(1).getUpdated().isAfter(LocalDateTime.now().minusMinutes(2)));
    }

    @Test
    void create_whenHeartBeatEnabledAndIntervalIsSameSizeAsBatchSize() {
        when(asyncDownloadHeartBeatConfiguration.isEnabled()).thenReturn(true);
        when(asyncDownloadHeartBeatConfiguration.getInterval()).thenReturn(50L);
        downloadJob.setTotalEntries(1000L);

        heartBeatProducer.create(downloadJob, 50);

        verifySavedJob(50L);

        heartBeatProducer.create(downloadJob, 50);

        verify(jobRepository, times(2)).save(downloadJobArgumentCaptor.capture());
        List<DownloadJob> savedJobs = downloadJobArgumentCaptor.getAllValues();
        assertEquals(100L, savedJobs.get(1).getEntriesProcessed());
        assertTrue(savedJobs.get(1).getUpdated().isAfter(LocalDateTime.now().minusMinutes(2)));
    }

    @Test
    void create_whenHeartBeatDisabled() {
        when(asyncDownloadHeartBeatConfiguration.isEnabled()).thenReturn(false);

        downloadJob.setTotalEntries(1000L);

        heartBeatProducer.create(downloadJob, 70);
        heartBeatProducer.create(downloadJob, 70);

        verify(jobRepository, never()).save(downloadJobArgumentCaptor.capture());
    }

    @Test
    void create_whenHeartBeatEnabledAndFinalUpdate() {
        when(asyncDownloadHeartBeatConfiguration.isEnabled()).thenReturn(true);
        when(asyncDownloadHeartBeatConfiguration.getInterval()).thenReturn(200L);
        downloadJob.setTotalEntries(130L);
        downloadJob.setEntriesProcessed(100L);

        heartBeatProducer.create(downloadJob, 70);
        heartBeatProducer.create(downloadJob, 60);

        verifySavedJob(130L);
    }

    @Test
    void create_whenExceptionsOccurs() {
        when(asyncDownloadHeartBeatConfiguration.isEnabled()).thenReturn(true);
        when(asyncDownloadHeartBeatConfiguration.getInterval()).thenReturn(50L);
        doThrow(RuntimeException.class).when(jobRepository).save(any(DownloadJob.class));
        downloadJob.setTotalEntries(130L);

        assertDoesNotThrow(() -> heartBeatProducer.create(downloadJob, 70));
    }

    @Test
    void create() {
        when(asyncDownloadHeartBeatConfiguration.isEnabled()).thenReturn(true);

        heartBeatProducer.create(downloadJob);

        verify(jobRepository).save(downloadJobArgumentCaptor.capture());
        DownloadJob savedJob = downloadJobArgumentCaptor.getValue();
        assertTrue(savedJob.getUpdated().isAfter(LocalDateTime.now().minusMinutes(2)));
    }

    @Test
    void create_whenDisabled() {
        when(asyncDownloadHeartBeatConfiguration.isEnabled()).thenReturn(false);

        heartBeatProducer.create(downloadJob);

        verify(jobRepository, never()).save(downloadJobArgumentCaptor.capture());
    }

    @Test
    void stop() {
        when(asyncDownloadHeartBeatConfiguration.isEnabled()).thenReturn(true);
        when(asyncDownloadHeartBeatConfiguration.getInterval()).thenReturn(50L);
        downloadJob.setTotalEntries(130L);

        heartBeatProducer.create(downloadJob, 70);
        downloadJob.setEntriesProcessed(0);
        heartBeatProducer.stop(JOB_ID);
        heartBeatProducer.create(downloadJob, 70);

        verify(jobRepository, times(2)).save(downloadJobArgumentCaptor.capture());
        List<DownloadJob> savedJobs = downloadJobArgumentCaptor.getAllValues();
        DownloadJob afterStopped = savedJobs.get(1);
        assertEquals(70L, afterStopped.getEntriesProcessed());
        assertTrue(afterStopped.getUpdated().isAfter(LocalDateTime.now().minusMinutes(2)));
    }
}
