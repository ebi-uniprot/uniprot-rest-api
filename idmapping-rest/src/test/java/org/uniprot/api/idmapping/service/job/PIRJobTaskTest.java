package org.uniprot.api.idmapping.service.job;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClientException;
import org.uniprot.api.common.repository.search.ProblemPair;
import org.uniprot.api.idmapping.model.IdMappingJob;
import org.uniprot.api.idmapping.model.IdMappingResult;
import org.uniprot.api.idmapping.service.IdMappingJobCacheService;
import org.uniprot.api.idmapping.service.IdMappingPIRService;

class PIRJobTaskTest {

    @Test
    void processTaskWithUnexpectedException() {
        IdMappingJob job = mock(IdMappingJob.class);
        IdMappingJobCacheService cacheService = mock(IdMappingJobCacheService.class);
        IdMappingPIRService pirService = mock(IdMappingPIRService.class);
        PIRJobTask pirJobTask = new PIRJobTask(null, null, pirService);
        when(pirService.mapIds(any(), any())).thenThrow(new IndexOutOfBoundsException("pir error"));

        IdMappingResult idMappingResult = pirJobTask.processTask(job);

        List<ProblemPair> errors = idMappingResult.getErrors();
        assertEquals(1, errors.size());
        assertEquals(50, errors.get(0).getCode());
        assertEquals("Internal server error.", errors.get(0).getMessage());
    }

    @Test
    void processTaskWithRestClientException() {
        IdMappingJob job = mock(IdMappingJob.class);
        IdMappingJobCacheService cacheService = mock(IdMappingJobCacheService.class);
        IdMappingPIRService pirService = mock(IdMappingPIRService.class);
        PIRJobTask pirJobTask = new PIRJobTask(null, null, pirService);
        when(pirService.mapIds(any(), any()))
                .thenThrow(new RestClientException("pir RestClientException"));

        IdMappingResult idMappingResult = pirJobTask.processTask(job);

        List<ProblemPair> errors = idMappingResult.getErrors();
        assertEquals(1, errors.size());
        assertEquals(50, errors.get(0).getCode());
        assertEquals("pir RestClientException", errors.get(0).getMessage());
    }
}
