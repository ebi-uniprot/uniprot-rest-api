package org.uniprot.api.mapto.common.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.uniprot.api.rest.download.model.JobStatus.*;
import static org.uniprot.api.rest.download.model.JobStatus.FINISHED;
import static org.uniprot.store.config.UniProtDataType.*;

import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.uniprot.api.common.repository.search.ProblemPair;
import org.uniprot.api.idmapping.common.request.JobDetailResponse;
import org.uniprot.api.mapto.common.model.MapToJob;
import org.uniprot.api.mapto.common.model.MapToJobRequest;
import org.uniprot.api.mapto.common.model.MapToTask;
import org.uniprot.api.mapto.common.search.MapToSearchFacade;
import org.uniprot.api.mapto.common.search.MapToSearchService;
import org.uniprot.api.rest.download.model.JobStatus;
import org.uniprot.api.rest.output.job.JobStatusResponse;
import org.uniprot.api.rest.output.job.JobSubmitResponse;
import org.uniprot.store.config.UniProtDataType;

import net.jodah.failsafe.RetryPolicy;

@ExtendWith(MockitoExtension.class)
class MapToJobSubmissionServiceTest {
    public static final String ID = "jobId";
    public static final JobStatus STATUS = FINISHED;
    public static final int SIZE = 10;
    public static final UniProtDataType UNI_PROT_DATA_TYPE = UNIPROTKB;
    private static final UniProtDataType SOURCE_DB = UNIPROTKB;
    private static final UniProtDataType TARGET_DB = UNIREF;
    private static final String QUERY = "query";
    private static final LocalDateTime CREATED = LocalDateTime.now();
    private static final LocalDateTime UPDATED = LocalDateTime.now();
    private static final Boolean INCLUDE_ISOFORM = false;
    private static final Integer MAX_TARGET_ID_COUNT = 100;
    @Mock private ThreadPoolTaskExecutor jobTaskExecutor;
    @Mock private MapToHashGenerator hashGenerator;
    @Mock private MapToJobService mapToJobService;
    @Mock private MapToSearchFacade mapToSearchFacade;
    @Mock private RetryPolicy<Object> retryPolicy;
    private MapToJobSubmissionService mapToJobSubmissionService;
    @Mock public ProblemPair error;

    @Mock private MapToJobRequest mapToJobRequest;
    @Mock private MapToJob mapToJob;
    @Mock private MapToSearchService mapToSearchService;

    @BeforeEach
    void setUp() {
        mapToJobSubmissionService =
                new MapToJobSubmissionService(
                        jobTaskExecutor,
                        hashGenerator,
                        mapToJobService,
                        mapToSearchFacade,
                        retryPolicy,
                        MAX_TARGET_ID_COUNT);
        lenient().when(hashGenerator.generateHash(mapToJobRequest)).thenReturn(ID);
    }

    @Test
    void submit() {
        when(mapToJobService.createMapToJob(ID, mapToJobRequest)).thenReturn(mapToJob);
        when(mapToSearchFacade.getMapToSearchService(UNI_PROT_DATA_TYPE))
                .thenReturn(mapToSearchService);
        when(mapToJobRequest.getSource()).thenReturn(UNI_PROT_DATA_TYPE);

        JobSubmitResponse jobSubmitResponse = mapToJobSubmissionService.submit(mapToJobRequest);

        verify(jobTaskExecutor)
                .execute(
                        new MapToTask(
                                mapToSearchService,
                                mapToJobService,
                                mapToJob,
                                retryPolicy,
                                MAX_TARGET_ID_COUNT));
        assertSame(ID, jobSubmitResponse.getJobId());
    }

    @Test
    void submit_alreadyExistingJob() {
        when(mapToJobService.mapToJobExists(ID)).thenReturn(true);

        JobSubmitResponse jobSubmitResponse = mapToJobSubmissionService.submit(mapToJobRequest);
        verify(jobTaskExecutor, never()).execute(any());
        assertSame(ID, jobSubmitResponse.getJobId());
    }

    @Test
    void getJobStatus() {
        when(mapToJobService.findMapToJob(ID)).thenReturn(mapToJob);
        when(mapToJob.getStatus()).thenReturn(STATUS);
        when(mapToJob.getError()).thenReturn(error);
        when(mapToJob.getCreated()).thenReturn(CREATED);
        when(mapToJob.getUpdated()).thenReturn(UPDATED);

        JobStatusResponse jobStatus = mapToJobSubmissionService.getJobStatus(ID);

        assertSame(STATUS, jobStatus.getJobStatus());
        assertSame(error, jobStatus.getErrors().get(0));
        assertSame(CREATED, jobStatus.getStart());
        assertEquals(SIZE, (long) SIZE);
        assertSame(UPDATED, jobStatus.getLastUpdated());
    }

    @Test
    void getJobDetails_finished() {
        when(mapToJobService.findMapToJob(ID)).thenReturn(mapToJob);
        when(mapToJob.getSourceDB()).thenReturn(SOURCE_DB);
        when(mapToJob.getTargetDB()).thenReturn(TARGET_DB);
        when(mapToJob.getQuery()).thenReturn(QUERY);
        when(mapToJob.getIncludeIsoform()).thenReturn(INCLUDE_ISOFORM);
        when(mapToJob.getStatus()).thenReturn(FINISHED);

        String requestUrl = "requestUrl";
        String mappingType = "mappingType";
        JobDetailResponse jobDetails =
                mapToJobSubmissionService.getJobDetails(ID, requestUrl, mappingType);

        assertSame(SOURCE_DB.name(), jobDetails.getFrom());
        assertSame(TARGET_DB.name(), jobDetails.getTo());
        assertSame(QUERY, jobDetails.getQuery());
        assertFalse(jobDetails.getIncludeIsoform());
        assertEquals("requemappingType/results/jobId", jobDetails.getRedirectURL());
    }

    @Test
    void getJobDetails_notFinished() {
        when(mapToJobService.findMapToJob(ID)).thenReturn(mapToJob);
        when(mapToJob.getSourceDB()).thenReturn(SOURCE_DB);
        when(mapToJob.getTargetDB()).thenReturn(TARGET_DB);
        when(mapToJob.getQuery()).thenReturn(QUERY);
        when(mapToJob.getIncludeIsoform()).thenReturn(INCLUDE_ISOFORM);
        when(mapToJob.getStatus()).thenReturn(RUNNING);

        String requestUrl = "requestUrl";
        String mappingType = "mappingType";
        JobDetailResponse jobDetails =
                mapToJobSubmissionService.getJobDetails(ID, requestUrl, mappingType);

        assertSame(SOURCE_DB.name(), jobDetails.getFrom());
        assertSame(TARGET_DB.name(), jobDetails.getTo());
        assertSame(QUERY, jobDetails.getQuery());
        assertFalse(jobDetails.getIncludeIsoform());
        assertNull(jobDetails.getRedirectURL());
    }

    @Test
    void isJobFinished() {
        when(mapToJobService.findMapToJob(ID)).thenReturn(mapToJob);
        when(mapToJob.getStatus()).thenReturn(FINISHED);

        assertTrue(mapToJobSubmissionService.isJobFinished(ID));
    }

    @Test
    void isJobFinished_notFinished() {
        when(mapToJobService.findMapToJob(ID)).thenReturn(mapToJob);
        when(mapToJob.getStatus()).thenReturn(RUNNING);

        assertFalse(mapToJobSubmissionService.isJobFinished(ID));
    }
}
