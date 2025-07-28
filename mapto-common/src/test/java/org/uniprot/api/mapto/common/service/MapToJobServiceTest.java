package org.uniprot.api.mapto.common.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.uniprot.api.rest.download.model.JobStatus.*;
import static org.uniprot.store.config.UniProtDataType.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.uniprot.api.common.exception.ResourceNotFoundException;
import org.uniprot.api.common.repository.search.ProblemPair;
import org.uniprot.api.mapto.common.model.MapToJob;
import org.uniprot.api.mapto.common.model.MapToJobRequest;
import org.uniprot.api.mapto.common.repository.MapToJobRepository;
import org.uniprot.api.mapto.common.repository.MapToResultRepository;
import org.uniprot.api.rest.download.model.JobStatus;
import org.uniprot.store.config.UniProtDataType;

@ExtendWith(MockitoExtension.class)
public class MapToJobServiceTest {

    public static final JobStatus JOB_STATUS = RUNNING;
    public static final String ID = "id";
    private static final List<String> TARGET_IDS = List.of("abc", "def", "ghi");
    public static final UniProtDataType SOURCE = UNIPROTKB;
    public static final UniProtDataType TARGET = UNIREF;
    public static final String QUERY = "query";
    @Mock private MapToJobRepository jobRepository;
    @Mock private MapToResultRepository mapToResultRepository;

    @Mock private MapToJob mapToJob;

    private MapToJobService jobService;
    @Mock private MapToJobRequest mapToJobRequest;
    @Mock private Map<String, String> extraParams;
    @Captor private ArgumentCaptor<MapToJob> mapToJobCaptor;
    @Mock private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        this.jobService = new MapToJobService(jobRepository, jdbcTemplate);
    }

    @Test
    void testCreate() {
        this.jobService.createMapToJob(mapToJob);
        verify(jobRepository).save(mapToJob);
    }

    @Test
    void testCreateDuplicateJob() {
        String id = "ID";
        when(this.mapToJob.getJobId()).thenReturn(id);
        when(this.jobRepository.findByJobId(id)).thenReturn(Optional.of(mapToJob));
        assertThrows(RuntimeException.class, () -> this.jobService.createMapToJob(mapToJob));
    }

    @Test
    void testFindById() {
        String id = "ID";
        when(this.jobRepository.findByJobId(id)).thenReturn(Optional.of(this.mapToJob));
        Assertions.assertEquals(this.mapToJob, this.jobService.findMapToJob(id));
    }

    @Test
    void testDelete() {
        String id = "ID";
        jobService.deleteMapToJob(id);
        verify(this.jobRepository).deleteByJobId(id);
    }

    @Test
    void testExists() {
        String id = "ID";
        Boolean answer = true;
        when(this.jobRepository.existsByJobId(id)).thenReturn(answer);
        Assertions.assertEquals(answer, jobService.mapToJobExists(id));
    }

    @Test
    void updateStatus() {
        when(jobRepository.findByJobId(ID)).thenReturn(Optional.of(mapToJob));

        jobService.updateStatus(ID, JOB_STATUS);

        verify(mapToJob).setStatus(JOB_STATUS);
        verify(jobRepository).save(mapToJob);
    }

    @Test
    void updateUpdated() {
        when(jobRepository.findByJobId(ID)).thenReturn(Optional.of(mapToJob));

        jobService.updateUpdated(ID);

        verify(jobRepository).save(mapToJob);
    }

    @Test
    void setErrors() {
        when(jobRepository.findByJobId(ID)).thenReturn(Optional.of(mapToJob));

        ProblemPair error = new ProblemPair(20, "message");
        jobService.setErrors(ID, error);

        verify(mapToJob).setError(error);
        verify(mapToJob).setStatus(ERROR);
        verify(jobRepository).save(mapToJob);
    }

    @Test
    void updateStatus_whenJobNotExist() {
        when(jobRepository.findByJobId(ID)).thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class, () -> jobService.updateStatus(ID, JOB_STATUS));
    }

    @Test
    void setTargetIds() {
        when(jobRepository.findByJobId(ID)).thenReturn(Optional.of(mapToJob));

        jobService.setTargetIds(ID, TARGET_IDS);

        verify(jdbcTemplate).batchUpdate(anyString(), anyList(), eq(TARGET_IDS.size()), any());
        verify(jobRepository).save(mapToJob);
    }

    @Test
    void setTargetIds_whenJobNotExist() {
        when(jobRepository.findByJobId(ID)).thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class, () -> jobService.setTargetIds(ID, TARGET_IDS));
    }

    @Test
    void createMapToJob() {
        when(mapToJobRequest.getSource()).thenReturn(SOURCE);
        when(mapToJobRequest.getTarget()).thenReturn(TARGET);
        when(mapToJobRequest.getQuery()).thenReturn(QUERY);
        when(mapToJobRequest.getExtraParams()).thenReturn(extraParams);

        jobService.createMapToJob(ID, mapToJobRequest);

        verify(jobRepository).save(mapToJobCaptor.capture());
        MapToJob capturedMapToJob = mapToJobCaptor.getValue();
        assertSame(SOURCE, capturedMapToJob.getSourceDB());
        assertSame(TARGET, capturedMapToJob.getTargetDB());
        assertSame(QUERY, capturedMapToJob.getQuery());
        assertSame(
                Boolean.valueOf(extraParams.get("includeIsoform")),
                capturedMapToJob.getIncludeIsoform());
        assertSame(capturedMapToJob.getCreated(), capturedMapToJob.getUpdated());
    }
}
