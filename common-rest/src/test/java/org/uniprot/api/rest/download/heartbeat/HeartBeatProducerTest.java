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
    void createWithProgress_whenHeartBeatEnabledAndIntervalIsBiggerThanBatchSize() {
        when(asyncDownloadHeartBeatConfiguration.isEnabled()).thenReturn(true);
        when(asyncDownloadHeartBeatConfiguration.getInterval()).thenReturn(100L);
        downloadJob.setTotalEntries(1000L);

        heartBeatProducer.createWithProgress(downloadJob, 70);
        heartBeatProducer.createWithProgress(downloadJob, 70);

        verifySavedJob(140L);
    }

    private void verifySavedJob(long processedEntries) {
        verify(jobRepository).save(downloadJobArgumentCaptor.capture());
        DownloadJob savedJob = downloadJobArgumentCaptor.getValue();
        assertEquals(1, savedJob.getUpdateCount());
        assertEquals(processedEntries, savedJob.getProcessedEntries());
        assertTrue(savedJob.getUpdated().isAfter(LocalDateTime.now().minusMinutes(2)));
    }

    @Test
    void createWithProgress_whenHeartBeatEnabledAndIntervalIsSmallerThanBatchSize() {
        when(asyncDownloadHeartBeatConfiguration.isEnabled()).thenReturn(true);
        when(asyncDownloadHeartBeatConfiguration.getInterval()).thenReturn(50L);
        downloadJob.setTotalEntries(1000L);

        heartBeatProducer.createWithProgress(downloadJob, 70);

        verifySavedJob(70L);

        heartBeatProducer.createWithProgress(downloadJob, 70);

        verify(jobRepository, times(2)).save(downloadJobArgumentCaptor.capture());
        List<DownloadJob> savedJobs = downloadJobArgumentCaptor.getAllValues();
        assertEquals(140L, savedJobs.get(1).getProcessedEntries());
        assertEquals(2, savedJobs.get(1).getUpdateCount());
        assertTrue(savedJobs.get(1).getUpdated().isAfter(LocalDateTime.now().minusMinutes(2)));
    }

    @Test
    void createWithProgress_whenHeartBeatEnabledAndIntervalIsSameSizeAsBatchSize() {
        when(asyncDownloadHeartBeatConfiguration.isEnabled()).thenReturn(true);
        when(asyncDownloadHeartBeatConfiguration.getInterval()).thenReturn(50L);
        downloadJob.setTotalEntries(1000L);

        heartBeatProducer.createWithProgress(downloadJob, 50);

        verifySavedJob(50L);

        heartBeatProducer.createWithProgress(downloadJob, 50);

        verify(jobRepository, times(2)).save(downloadJobArgumentCaptor.capture());
        List<DownloadJob> savedJobs = downloadJobArgumentCaptor.getAllValues();
        assertEquals(100L, savedJobs.get(1).getProcessedEntries());
        assertEquals(2, savedJobs.get(1).getUpdateCount());
        assertTrue(savedJobs.get(1).getUpdated().isAfter(LocalDateTime.now().minusMinutes(2)));
    }

    @Test
    void createWithProgress_whenHeartBeatDisabled() {
        when(asyncDownloadHeartBeatConfiguration.isEnabled()).thenReturn(false);

        downloadJob.setTotalEntries(1000L);

        heartBeatProducer.createWithProgress(downloadJob, 70);
        heartBeatProducer.createWithProgress(downloadJob, 70);

        verify(jobRepository, never()).save(downloadJobArgumentCaptor.capture());
    }

    @Test
    void createWithProgress_whenHeartBeatEnabledAndFinalUpdate() {
        when(asyncDownloadHeartBeatConfiguration.isEnabled()).thenReturn(true);
        when(asyncDownloadHeartBeatConfiguration.getInterval()).thenReturn(200L);
        downloadJob.setTotalEntries(130L);
        downloadJob.setProcessedEntries(100L);

        heartBeatProducer.createWithProgress(downloadJob, 70);
        heartBeatProducer.createWithProgress(downloadJob, 60);

        verifySavedJob(130L);
    }

    @Test
    void createWithProgress_whenExceptionsOccurs() {
        when(asyncDownloadHeartBeatConfiguration.isEnabled()).thenReturn(true);
        when(asyncDownloadHeartBeatConfiguration.getInterval()).thenReturn(50L);
        doThrow(RuntimeException.class).when(jobRepository).save(any(DownloadJob.class));
        downloadJob.setTotalEntries(130L);

        assertDoesNotThrow(() -> heartBeatProducer.createWithProgress(downloadJob, 70));
    }

    @Test
    void create() {
        when(asyncDownloadHeartBeatConfiguration.isEnabled()).thenReturn(true);
        when(asyncDownloadHeartBeatConfiguration.getInterval()).thenReturn(2L);
        downloadJob.setTotalEntries(1000L);

        heartBeatProducer.create(downloadJob);
        heartBeatProducer.create(downloadJob);

        verifySavedJob(0L);
    }

    @Test
    void create_whenHeartBeatIsDisabled() {
        when(asyncDownloadHeartBeatConfiguration.isEnabled()).thenReturn(false);

        downloadJob.setTotalEntries(1000L);

        heartBeatProducer.create(downloadJob);
        heartBeatProducer.create(downloadJob);

        verify(jobRepository, never()).save(downloadJobArgumentCaptor.capture());
    }

    @Test
    void create_whenExceptionsOccurs() {
        when(asyncDownloadHeartBeatConfiguration.isEnabled()).thenReturn(true);
        when(asyncDownloadHeartBeatConfiguration.getInterval()).thenReturn(1L);
        doThrow(RuntimeException.class).when(jobRepository).save(any(DownloadJob.class));
        downloadJob.setTotalEntries(130L);

        assertDoesNotThrow(() -> heartBeatProducer.create(downloadJob));
    }

    @Test
    void stop() {
        when(asyncDownloadHeartBeatConfiguration.isEnabled()).thenReturn(true);
        when(asyncDownloadHeartBeatConfiguration.getInterval()).thenReturn(50L);
        downloadJob.setTotalEntries(130L);

        heartBeatProducer.createWithProgress(downloadJob, 70);
        downloadJob.setProcessedEntries(0);
        heartBeatProducer.stop(JOB_ID);
        heartBeatProducer.createWithProgress(downloadJob, 70);

        verify(jobRepository, times(2)).save(downloadJobArgumentCaptor.capture());
        List<DownloadJob> savedJobs = downloadJobArgumentCaptor.getAllValues();
        DownloadJob afterStopped = savedJobs.get(1);
        assertEquals(70L, afterStopped.getProcessedEntries());
        assertEquals(2, savedJobs.get(1).getUpdateCount());
        assertTrue(afterStopped.getUpdated().isAfter(LocalDateTime.now().minusMinutes(2)));
    }
}
