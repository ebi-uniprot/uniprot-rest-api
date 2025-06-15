package org.uniprot.api.mapto.common.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.uniprot.api.rest.download.model.JobStatus.*;
import static org.uniprot.api.rest.download.model.JobStatus.FINISHED;
import static org.uniprot.store.config.UniProtDataType.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

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
    @Mock private ThreadPoolTaskExecutor jobTaskExecutor;
    @Mock private MapToHashGenerator hashGenerator;
    @Mock private MapToJobService mapToJobService;
    @Mock private MapToSearchFacade mapToSearchFacade;
    @Mock private RetryPolicy<Object> retryPolicy;
    private MapToJobSubmissionService mapToJobSubmissionService;
    @Mock public List<ProblemPair> errors;

    @Mock private MapToJobRequest mapToJobRequest;
    @Mock private MapToJob mapToJob;
    @Mock private MapToSearchService mapToSearchService;
    @Mock private List<String> targetIds;
    @Mock private Map<String, String> extraParams;
    private Integer maxTargetIdCount = 100;

    @BeforeEach
    void setUp() {
        mapToJobSubmissionService =
                new MapToJobSubmissionService(
                        jobTaskExecutor,
                        hashGenerator,
                        mapToJobService,
                        mapToSearchFacade,
                        retryPolicy,
                        maxTargetIdCount);
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
                                maxTargetIdCount));
        assertSame(ID, jobSubmitResponse.getJobId());
    }

    @Test
    void submit_alreadyExistingJob() {
        when(mapToJobService.mapToJobExists(ID)).thenReturn(true);

        JobSubmitResponse jobSubmitResponse = mapToJobSubmissionService.submit(mapToJobRequest);
        verify(jobTaskExecutor, never()).execute(any());
        verify(mapToJobService).updateUpdated(ID);
        assertSame(ID, jobSubmitResponse.getJobId());
    }

    @Test
    void getJobStatus() {
        when(mapToJobService.findMapToJob(ID)).thenReturn(mapToJob);
        when(mapToJob.getStatus()).thenReturn(STATUS);
        when(mapToJob.getErrors()).thenReturn(errors);
        when(mapToJob.getCreated()).thenReturn(CREATED);
        when(mapToJob.getTargetIds()).thenReturn(targetIds);
        when(targetIds.size()).thenReturn(SIZE);
        when(mapToJob.getUpdated()).thenReturn(UPDATED);

        JobStatusResponse jobStatus = mapToJobSubmissionService.getJobStatus(ID);
        assertSame(STATUS, jobStatus.getJobStatus());
        assertSame(errors, jobStatus.getErrors());
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
        when(mapToJob.getExtraParams()).thenReturn(extraParams);
        when(mapToJob.getStatus()).thenReturn(FINISHED);

        String requestUrl = "requestUrl";
        String mappingType = "mappingType";
        JobDetailResponse jobDetails =
                mapToJobSubmissionService.getJobDetails(ID, requestUrl, mappingType);

        assertSame(SOURCE_DB.name(), jobDetails.getFrom());
        assertSame(TARGET_DB.name(), jobDetails.getTo());
        assertSame(QUERY, jobDetails.getQuery());
        assertNull(jobDetails.getIncludeIsoform());
        assertEquals("requemappingType/results/jobId", jobDetails.getRedirectURL());
    }

    @Test
    void getJobDetails_notFinished() {
        when(mapToJobService.findMapToJob(ID)).thenReturn(mapToJob);
        when(mapToJob.getSourceDB()).thenReturn(SOURCE_DB);
        when(mapToJob.getTargetDB()).thenReturn(TARGET_DB);
        when(mapToJob.getQuery()).thenReturn(QUERY);
        when(mapToJob.getExtraParams()).thenReturn(extraParams);
        when(mapToJob.getStatus()).thenReturn(RUNNING);

        String requestUrl = "requestUrl";
        String mappingType = "mappingType";
        JobDetailResponse jobDetails =
                mapToJobSubmissionService.getJobDetails(ID, requestUrl, mappingType);

        assertSame(SOURCE_DB.name(), jobDetails.getFrom());
        assertSame(TARGET_DB.name(), jobDetails.getTo());
        assertSame(QUERY, jobDetails.getQuery());
        assertNull(jobDetails.getIncludeIsoform());
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
