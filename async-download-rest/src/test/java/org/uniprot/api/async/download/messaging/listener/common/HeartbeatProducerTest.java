package org.uniprot.api.async.download.messaging.listener.common;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.uniprot.api.async.download.messaging.listener.uniprotkb.UniProtKBHeartbeatProducer;
import org.uniprot.api.async.download.model.uniprotkb.UniProtKBDownloadJob;
import org.uniprot.api.async.download.refactor.service.JobService;
import org.uniprot.api.async.download.refactor.service.uniprotkb.UniProtKBJobService;

@ExtendWith(MockitoExtension.class)
class HeartbeatProducerTest {
    public static final String JOB_ID = "downloadJobId";
    public static final String UPDATE_COUNT = "updateCount";
    public static final String PROCESSED_ENTRIES = "processedEntries";
    public static final String UPDATED = "updated";
    private final UniProtKBDownloadJob downloadJob =
            UniProtKBDownloadJob.builder().id(JOB_ID).build();
    @Mock private HeartbeatConfig heartbeatConfig;
    @Mock private UniProtKBJobService jobService;
    @Captor private ArgumentCaptor<Map<String, Object>> downloadJobArgumentCaptor;
    private UniProtKBHeartbeatProducer heartBeatProducer;

    @BeforeEach
    void setUp() {
        when(heartbeatConfig.getRetryCount()).thenReturn(3);
        when(heartbeatConfig.getRetryDelayInMillis()).thenReturn(100);
        heartBeatProducer = new UniProtKBHeartbeatProducer(heartbeatConfig, jobService);
    }

    @Test
    void createForResults_whenHeartBeatEnabledAndIntervalIsBiggerThanBatchSize() {
        when(heartbeatConfig.isEnabled()).thenReturn(true);
        when(heartbeatConfig.getResultsInterval()).thenReturn(100L);
        downloadJob.setTotalEntries(1000L);

        heartBeatProducer.createForResults(downloadJob, 70);
        heartBeatProducer.createForResults(downloadJob, 70);

        verifySavedJob(140L);
    }

    private void verifySavedJob(Long processedEntries) {
        verify(jobService).update(eq(JOB_ID), downloadJobArgumentCaptor.capture());
        Map<String, Object> savedJob = downloadJobArgumentCaptor.getValue();
        assertEquals(1L, savedJob.get(UPDATE_COUNT));
        assertEquals(processedEntries, savedJob.get(PROCESSED_ENTRIES));
        assertTrue(
                ((LocalDateTime) savedJob.get(UPDATED))
                        .isAfter(LocalDateTime.now().minusMinutes(2)));
    }

    @Test
    void createForResults_whenHeartBeatEnabledAndIntervalIsSmallerThanBatchSize() {
        when(heartbeatConfig.isEnabled()).thenReturn(true);
        when(heartbeatConfig.getResultsInterval()).thenReturn(50L);
        downloadJob.setTotalEntries(1000L);

        heartBeatProducer.createForResults(downloadJob, 70);

        verifySavedJob(70L);

        heartBeatProducer.createForResults(downloadJob, 70);

        verify(jobService, times(2)).update(eq(JOB_ID), downloadJobArgumentCaptor.capture());
        List<Map<String, Object>> savedJobs = downloadJobArgumentCaptor.getAllValues();
        Map<String, Object> lastCall = savedJobs.get(2);
        assertEquals(2L, lastCall.get(UPDATE_COUNT));
        assertEquals(140L, lastCall.get(PROCESSED_ENTRIES));
        assertTrue(
                ((LocalDateTime) lastCall.get(UPDATED))
                        .isAfter(LocalDateTime.now().minusMinutes(2)));
    }

    @Test
    void createForResults_whenHeartBeatEnabledAndIntervalIsSameSizeAsBatchSize() {
        when(heartbeatConfig.isEnabled()).thenReturn(true);
        when(heartbeatConfig.getResultsInterval()).thenReturn(50L);
        downloadJob.setTotalEntries(1000L);

        heartBeatProducer.createForResults(downloadJob, 50);

        verifySavedJob(50L);

        heartBeatProducer.createForResults(downloadJob, 50);

        verify(jobService, times(2)).update(eq(JOB_ID), downloadJobArgumentCaptor.capture());
        List<Map<String, Object>> savedJobs = downloadJobArgumentCaptor.getAllValues();
        Map<String, Object> lastCall = savedJobs.get(2);
        assertEquals(2L, lastCall.get(UPDATE_COUNT));
        assertEquals(100L, lastCall.get(PROCESSED_ENTRIES));
        assertTrue(
                ((LocalDateTime) lastCall.get(UPDATED))
                        .isAfter(LocalDateTime.now().minusMinutes(2)));
    }

