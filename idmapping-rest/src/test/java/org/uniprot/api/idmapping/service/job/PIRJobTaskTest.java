package org.uniprot.api.idmapping.service.job;

import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.uniprot.store.config.idmapping.IdMappingFieldConfig.ACC_ID_STR;
import static org.uniprot.store.config.idmapping.IdMappingFieldConfig.UNIPROTKB_STR;

import java.io.IOException;
import java.util.List;

import org.apache.solr.client.solrj.SolrServerException;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClientException;
import org.uniprot.api.common.repository.search.ProblemPair;
import org.uniprot.api.idmapping.model.IdMappingJob;
import org.uniprot.api.idmapping.model.IdMappingResult;
import org.uniprot.api.idmapping.model.IdMappingStringPair;
import org.uniprot.api.idmapping.repository.IdMappingRepository;
import org.uniprot.api.idmapping.service.IdMappingJobCacheService;
import org.uniprot.api.idmapping.service.IdMappingPIRService;
import org.uniprot.api.rest.request.idmapping.IdMappingJobRequest;
import org.uniprot.store.search.SolrCollection;

class PIRJobTaskTest {

    @Test
    void processTaskWithUnexpectedException() {
        IdMappingJob job = mock(IdMappingJob.class);
        IdMappingJobCacheService cacheService = mock(IdMappingJobCacheService.class);
        IdMappingPIRService pirService = mock(IdMappingPIRService.class);
        IdMappingRepository repo = mock(IdMappingRepository.class);
        PIRJobTask pirJobTask = new PIRJobTask(null, null, pirService, repo);
        when(pirService.mapIds(any(), any())).thenThrow(new IndexOutOfBoundsException("pir error"));

        IdMappingResult idMappingResult = pirJobTask.processTask(job);

        List<ProblemPair> errors = idMappingResult.getErrors();
        assertEquals(1, errors.size());
        assertEquals(50, errors.get(0).getCode());
        assertEquals("Internal server error.", errors.get(0).getMessage());
        assertNull(idMappingResult.getObsoleteCount());
    }

    @Test
    void processTaskWithRestClientException() {
        IdMappingJob job = mock(IdMappingJob.class);
        IdMappingPIRService pirService = mock(IdMappingPIRService.class);
        IdMappingRepository repo = mock(IdMappingRepository.class);
        PIRJobTask pirJobTask = new PIRJobTask(null, null, pirService, repo);
        when(pirService.mapIds(any(), any()))
                .thenThrow(new RestClientException("pir RestClientException"));

        IdMappingResult idMappingResult = pirJobTask.processTask(job);

        List<ProblemPair> errors = idMappingResult.getErrors();
        assertEquals(1, errors.size());
        assertEquals(50, errors.get(0).getCode());
        assertEquals("pir RestClientException", errors.get(0).getMessage());
        assertNull(idMappingResult.getObsoleteCount());
    }

    @Test
    void processTaskWithObsoleteEntries() throws SolrServerException, IOException {
        // when
        IdMappingJob job = mock(IdMappingJob.class);
        IdMappingJobRequest request = new IdMappingJobRequest();
        request.setFrom(ACC_ID_STR);
        request.setTo(UNIPROTKB_STR);
        IdMappingPIRService pirService = mock(IdMappingPIRService.class);
        IdMappingRepository repo = mock(IdMappingRepository.class);
        PIRJobTask pirJobTask = new PIRJobTask(null, null, pirService, repo);
        IdMappingResult.IdMappingResultBuilder resultBuilder = IdMappingResult.builder();
        resultBuilder.mappedIds(
                List.of(
                        new IdMappingStringPair("P12345", "P12345"),
                        new IdMappingStringPair("Q12345", "Q12345"),
                        new IdMappingStringPair("P05067", "P05067")));
        IdMappingResult result = resultBuilder.build();
        List<IdMappingStringPair> obsoletePairs =
                List.of(
                        new IdMappingStringPair("Q12345", "Q12345"),
                        new IdMappingStringPair("P12345", "P12345"));
        when(job.getIdMappingRequest()).thenReturn(request);
        when(pirService.mapIds(any(), any())).thenReturn(result);
        when(repo.getAllMappingIds(
                        SolrCollection.uniprot,
                        List.of("P12345", "Q12345", "P05067"),
                        "active:false"))
                .thenReturn(obsoletePairs);
        // then
        IdMappingResult idmappingResult = pirJobTask.processTask(job);
        // verify
        assertTrue(idmappingResult.getErrors().isEmpty());
        assertEquals(3, idmappingResult.getMappedIds().size());
        assertEquals(2, idmappingResult.getObsoleteCount());
    }
}
