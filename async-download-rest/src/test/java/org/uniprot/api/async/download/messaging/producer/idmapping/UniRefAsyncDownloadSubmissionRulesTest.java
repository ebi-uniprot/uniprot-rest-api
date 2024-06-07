package org.uniprot.api.async.download.messaging.producer.idmapping;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.uniprot.api.async.download.messaging.producer.common.AsyncDownloadSubmissionRulesTest;
import org.uniprot.api.async.download.model.common.JobSubmitFeedback;
import org.uniprot.api.async.download.model.idmapping.IdMappingDownloadJob;
import org.uniprot.api.async.download.refactor.request.idmapping.IdMappingDownloadRequest;
import org.uniprot.api.async.download.refactor.service.idmapping.IdMappingJobService;
import org.uniprot.api.idmapping.common.model.IdMappingJob;
import org.uniprot.api.idmapping.common.model.IdMappingResult;
import org.uniprot.api.idmapping.common.service.IdMappingJobCacheService;

import static org.junit.Assert.assertThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IdMappingAsyncDownloadSubmissionRulesTest extends AsyncDownloadSubmissionRulesTest<IdMappingDownloadRequest, IdMappingDownloadJob> {
    public static final String JOB_ID = "jobId";
    @Mock
    private IdMappingJobService idMappingJobService;
    @Mock
    private IdMappingDownloadJob idMappingDownloadJob;
    @Mock
    private IdMappingDownloadRequest idMappingDownloadRequest;
    @Mock
    private IdMappingJobCacheService idMappingJobCacheService;
    @Mock
    private IdMappingJob idMappingJob;
    @Mock
    private IdMappingResult idMappingResult;

    @BeforeEach
    void setUp() {
        jobService = idMappingJobService;
        downloadJob = idMappingDownloadJob;
        downloadRequest = idMappingDownloadRequest;
        asyncDownloadSubmissionRules =
                new IdMappingAsyncDownloadSubmissionRules(
                        MAX_RETRY_COUNT, MAX_WAITING_TIME, idMappingJobService, idMappingJobCacheService);
        when(downloadRequest.getJobId()).thenReturn(JOB_ID);
        when(idMappingJobCacheService.get(JOB_ID)).thenReturn(idMappingJob);
        lenient().when(idMappingJob.getIdMappingResult()).thenReturn(idMappingResult);
        mock();
    }

    @Test
    void submit_whenIdMappingJobIsNotPresent() {
        when(idMappingJobCacheService.get(JOB_ID)).thenReturn(null);

        assertThrows(IllegalArgumentException.class, () -> asyncDownloadSubmissionRules.submit(idMappingDownloadRequest));
    }

    @Test
    void submit_whenIdMappingJobResultIsNotReady() {
        when(idMappingJob.getIdMappingResult()).thenReturn(null);

        JobSubmitFeedback jobSubmitFeedback = asyncDownloadSubmissionRules.submit(idMappingDownloadRequest);

        assertFalse(jobSubmitFeedback.isAllowed());
        assertEquals("ID Mapping Job jobId id not yet finished", jobSubmitFeedback.getMessage());
    }
}
