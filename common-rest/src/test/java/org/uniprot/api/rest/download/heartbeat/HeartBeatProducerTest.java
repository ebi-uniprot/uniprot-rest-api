package org.uniprot.api.rest.download.heartbeat;

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
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HeartBeatProducerTest {
    public static final String JOB_ID = "downloadJobId";
    public static final String UPDATE_COUNT = "updateCount";
    public static final String PROCESSED_ENTRIES = "processedEntries";
    public static final String UPDATED = "updated";
    private final DownloadJob downloadJob = DownloadJob.builder().id(JOB_ID).build();
    @Mock
    private AsyncDownloadHeartBeatConfiguration asyncDownloadHeartBeatConfiguration;
    @Mock
    private DownloadJobRepository jobRepository;
    @Captor
    private ArgumentCaptor<Map<String, Object>> downloadJobArgumentCaptor;
    @InjectMocks
    private HeartBeatProducer heartBeatProducer;

    @Test
    void createWithProgress_whenHeartBeatEnabledAndIntervalIsBiggerThanBatchSize() {
        when(asyncDownloadHeartBeatConfiguration.isEnabled()).thenReturn(true);
        when(asyncDownloadHeartBeatConfiguration.getInterval()).thenReturn(100L);
        downloadJob.setTotalEntries(1000L);

        heartBeatProducer.createWithProgress(downloadJob, 70);
        heartBeatProducer.createWithProgress(downloadJob, 70);

        verifySavedJob(140L);
    }

    private void verifySavedJob(Long processedEntries) {
        verify(jobRepository).update(eq(JOB_ID), downloadJobArgumentCaptor.capture());
        Map<String, Object> savedJob = downloadJobArgumentCaptor.getValue();
        assertEquals(1L, savedJob.get(UPDATE_COUNT));
        assertEquals(processedEntries, savedJob.get(PROCESSED_ENTRIES));
        assertTrue(
                ((LocalDateTime) savedJob.get(UPDATED))
                        .isAfter(LocalDateTime.now().minusMinutes(2)));
    }

    @Test
    void createWithProgress_whenHeartBeatEnabledAndIntervalIsSmallerThanBatchSize() {
        when(asyncDownloadHeartBeatConfiguration.isEnabled()).thenReturn(true);
        when(asyncDownloadHeartBeatConfiguration.getInterval()).thenReturn(50L);
        downloadJob.setTotalEntries(1000L);

        heartBeatProducer.createWithProgress(downloadJob, 70);

        verifySavedJob(70L);

        heartBeatProducer.createWithProgress(downloadJob, 70);

        verify(jobRepository, times(2)).update(eq(JOB_ID), downloadJobArgumentCaptor.capture());
        List<Map<String, Object>> savedJobs = downloadJobArgumentCaptor.getAllValues();
        Map<String, Object> lastCall = savedJobs.get(2);
        assertEquals(2L, lastCall.get(UPDATE_COUNT));
        assertEquals(140L, lastCall.get(PROCESSED_ENTRIES));
        assertTrue(
                ((LocalDateTime) lastCall.get(UPDATED))
                        .isAfter(LocalDateTime.now().minusMinutes(2)));
    }

    @Test
    void createWithProgress_whenHeartBeatEnabledAndIntervalIsSameSizeAsBatchSize() {
        when(asyncDownloadHeartBeatConfiguration.isEnabled()).thenReturn(true);
        when(asyncDownloadHeartBeatConfiguration.getInterval()).thenReturn(50L);
        downloadJob.setTotalEntries(1000L);

        heartBeatProducer.createWithProgress(downloadJob, 50);

        verifySavedJob(50L);

        heartBeatProducer.createWithProgress(downloadJob, 50);

        verify(jobRepository, times(2)).update(eq(JOB_ID), downloadJobArgumentCaptor.capture());
        List<Map<String, Object>> savedJobs = downloadJobArgumentCaptor.getAllValues();
        Map<String, Object> lastCall = savedJobs.get(2);
        assertEquals(2L, lastCall.get(UPDATE_COUNT));
        assertEquals(100L, lastCall.get(PROCESSED_ENTRIES));
        assertTrue(
                ((LocalDateTime) lastCall.get(UPDATED))
                        .isAfter(LocalDateTime.now().minusMinutes(2)));
    }

    @Test
    void createWithProgress_whenHeartBeatDisabled() {
        when(asyncDownloadHeartBeatConfiguration.isEnabled()).thenReturn(false);

        downloadJob.setTotalEntries(1000L);

        heartBeatProducer.createWithProgress(downloadJob, 70);
        heartBeatProducer.createWithProgress(downloadJob, 70);

        verify(jobRepository, never()).save(any());
        verify(jobRepository, never()).update(any(), any());
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
        doThrow(RuntimeException.class)
                .when(jobRepository)
                .update(any(String.class), any(Map.class));
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

        verifySavedJob(null);

        heartBeatProducer.create(downloadJob);
        heartBeatProducer.create(downloadJob);
        verify(jobRepository, times(2)).update(eq(JOB_ID), any(Map.class));
    }

    @Test
    void create_whenHeartBeatIsDisabled() {
        when(asyncDownloadHeartBeatConfiguration.isEnabled()).thenReturn(false);

        downloadJob.setTotalEntries(1000L);

        heartBeatProducer.create(downloadJob);
        heartBeatProducer.create(downloadJob);

        verify(jobRepository, never()).save(any());
        verify(jobRepository, never()).update(any(), any());
    }

    @Test
    void create_whenExceptionsOccurs() {
        when(asyncDownloadHeartBeatConfiguration.isEnabled()).thenReturn(true);
        when(asyncDownloadHeartBeatConfiguration.getInterval()).thenReturn(1L);
        doThrow(RuntimeException.class)
                .when(jobRepository)
                .update(any(String.class), any(Map.class));
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

        verify(jobRepository, times(2)).update(eq(JOB_ID), downloadJobArgumentCaptor.capture());
        List<Map<String, Object>> savedJobs = downloadJobArgumentCaptor.getAllValues();
        Map<String, Object> afterStopped = savedJobs.get(1);
        assertEquals(2L, afterStopped.get(UPDATE_COUNT));
        assertEquals(70L, afterStopped.get(PROCESSED_ENTRIES));
        assertTrue(
                ((LocalDateTime) afterStopped.get(UPDATED))
                        .isAfter(LocalDateTime.now().minusMinutes(2)));
    }
}
