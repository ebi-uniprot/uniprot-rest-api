package org.uniprot.api.async.download.messaging.producer.idmapping;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.uniprot.api.async.download.messaging.producer.AsyncDownloadSubmissionRulesTest;
import org.uniprot.api.async.download.model.JobSubmitFeedback;
import org.uniprot.api.async.download.model.job.idmapping.IdMappingDownloadJob;
import org.uniprot.api.async.download.model.request.idmapping.IdMappingDownloadRequest;
import org.uniprot.api.async.download.service.idmapping.IdMappingJobService;
import org.uniprot.api.idmapping.common.model.IdMappingJob;
import org.uniprot.api.idmapping.common.model.IdMappingResult;
import org.uniprot.api.idmapping.common.service.IdMappingJobCacheService;
import org.uniprot.api.rest.download.model.JobStatus;

@ExtendWith(MockitoExtension.class)
class IdMappingAsyncDownloadSubmissionRulesTest
        extends AsyncDownloadSubmissionRulesTest<IdMappingDownloadRequest, IdMappingDownloadJob> {
    public static final String JOB_ID = "jobId";
    @Mock private IdMappingJobService idMappingJobService;
    @Mock private IdMappingDownloadJob idMappingDownloadJob;
    @Mock private IdMappingDownloadRequest idMappingDownloadRequest;
    @Mock private IdMappingJobCacheService idMappingJobCacheService;
    @Mock private IdMappingJob idMappingJob;
    @Mock private IdMappingResult idMappingResult;

    @BeforeEach
    void setUp() {
        jobService = idMappingJobService;
        downloadJob = idMappingDownloadJob;
        downloadRequest = idMappingDownloadRequest;
        asyncDownloadSubmissionRules =
                new IdMappingAsyncDownloadSubmissionRules(
                        MAX_RETRY_COUNT,
                        MAX_WAITING_TIME,
                        idMappingJobService,
                        idMappingJobCacheService);
        when(downloadRequest.getJobId()).thenReturn(JOB_ID);
        lenient().when(idMappingJob.getJobStatus()).thenReturn(JobStatus.FINISHED);
        when(idMappingJobCacheService.get(JOB_ID)).thenReturn(idMappingJob);
        lenient().when(idMappingJob.getIdMappingResult()).thenReturn(idMappingResult);
        mock();
    }

    @Test
    void submit_whenIdMappingJobIsNotPresent() {
        when(idMappingJobCacheService.get(JOB_ID)).thenReturn(null);

        JobSubmitFeedback jobSubmitFeedback =
                asyncDownloadSubmissionRules.submit(idMappingDownloadRequest);

        assertFalse(jobSubmitFeedback.isAllowed());
        assertEquals("ID Mapping Job id jobId not found", jobSubmitFeedback.getMessage());
    }

    @Test
    void submit_whenIdMappingJobResultIsNotReady() {
        when(idMappingJob.getJobStatus()).thenReturn(JobStatus.RUNNING);

        JobSubmitFeedback jobSubmitFeedback =
                asyncDownloadSubmissionRules.submit(idMappingDownloadRequest);

        assertFalse(jobSubmitFeedback.isAllowed());
        assertEquals("ID Mapping Job id jobId not yet finished", jobSubmitFeedback.getMessage());
    }
}
