package org.uniprot.api.idmapping.common.service.job;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

import java.io.IOException;
import java.util.List;

import org.apache.solr.client.solrj.SolrServerException;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.uniprot.api.common.repository.search.ProblemPair;
import org.uniprot.api.idmapping.common.model.IdMappingJob;
import org.uniprot.api.idmapping.common.model.IdMappingResult;
import org.uniprot.api.idmapping.common.repository.IdMappingRepository;
import org.uniprot.api.idmapping.common.request.IdMappingJobRequest;
import org.uniprot.api.idmapping.common.response.model.IdMappingStringPair;
import org.uniprot.api.idmapping.common.service.IdMappingJobCacheService;
import org.uniprot.store.search.SolrCollection;

class SolrJobTaskTest {
    private SolrJobTask solrJobTask =
            new SolrJobTask(
                    mock(IdMappingJob.class),
                    mock(IdMappingJobCacheService.class),
                    mock(IdMappingRepository.class));

    @Test
    void taskProcessingWillGetRequestFromJobObject() {
        var idMappingJob = mappingJob("UniParc", "unknown db");

        solrJobTask.processTask(idMappingJob);

        verify(idMappingJob, atLeast(1)).getIdMappingRequest();
    }

    @Test
    void unknownToDBWillCauseErrorMsg() {
        var idMappingJob = mappingJob("UniParc", "unknown db");

        IdMappingResult actualProcessTaskResult = solrJobTask.processTask(idMappingJob);

        List<ProblemPair> errors = actualProcessTaskResult.getErrors();
        assertEquals(1, errors.size());
        assertEquals(50, errors.get(0).getCode());
        assertEquals("unsupported collection", errors.get(0).getMessage());
    }

    @Test
    void unknownToDbWillNotHaveWarningMsg() {
        var idMappingJob = mappingJob("UniParc", "unknown db");

        IdMappingResult actualProcessTaskResult = solrJobTask.processTask(idMappingJob);

        List<ProblemPair> warnings = actualProcessTaskResult.getWarnings();
        assertTrue(warnings.isEmpty());
    }

    @Test
    void solrRepoThrowException() throws SolrServerException, IOException {
        IdMappingRepository repo = mockRepo();
        when(repo.getAllMappingIds(any(), any())).thenThrow(new SolrServerException("solr error"));

        var idMappingResult = solrJobTask.processTask(mappingJob("db", "UniRef50"));

        List<ProblemPair> errors = idMappingResult.getErrors();
        assertEquals(1, errors.size());
        assertEquals(50, errors.get(0).getCode());
        assertEquals("Mapping request got failed", errors.get(0).getMessage());
    }

    @Nested
    class RepoWillGetCorrectCollectionTest {
        @ParameterizedTest
        @ValueSource(strings = {"UniRef50", "UniRef90", "UniRef100"})
        void repoWillAskedToLookIntoUniref(String db) throws SolrServerException, IOException {
            IdMappingRepository repo = mockRepo();

            solrJobTask.processTask(mappingJob(db, db));

            verify(repo, atLeastOnce()).getAllMappingIds(SolrCollection.uniref, List.of("Ids"));
        }

        @ParameterizedTest
        @ValueSource(strings = {"UniProtKB_AC-ID", "UniProtKB-Swiss-Prot", "UniProtKB"})
        void repoWillAskedToLookIntoUniprotkb(String db) throws SolrServerException, IOException {
            IdMappingRepository repo = mockRepo();

            solrJobTask.processTask(mappingJob(db, db));

            verify(repo, atLeastOnce()).getAllMappingIds(SolrCollection.uniprot, List.of("Ids"));
        }

        @Test
        void repoWillAskedToLookIntoUniparc() throws SolrServerException, IOException {
            IdMappingRepository repo = mockRepo();

            solrJobTask.processTask(mappingJob("UniParc", "UniParc"));

            verify(repo, atLeastOnce()).getAllMappingIds(SolrCollection.uniparc, List.of("Ids"));
        }
    }

    @Test
    void allIdsMatched() throws SolrServerException, IOException {
        IdMappingRepository repo = mockRepo();
        when(repo.getAllMappingIds(any(), any()))
                .thenReturn(List.of(new IdMappingStringPair("from", "to")));

        var idMappingResult =
                solrJobTask.processTask(mappingJob("UniProtKB_AC-ID", "UniProtKB_AC-ID"));

        assertTrue(idMappingResult.getErrors().isEmpty());
        assertTrue(idMappingResult.getUnmappedIds().isEmpty());
        assertFalse(idMappingResult.getMappedIds().isEmpty());
        assertEquals("to", idMappingResult.getMappedIds().get(0).getTo());
    }

    @Test
    void noIdsMatched() throws SolrServerException, IOException {
        IdMappingRepository repo = mockRepo();
        when(repo.getAllMappingIds(any(), any())).thenReturn(List.of());

        var idMappingResult =
                solrJobTask.processTask(mappingJob("UniProtKB_AC-ID", "UniProtKB_AC-ID"));

        assertTrue(idMappingResult.getErrors().isEmpty());
        assertFalse(idMappingResult.getUnmappedIds().isEmpty());
        assertEquals("Ids", idMappingResult.getUnmappedIds().get(0));
        assertTrue(idMappingResult.getMappedIds().isEmpty());
    }

    @Test
    void someIdsMatched() throws SolrServerException, IOException {
        IdMappingRepository repo = mockRepo();
        when(repo.getAllMappingIds(any(), any()))
                .thenReturn(List.of(new IdMappingStringPair("2", "2")));

        var idMappingResult =
                solrJobTask.processTask(mappingJob("UniProtKB_AC-ID", "UniProtKB_AC-ID", "1,2,3"));

        assertTrue(idMappingResult.getErrors().isEmpty());
        assertFalse(idMappingResult.getMappedIds().isEmpty());
        assertEquals("2", idMappingResult.getMappedIds().get(0).getTo());
        assertFalse(idMappingResult.getUnmappedIds().isEmpty());
        assertEquals("1", idMappingResult.getUnmappedIds().get(0));
        assertEquals("3", idMappingResult.getUnmappedIds().get(1));
    }

    private IdMappingJob mappingJob(String from, String to) {
        return mappingJob(from, to, "Ids");
    }

    private IdMappingJob mappingJob(String from, String to, String ids) {
        IdMappingJob idMappingJob = mock(IdMappingJob.class);
        IdMappingJobRequest idMappingJobRequest = new IdMappingJobRequest();
        idMappingJobRequest.setFrom(from);
        idMappingJobRequest.setIds(ids);
        idMappingJobRequest.setTaxId("42");
        idMappingJobRequest.setTo(to);
        when(idMappingJob.getIdMappingRequest()).thenReturn(idMappingJobRequest);
        return idMappingJob;
    }

    private IdMappingRepository mockRepo() {
        IdMappingRepository repo = mock(IdMappingRepository.class);
        solrJobTask =
                new SolrJobTask(
                        mock(IdMappingJob.class), mock(IdMappingJobCacheService.class), repo);
        return repo;
    }
}
