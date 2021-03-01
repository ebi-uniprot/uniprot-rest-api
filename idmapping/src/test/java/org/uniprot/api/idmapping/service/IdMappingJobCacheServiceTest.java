package org.uniprot.api.idmapping.service;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.uniprot.api.common.exception.ResourceNotFoundException;
import org.uniprot.api.idmapping.controller.response.JobStatus;
import org.uniprot.api.idmapping.model.IdMappingJob;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;

class IdMappingJobCacheServiceTest {
    private static final String NEW_JOB_IN_CACHE = "new job";
    private static final String FINISHED_JOB_IN_CACHE = "finished job";
    private static final String NON_EXISTENT_JOB_IN_CACHE = "non existent job";
    private static FakeIdMappingJobCacheService cacheService;

    @BeforeAll
    static void setUp() {
        cacheService = new FakeIdMappingJobCacheService();
    }

    @Test
    void canGetJobAsResourceSuccessfully() {
        assertThat(cacheService.getJobAsResource(NEW_JOB_IN_CACHE), is(notNullValue()));
    }

    @Test
    void nonExistententJobAsResourceThrowsException() {
        assertThrows(
                ResourceNotFoundException.class,
                () -> cacheService.getJobAsResource(NON_EXISTENT_JOB_IN_CACHE));
    }

    @Test
    void canGetCompletedJobAsResourcesSuccessfully() {
        assertThat(cacheService.getJobAsResource(FINISHED_JOB_IN_CACHE), is(notNullValue()));
    }

    @Test
    void nonExsistentCompletedJobAsResourceThrowsException() {
        assertThrows(
                ResourceNotFoundException.class,
                () -> cacheService.getCompletedJobAsResource(NON_EXISTENT_JOB_IN_CACHE));
    }

    static class FakeIdMappingJobCacheService implements IdMappingJobCacheService {
        @Override
        public void put(String key, IdMappingJob value) {
            throw new UnsupportedOperationException();
        }

        @Override
        public IdMappingJob get(String key) {
            if (key.equals(NEW_JOB_IN_CACHE)) {
                return IdMappingJob.builder().jobId(NEW_JOB_IN_CACHE).build();
            } else if (key.equals(FINISHED_JOB_IN_CACHE)) {
                return IdMappingJob.builder()
                        .jobStatus(JobStatus.FINISHED)
                        .jobId(FINISHED_JOB_IN_CACHE)
                        .build();
            } else {
                return null;
            }
        }

        @Override
        public boolean exists(String key) {
            return key.equals(NEW_JOB_IN_CACHE) || key.equals(FINISHED_JOB_IN_CACHE);
        }

        @Override
        public void delete(String key) {
            throw new UnsupportedOperationException();
        }
    }
}
