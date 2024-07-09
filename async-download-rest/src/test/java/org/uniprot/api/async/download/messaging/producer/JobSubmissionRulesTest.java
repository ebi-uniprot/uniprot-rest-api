package org.uniprot.api.async.download.messaging.producer;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.uniprot.api.async.download.model.JobSubmitFeedback;
import org.uniprot.api.async.download.model.job.DownloadJob;
import org.uniprot.api.async.download.model.request.DownloadRequest;
import org.uniprot.api.async.download.service.JobService;
import org.uniprot.api.rest.download.model.JobStatus;

public abstract class JobSubmissionRulesTest<T extends DownloadRequest, R extends DownloadJob> {
    protected static final int MAX_RETRY_COUNT = 3;
    protected static final int MAX_WAITING_TIME = 10;
    protected static final String ID = "someId";
    protected JobService<R> jobService;
    protected R downloadJob;
    protected T downloadRequest;
    protected JobSubmissionRules<T, R> jobSubmissionRules;

    protected void mock() {
        lenient().when(downloadRequest.getDownloadJobId()).thenReturn(ID);
    }

    @Test
    void submit_whenJobWithSameIdIsNotPresentAndWithoutForce() {
        when(jobService.find(ID)).thenReturn(Optional.empty());

        JobSubmitFeedback jobSubmitFeedback = jobSubmissionRules.submit(downloadRequest);

        assertTrue(jobSubmitFeedback.isAllowed());
        assertNull(jobSubmitFeedback.getMessage());
    }

    @Test
    void submit_whenJobWithSameIdIsPresentAndWithoutForce() {
        when(jobService.find(ID)).thenReturn(Optional.of(downloadJob));

        JobSubmitFeedback jobSubmitFeedback = jobSubmissionRules.submit(downloadRequest);

        assertFalse(jobSubmitFeedback.isAllowed());
        assertEquals(
                "Job with id someId has already been submitted", jobSubmitFeedback.getMessage());
    }

    @Test
    void submit_whenJobWithSameIdIsNotPresentAndWithForce() {
        when(jobService.find(ID)).thenReturn(Optional.empty());

        JobSubmitFeedback jobSubmitFeedback = jobSubmissionRules.submit(downloadRequest);

        assertTrue(jobSubmitFeedback.isAllowed());
        assertNull(jobSubmitFeedback.getMessage());
    }

    @Test
    void submit_whenJobWithSameIdIsPresentAndUnfinishedWithForce() {
        when(jobService.find(ID)).thenReturn(Optional.of(downloadJob));
        when(downloadJob.getStatus()).thenReturn(JobStatus.UNFINISHED);
        when(downloadRequest.isForce()).thenReturn(true);

        JobSubmitFeedback jobSubmitFeedback = jobSubmissionRules.submit(downloadRequest);

        assertFalse(jobSubmitFeedback.isAllowed());
        assertEquals(
                "Job with id someId has already been submitted", jobSubmitFeedback.getMessage());
    }

    @Test
    void submit_whenJobWithSameIdIsPresentAndAbortedWithForce() {
        when(jobService.find(ID)).thenReturn(Optional.of(downloadJob));
        when(downloadJob.getStatus()).thenReturn(JobStatus.ABORTED);
        when(downloadRequest.isForce()).thenReturn(true);

        JobSubmitFeedback jobSubmitFeedback = jobSubmissionRules.submit(downloadRequest);

        assertFalse(jobSubmitFeedback.isAllowed());
        assertEquals(
                "Job with id someId is already aborted for the excess size of results",
                jobSubmitFeedback.getMessage());
    }

    @Test
    void submit_whenJobWithSameIdIsPresentAndFinishedWithForce() {
        when(jobService.find(ID)).thenReturn(Optional.of(downloadJob));
        when(downloadJob.getStatus()).thenReturn(JobStatus.FINISHED);
        when(downloadRequest.isForce()).thenReturn(true);

        JobSubmitFeedback jobSubmitFeedback = jobSubmissionRules.submit(downloadRequest);

        assertFalse(jobSubmitFeedback.isAllowed());
        assertEquals(
                "Job with id someId has already been finished successfully.",
                jobSubmitFeedback.getMessage());
    }