    @Test
    void createForResults_whenHeartBeatDisabled() {
        when(heartbeatConfig.isEnabled()).thenReturn(false);

        downloadJob.setTotalEntries(1000L);

        heartBeatProducer.createForResults(downloadJob, 70);
        heartBeatProducer.createForResults(downloadJob, 70);

        verify(jobService, never()).save(any());
        verify(jobService, never()).update(any(), any());
    }

    @Test
    void createForResults_whenHeartBeatEnabledAndFinalUpdate() {
        when(heartbeatConfig.isEnabled()).thenReturn(true);
        when(heartbeatConfig.getResultsInterval()).thenReturn(200L);
        downloadJob.setTotalEntries(130L);
        downloadJob.setProcessedEntries(100L);

        heartBeatProducer.createForResults(downloadJob, 70);
        heartBeatProducer.createForResults(downloadJob, 60);

        verifySavedJob(130L);
    }

    @Test
    void createForResults_whenExceptionsOccurs() {
        when(heartbeatConfig.isEnabled()).thenReturn(true);
        when(heartbeatConfig.getResultsInterval()).thenReturn(50L);
        doThrow(RuntimeException.class)
                .when(jobService)
                .update(any(String.class), any(Map.class));
        downloadJob.setTotalEntries(130L);

        assertDoesNotThrow(() -> heartBeatProducer.createForResults(downloadJob, 70));
    }

    @Test
    void createForIds() {
        when(heartbeatConfig.isEnabled()).thenReturn(true);
        when(heartbeatConfig.getIdsInterval()).thenReturn(2L);
        downloadJob.setTotalEntries(1000L);

        heartBeatProducer.createForIds(downloadJob);
        heartBeatProducer.createForIds(downloadJob);

        verifySavedJob(null);

        heartBeatProducer.createForIds(downloadJob);
        heartBeatProducer.createForIds(downloadJob);
        verify(jobService, times(2)).update(eq(JOB_ID), any(Map.class));
    }

    @Test
    void createForIds_whenHeartBeatIsDisabled() {
        when(heartbeatConfig.isEnabled()).thenReturn(false);
        when(heartbeatConfig.getIdsInterval()).thenReturn(2L);

        downloadJob.setTotalEntries(1000L);

        heartBeatProducer.createForIds(downloadJob);
        heartBeatProducer.createForIds(downloadJob);

        verify(jobService, never()).save(any());
        verify(jobService, never()).update(any(), any());
    }

    @Test
    void createForIds_whenExceptionsOccurs() {
        when(heartbeatConfig.isEnabled()).thenReturn(true);
        when(heartbeatConfig.getIdsInterval()).thenReturn(1L);
        doThrow(RuntimeException.class)
                .when(jobService)
                .update(any(String.class), any(Map.class));
        downloadJob.setTotalEntries(130L);

        assertDoesNotThrow(() -> heartBeatProducer.createForIds(downloadJob));
    }

    @Test
    void stop() {
        when(heartbeatConfig.isEnabled()).thenReturn(true);
        when(heartbeatConfig.getResultsInterval()).thenReturn(50L);
        downloadJob.setTotalEntries(130L);

        heartBeatProducer.createForResults(downloadJob, 70);
        downloadJob.setProcessedEntries(0);
        heartBeatProducer.stop(JOB_ID);
        heartBeatProducer.createForResults(downloadJob, 70);

        verify(jobService, times(2)).update(eq(JOB_ID), downloadJobArgumentCaptor.capture());
        List<Map<String, Object>> savedJobs = downloadJobArgumentCaptor.getAllValues();
        Map<String, Object> afterStopped = savedJobs.get(1);
        assertEquals(2L, afterStopped.get(UPDATE_COUNT));
        assertEquals(70L, afterStopped.get(PROCESSED_ENTRIES));
        assertTrue(
                ((LocalDateTime) afterStopped.get(UPDATED))
                        .isAfter(LocalDateTime.now().minusMinutes(2)));
    }
}
