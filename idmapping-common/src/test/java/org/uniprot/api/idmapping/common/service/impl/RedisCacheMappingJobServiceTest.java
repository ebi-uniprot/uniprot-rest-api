package org.uniprot.api.idmapping.common.service.impl;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.nullValue;

import java.util.Collections;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.cache.Cache;
import org.springframework.cache.concurrent.ConcurrentMapCacheFactoryBean;
import org.springframework.cache.support.SimpleCacheManager;
import org.uniprot.api.idmapping.common.model.IdMappingJob;
import org.uniprot.api.idmapping.common.model.IdMappingResult;
import org.uniprot.api.idmapping.common.response.model.IdMappingStringPair;
import org.uniprot.api.rest.download.model.JobStatus;

class RedisCacheMappingJobServiceTest {
    private Cache fakeCache;
    private RedisCacheMappingJobService jobService;

    @BeforeEach
    void setUp() {
        ConcurrentMapCacheFactoryBean cacheFactoryBean = new ConcurrentMapCacheFactoryBean();
        cacheFactoryBean.setName("fakeCache");
        cacheFactoryBean.afterPropertiesSet();
        this.fakeCache = cacheFactoryBean.getObject();

        SimpleCacheManager cacheManager = new SimpleCacheManager();
        cacheManager.setCaches(Collections.singletonList(fakeCache));
        this.jobService = new RedisCacheMappingJobService(this.fakeCache);
    }

    @Test
    void canPutThenGet() {
        String id = "id";
        assertThat(fakeCache.get(id, IdMappingJob.class), is(nullValue()));

        IdMappingResult result =
                IdMappingResult.builder().mappedId(new IdMappingStringPair("from1", "to1")).build();
        IdMappingJob job =
                IdMappingJob.builder()
                        .jobStatus(JobStatus.FINISHED)
                        .idMappingResult(result)
                        .build();

        jobService.put(id, job);

        assertThat(fakeCache.get(id, IdMappingJob.class), is(job));
        assertThat(jobService.get(id), is(job));
    }

    @Test
    void gettingNonExistentKeyReturnsNull() {
        assertThat(
                fakeCache.get("THIS IS A KEY THAT DOES NOT EXIST", IdMappingJob.class),
                is(nullValue()));
    }

    @Test
    void canOverwriteValue() {
        String id = "id";
        assertThat(fakeCache.get(id, IdMappingJob.class), is(nullValue()));

        IdMappingResult value1 = IdMappingResult.builder().build();
        IdMappingJob job1 =
                IdMappingJob.builder().jobStatus(JobStatus.RUNNING).idMappingResult(value1).build();

        IdMappingResult value2 =
                IdMappingResult.builder().mappedId(new IdMappingStringPair("from1", "to1")).build();
        IdMappingJob job2 =
                IdMappingJob.builder()
                        .jobStatus(JobStatus.FINISHED)
                        .idMappingResult(value2)
                        .build();

        // put
        jobService.put(id, job1);
        assertThat(jobService.get(id), is(job1));

        // overwrite with second put
        jobService.put(id, job2);
        assertThat(jobService.get(id), is(job2));
    }

    @Test
    void checkExists() {
        String id = "id";
        IdMappingResult result =
                IdMappingResult.builder().mappedId(new IdMappingStringPair("from1", "to1")).build();
        IdMappingJob job =
                IdMappingJob.builder()
                        .jobStatus(JobStatus.FINISHED)
                        .idMappingResult(result)
                        .build();

        assertThat(jobService.exists(id), is(false));

        jobService.put(id, job);
        assertThat(jobService.exists(id), is(true));
    }

    @Test
    void canDelete() {
        String id = "id";
        IdMappingResult result =
                IdMappingResult.builder().mappedId(new IdMappingStringPair("from1", "to1")).build();
        IdMappingJob job =
                IdMappingJob.builder()
                        .jobStatus(JobStatus.FINISHED)
                        .idMappingResult(result)
                        .build();

        jobService.put(id, job);
        assertThat(jobService.exists(id), is(true));

        jobService.delete(id);
        assertThat(jobService.exists(id), is(false));
    }
}
