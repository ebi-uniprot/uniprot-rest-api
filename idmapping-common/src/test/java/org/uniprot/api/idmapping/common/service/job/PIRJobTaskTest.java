package org.uniprot.api.idmapping.common.service.job;

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
import org.uniprot.api.idmapping.common.model.IdMappingJob;
import org.uniprot.api.idmapping.common.model.IdMappingResult;
import org.uniprot.api.idmapping.common.repository.IdMappingRepository;
import org.uniprot.api.idmapping.common.request.IdMappingJobRequest;
import org.uniprot.api.idmapping.common.response.model.IdMappingStringPair;
import org.uniprot.api.idmapping.common.service.IdMappingJobCacheService;
import org.uniprot.api.idmapping.common.service.IdMappingPIRService;
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

    @Test
    void testGetObsoleteUniProtEntryCountBatchingWithLastBatchLessThanBatchSize()
            throws SolrServerException, IOException {
        // when
        IdMappingRepository repo = mock(IdMappingRepository.class);
        PIRJobTask pirJobTask = new PIRJobTask(null, null, null, repo);
        IdMappingResult.IdMappingResultBuilder resultBuilder = IdMappingResult.builder();
        List<IdMappingStringPair> allPairs =
                List.of(
                        new IdMappingStringPair("P00000", "P00000"),
                        new IdMappingStringPair("P00001", "P00001"),
                        new IdMappingStringPair("P00002", "P00002"),
                        new IdMappingStringPair("P00003", "P00003"),
                        new IdMappingStringPair("P00004", "P00004"));
        List<IdMappingStringPair> obsoletePairs1 =
                List.of(
                        new IdMappingStringPair("P00000", "P00000"),
                        new IdMappingStringPair("P00001", "P00001"));
        List<IdMappingStringPair> obsoletePairs2 =
                List.of(new IdMappingStringPair("P00004", "P00004"));
        when(repo.getAllMappingIds(
                        SolrCollection.uniprot, List.of("P00000", "P00001"), "active:false"))
                .thenReturn(obsoletePairs1);
        when(repo.getAllMappingIds(
                        SolrCollection.uniprot, List.of("P00002", "P00003"), "active:false"))
                .thenReturn(List.of());
        when(repo.getAllMappingIds(SolrCollection.uniprot, List.of("P00004"), "active:false"))
                .thenReturn(obsoletePairs2);
        resultBuilder.mappedIds(allPairs);
        IdMappingResult result = resultBuilder.build();
        // then
        int batchSize = 2;
        Integer obsoleteCount = pirJobTask.getObsoleteUniProtEntryCount(result, batchSize);
        assertEquals(3, obsoleteCount);
    }

    @Test
    void testGetObsoleteUniProtEntryCountBatchingWithLastBatchSameAsBatchSize()
            throws SolrServerException, IOException {
        // when
        IdMappingRepository repo = mock(IdMappingRepository.class);
        PIRJobTask pirJobTask = new PIRJobTask(null, null, null, repo);
        IdMappingResult.IdMappingResultBuilder resultBuilder = IdMappingResult.builder();
        List<IdMappingStringPair> allPairs =
                List.of(
                        new IdMappingStringPair("P00000", "P00000"),
                        new IdMappingStringPair("P00001", "P00001"),
                        new IdMappingStringPair("P00002", "P00002"),
                        new IdMappingStringPair("P00003", "P00003"));
        List<IdMappingStringPair> obsoletePairs1 =
                List.of(
                        new IdMappingStringPair("P00000", "P00000"),
                        new IdMappingStringPair("P00001", "P00001"));
        when(repo.getAllMappingIds(
                        SolrCollection.uniprot, List.of("P00000", "P00001"), "active:false"))
                .thenReturn(obsoletePairs1);
        when(repo.getAllMappingIds(
                        SolrCollection.uniprot, List.of("P00002", "P00003"), "active:false"))
                .thenReturn(List.of(new IdMappingStringPair("P00003", "P00003")));
        resultBuilder.mappedIds(allPairs);
        IdMappingResult result = resultBuilder.build();
        // then
        int batchSize = 2;
        Integer obsoleteCount = pirJobTask.getObsoleteUniProtEntryCount(result, batchSize);
        assertEquals(3, obsoleteCount);
    }
}
