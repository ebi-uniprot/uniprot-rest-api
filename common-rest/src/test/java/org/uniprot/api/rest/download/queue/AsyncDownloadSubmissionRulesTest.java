package org.uniprot.api.rest.download.queue;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.uniprot.api.rest.download.model.DownloadJob;
import org.uniprot.api.rest.download.model.JobStatus;
import org.uniprot.api.rest.download.repository.DownloadJobRepository;
import org.uniprot.api.rest.output.job.JobSubmitFeedback;

@ExtendWith(MockitoExtension.class)
class AsyncDownloadSubmissionRulesTest {
    private static final int MAX_RETRY_COUNT = 3;
    private static final int MAX_WAITING_TIME = 10;
    public static final String JOB_ID = "jobId";
    @Mock private DownloadJobRepository downloadJobRepository;
    @Mock private DownloadJob downloadJob;
    private AsyncDownloadSubmissionRules asyncDownloadSubmissionRules;

    @BeforeEach
    void setUp() {
        asyncDownloadSubmissionRules =
                new AsyncDownloadSubmissionRules(
                        MAX_RETRY_COUNT, MAX_WAITING_TIME, downloadJobRepository);
    }

    @Test
    void submit_whenJobWithSameIdIsNotPresentAndWithoutForce() {
        when(downloadJobRepository.findById(JOB_ID)).thenReturn(Optional.empty());

        JobSubmitFeedback jobSubmitFeedback = asyncDownloadSubmissionRules.submit(JOB_ID, false);

        assertTrue(jobSubmitFeedback.isAllowed());
        assertNull(jobSubmitFeedback.getMessage());
    }

    @Test
    void submit_whenJobWithSameIdIsPresentAndWithoutForce() {
        when(downloadJobRepository.findById(JOB_ID)).thenReturn(Optional.of(downloadJob));

        JobSubmitFeedback jobSubmitFeedback = asyncDownloadSubmissionRules.submit(JOB_ID, false);

        assertFalse(jobSubmitFeedback.isAllowed());
        assertEquals(
                "Job with id jobId has already been submitted", jobSubmitFeedback.getMessage());
    }

    @Test
    void submit_whenJobWithSameIdIsNotPresentAndWithForce() {
        when(downloadJobRepository.findById(JOB_ID)).thenReturn(Optional.empty());

        JobSubmitFeedback jobSubmitFeedback = asyncDownloadSubmissionRules.submit(JOB_ID, true);

        assertTrue(jobSubmitFeedback.isAllowed());
        assertNull(jobSubmitFeedback.getMessage());
    }

    @Test
    void submit_whenJobWithSameIdIsPresentAndUnfinishedWithForce() {
        when(downloadJobRepository.findById(JOB_ID)).thenReturn(Optional.of(downloadJob));
        when(downloadJob.getStatus()).thenReturn(JobStatus.UNFINISHED);

        JobSubmitFeedback jobSubmitFeedback = asyncDownloadSubmissionRules.submit(JOB_ID, true);

        assertFalse(jobSubmitFeedback.isAllowed());
        assertEquals(
                "Job with id jobId has already been submitted", jobSubmitFeedback.getMessage());
    }

    @Test
    void submit_whenJobWithSameIdIsPresentAndAbortedWithForce() {
        when(downloadJobRepository.findById(JOB_ID)).thenReturn(Optional.of(downloadJob));
        when(downloadJob.getStatus()).thenReturn(JobStatus.ABORTED);

        JobSubmitFeedback jobSubmitFeedback = asyncDownloadSubmissionRules.submit(JOB_ID, true);

        assertFalse(jobSubmitFeedback.isAllowed());
        assertEquals(
                "Job with id jobId is already aborted for the excess size of results",
                jobSubmitFeedback.getMessage());
    }

    @Test
    void submit_whenJobWithSameIdIsPresentAndFinishedWithForce() {
        when(downloadJobRepository.findById(JOB_ID)).thenReturn(Optional.of(downloadJob));
        when(downloadJob.getStatus()).thenReturn(JobStatus.FINISHED);

        JobSubmitFeedback jobSubmitFeedback = asyncDownloadSubmissionRules.submit(JOB_ID, true);

        assertFalse(jobSubmitFeedback.isAllowed());
        assertEquals(
                "Job with id jobId has already been finished successfully.",
                jobSubmitFeedback.getMessage());
    }