    @Test
    void submit_whenJobWithSameIdIsPresentAndNewWithForce() {
        when(jobService.find(ID)).thenReturn(Optional.of(downloadJob));
        when(downloadJob.getStatus()).thenReturn(JobStatus.NEW);
        when(downloadRequest.isForce()).thenReturn(true);

        JobSubmitFeedback jobSubmitFeedback = jobSubmissionRules.submit(downloadRequest);

        assertFalse(jobSubmitFeedback.isAllowed());
        assertEquals(
                "Job with id someId has already been submitted", jobSubmitFeedback.getMessage());
    }

    @Test
    void submit_whenJobWithSameIdIsPresentAndErrorWithRetryCountExceededWithForce() {
        when(jobService.find(ID)).thenReturn(Optional.of(downloadJob));
        when(downloadJob.getStatus()).thenReturn(JobStatus.ERROR);
        when(downloadJob.getRetried()).thenReturn(10);
        when(downloadRequest.isForce()).thenReturn(true);

        JobSubmitFeedback jobSubmitFeedback = jobSubmissionRules.submit(downloadRequest);

        assertTrue(jobSubmitFeedback.isAllowed());
        assertNull(jobSubmitFeedback.getMessage());
    }

    @Test
    void submit_whenJobWithSameIdIsPresentAndErrorWithoutRetryCountExceededWithForce() {
        when(jobService.find(ID)).thenReturn(Optional.of(downloadJob));
        when(downloadJob.getStatus()).thenReturn(JobStatus.ERROR);
        when(downloadJob.getRetried()).thenReturn(1);
        when(downloadRequest.isForce()).thenReturn(true);

        JobSubmitFeedback jobSubmitFeedback = jobSubmissionRules.submit(downloadRequest);

        assertFalse(jobSubmitFeedback.isAllowed());
        assertEquals("Job with id someId is already being retried", jobSubmitFeedback.getMessage());
    }

    @Test
    void submit_whenJobWithSameIdIsPresentAndRunningLiveWithForce() {
        when(jobService.find(ID)).thenReturn(Optional.of(downloadJob));
        when(downloadJob.getStatus()).thenReturn(JobStatus.RUNNING);
        when(downloadJob.getUpdated()).thenReturn(LocalDateTime.now().minusMinutes(1));
        when(downloadRequest.isForce()).thenReturn(true);

        JobSubmitFeedback jobSubmitFeedback = jobSubmissionRules.submit(downloadRequest);

        assertFalse(jobSubmitFeedback.isAllowed());
        assertEquals(
                "Job with id someId is already running and live", jobSubmitFeedback.getMessage());
    }

    @Test
    void submit_whenJobWithSameIdIsPresentAndProcessingLiveWithForce() {
        when(jobService.find(ID)).thenReturn(Optional.of(downloadJob));
        when(downloadJob.getStatus()).thenReturn(JobStatus.PROCESSING);
        when(downloadJob.getUpdated()).thenReturn(LocalDateTime.now().minusMinutes(1));
        when(downloadRequest.isForce()).thenReturn(true);

        JobSubmitFeedback jobSubmitFeedback = jobSubmissionRules.submit(downloadRequest);

        assertFalse(jobSubmitFeedback.isAllowed());
        assertEquals(
                "Job with id someId is already running and live", jobSubmitFeedback.getMessage());
    }

    @Test
    void submit_whenJobWithSameIdIsPresentAndRunningDeadWithForce() {
        when(jobService.find(ID)).thenReturn(Optional.of(downloadJob));
        when(downloadJob.getStatus()).thenReturn(JobStatus.RUNNING);
        when(downloadJob.getUpdated()).thenReturn(LocalDateTime.now().minusMinutes(20));
        when(downloadRequest.isForce()).thenReturn(true);

        JobSubmitFeedback jobSubmitFeedback = jobSubmissionRules.submit(downloadRequest);

        assertTrue(jobSubmitFeedback.isAllowed());
        assertNull(jobSubmitFeedback.getMessage());
    }

    @Test
    void submit_whenJobWithSameIdIsPresentAndProcessingDeadWithForce() {
        when(jobService.find(ID)).thenReturn(Optional.of(downloadJob));
        when(downloadJob.getStatus()).thenReturn(JobStatus.PROCESSING);
        when(downloadJob.getUpdated()).thenReturn(LocalDateTime.now().minusMinutes(20));
        when(downloadRequest.isForce()).thenReturn(true);

        JobSubmitFeedback jobSubmitFeedback = jobSubmissionRules.submit(downloadRequest);

        assertTrue(jobSubmitFeedback.isAllowed());
        assertNull(jobSubmitFeedback.getMessage());
    }
}
