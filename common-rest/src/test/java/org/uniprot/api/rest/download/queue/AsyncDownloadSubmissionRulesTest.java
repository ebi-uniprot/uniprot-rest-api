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

        assertTrue(asyncDownloadSubmissionRules.submit(JOB_ID, false).isAllowed());
    }

    @Test
    void submit_whenJobWithSameIdIsPresentAndWithoutForce() {
        when(downloadJobRepository.findById(JOB_ID)).thenReturn(Optional.of(downloadJob));

        assertFalse(asyncDownloadSubmissionRules.submit(JOB_ID, false).isAllowed());
    }

    @Test
    void submit_whenJobWithSameIdIsNotPresentAndWithForce() {
        when(downloadJobRepository.findById(JOB_ID)).thenReturn(Optional.empty());

        assertTrue(asyncDownloadSubmissionRules.submit(JOB_ID, true).isAllowed());
    }

    @Test
    void submit_whenJobWithSameIdIsPresentAndUnfinishedWithForce() {
        when(downloadJobRepository.findById(JOB_ID)).thenReturn(Optional.of(downloadJob));
        when(downloadJob.getStatus()).thenReturn(JobStatus.UNFINISHED);

        assertFalse(asyncDownloadSubmissionRules.submit(JOB_ID, true).isAllowed());
    }

    @Test
    void submit_whenJobWithSameIdIsPresentAndAbortedWithForce() {
        when(downloadJobRepository.findById(JOB_ID)).thenReturn(Optional.of(downloadJob));
        when(downloadJob.getStatus()).thenReturn(JobStatus.ABORTED);

        assertFalse(asyncDownloadSubmissionRules.submit(JOB_ID, true).isAllowed());
    }

    @Test
    void submit_whenJobWithSameIdIsPresentAndFinishedWithForce() {
        when(downloadJobRepository.findById(JOB_ID)).thenReturn(Optional.of(downloadJob));
        when(downloadJob.getStatus()).thenReturn(JobStatus.FINISHED);

        assertFalse(asyncDownloadSubmissionRules.submit(JOB_ID, true).isAllowed());
    }

    @Test
    void submit_whenJobWithSameIdIsPresentAndNewWithForce() {
        when(downloadJobRepository.findById(JOB_ID)).thenReturn(Optional.of(downloadJob));
        when(downloadJob.getStatus()).thenReturn(JobStatus.NEW);

        assertFalse(asyncDownloadSubmissionRules.submit(JOB_ID, true).isAllowed());
    }

    @Test
    void submit_whenJobWithSameIdIsPresentAndErrorWithoutRetryCountExceededWithForce() {
        when(downloadJobRepository.findById(JOB_ID)).thenReturn(Optional.of(downloadJob));
        when(downloadJob.getStatus()).thenReturn(JobStatus.ERROR);
        when(downloadJob.getRetried()).thenReturn(1);

        assertTrue(asyncDownloadSubmissionRules.submit(JOB_ID, true).isAllowed());
    }

    @Test
    void submit_whenJobWithSameIdIsPresentAndErrorWithRetryCountExceededWithForce() {
        when(downloadJobRepository.findById(JOB_ID)).thenReturn(Optional.of(downloadJob));
        when(downloadJob.getStatus()).thenReturn(JobStatus.ERROR);
        when(downloadJob.getRetried()).thenReturn(10);

        assertFalse(asyncDownloadSubmissionRules.submit(JOB_ID, true).isAllowed());
    }

    @Test
    void submit_whenJobWithSameIdIsPresentAndRunningLiveWithForce() {
        when(downloadJobRepository.findById(JOB_ID)).thenReturn(Optional.of(downloadJob));
        when(downloadJob.getStatus()).thenReturn(JobStatus.RUNNING);
        when(downloadJob.getUpdated()).thenReturn(LocalDateTime.now().minusMinutes(1));

        assertFalse(asyncDownloadSubmissionRules.submit(JOB_ID, true).isAllowed());
    }

    @Test
    void submit_whenJobWithSameIdIsPresentAndProcessingLiveWithForce() {
        when(downloadJobRepository.findById(JOB_ID)).thenReturn(Optional.of(downloadJob));
        when(downloadJob.getStatus()).thenReturn(JobStatus.PROCESSING);
        when(downloadJob.getUpdated()).thenReturn(LocalDateTime.now().minusMinutes(1));

        assertFalse(asyncDownloadSubmissionRules.submit(JOB_ID, true).isAllowed());
    }

    @Test
    void submit_whenJobWithSameIdIsPresentAndRunningDeadWithForce() {
        when(downloadJobRepository.findById(JOB_ID)).thenReturn(Optional.of(downloadJob));
        when(downloadJob.getStatus()).thenReturn(JobStatus.RUNNING);
        when(downloadJob.getUpdated()).thenReturn(LocalDateTime.now().minusMinutes(20));

        assertTrue(asyncDownloadSubmissionRules.submit(JOB_ID, true).isAllowed());
    }

    @Test
    void submit_whenJobWithSameIdIsPresentAndProcessingDeadWithForce() {
        when(downloadJobRepository.findById(JOB_ID)).thenReturn(Optional.of(downloadJob));
        when(downloadJob.getStatus()).thenReturn(JobStatus.PROCESSING);
        when(downloadJob.getUpdated()).thenReturn(LocalDateTime.now().minusMinutes(20));

        assertTrue(asyncDownloadSubmissionRules.submit(JOB_ID, true).isAllowed());
    }
}