    @Test
    void submit_whenJobWithSameIdIsPresentAndNewWithForce() {
        when(downloadJobRepository.findById(JOB_ID)).thenReturn(Optional.of(downloadJob));
        when(downloadJob.getStatus()).thenReturn(JobStatus.NEW);

        JobSubmitFeedback jobSubmitFeedback = asyncDownloadSubmissionRules.submit(JOB_ID, true);

        assertFalse(jobSubmitFeedback.isAllowed());
        assertEquals(
                "Job with id jobId has already been submitted", jobSubmitFeedback.getMessage());
    }

    @Test
    void submit_whenJobWithSameIdIsPresentAndErrorWithRetryCountExceededWithForce() {
        when(downloadJobRepository.findById(JOB_ID)).thenReturn(Optional.of(downloadJob));
        when(downloadJob.getStatus()).thenReturn(JobStatus.ERROR);
        when(downloadJob.getRetried()).thenReturn(10);

        JobSubmitFeedback jobSubmitFeedback = asyncDownloadSubmissionRules.submit(JOB_ID, true);

        assertTrue(jobSubmitFeedback.isAllowed());
        assertNull(jobSubmitFeedback.getMessage());
    }

    @Test
    void submit_whenJobWithSameIdIsPresentAndErrorWithoutRetryCountExceededWithForce() {
        when(downloadJobRepository.findById(JOB_ID)).thenReturn(Optional.of(downloadJob));
        when(downloadJob.getStatus()).thenReturn(JobStatus.ERROR);
        when(downloadJob.getRetried()).thenReturn(1);

        JobSubmitFeedback jobSubmitFeedback = asyncDownloadSubmissionRules.submit(JOB_ID, true);

        assertFalse(jobSubmitFeedback.isAllowed());
        assertEquals("Job with id jobId is already being retried", jobSubmitFeedback.getMessage());
    }

    @Test
    void submit_whenJobWithSameIdIsPresentAndRunningLiveWithForce() {
        when(downloadJobRepository.findById(JOB_ID)).thenReturn(Optional.of(downloadJob));
        when(downloadJob.getStatus()).thenReturn(JobStatus.RUNNING);
        when(downloadJob.getUpdated()).thenReturn(LocalDateTime.now().minusMinutes(1));

        JobSubmitFeedback jobSubmitFeedback = asyncDownloadSubmissionRules.submit(JOB_ID, true);

        assertFalse(jobSubmitFeedback.isAllowed());
        assertEquals(
                "Job with id jobId is already running and live", jobSubmitFeedback.getMessage());
    }

    @Test
    void submit_whenJobWithSameIdIsPresentAndProcessingLiveWithForce() {
        when(downloadJobRepository.findById(JOB_ID)).thenReturn(Optional.of(downloadJob));
        when(downloadJob.getStatus()).thenReturn(JobStatus.PROCESSING);
        when(downloadJob.getUpdated()).thenReturn(LocalDateTime.now().minusMinutes(1));

        JobSubmitFeedback jobSubmitFeedback = asyncDownloadSubmissionRules.submit(JOB_ID, true);

        assertFalse(jobSubmitFeedback.isAllowed());
        assertEquals(
                "Job with id jobId is already running and live", jobSubmitFeedback.getMessage());
    }

    @Test
    void submit_whenJobWithSameIdIsPresentAndRunningDeadWithForce() {
        when(downloadJobRepository.findById(JOB_ID)).thenReturn(Optional.of(downloadJob));
        when(downloadJob.getStatus()).thenReturn(JobStatus.RUNNING);
        when(downloadJob.getUpdated()).thenReturn(LocalDateTime.now().minusMinutes(20));

        JobSubmitFeedback jobSubmitFeedback = asyncDownloadSubmissionRules.submit(JOB_ID, true);

        assertTrue(jobSubmitFeedback.isAllowed());
        assertNull(jobSubmitFeedback.getMessage());
    }

    @Test
    void submit_whenJobWithSameIdIsPresentAndProcessingDeadWithForce() {
        when(downloadJobRepository.findById(JOB_ID)).thenReturn(Optional.of(downloadJob));
        when(downloadJob.getStatus()).thenReturn(JobStatus.PROCESSING);
        when(downloadJob.getUpdated()).thenReturn(LocalDateTime.now().minusMinutes(20));

        JobSubmitFeedback jobSubmitFeedback = asyncDownloadSubmissionRules.submit(JOB_ID, true);

        assertTrue(jobSubmitFeedback.isAllowed());
        assertNull(jobSubmitFeedback.getMessage());
    }
}
