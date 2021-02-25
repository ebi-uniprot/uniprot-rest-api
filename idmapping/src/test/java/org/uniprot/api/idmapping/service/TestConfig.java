package org.uniprot.api.idmapping.service;

import java.util.Collections;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCacheFactoryBean;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.uniprot.api.idmapping.model.IdMappingJob;
import org.uniprot.api.idmapping.service.cache.IdMappingJobCacheService;
import org.uniprot.api.idmapping.service.cache.impl.EhCacheMappingJobService;
import org.uniprot.api.idmapping.service.job.AsyncJobProducer;

/**
 * @author sahmad
 * @created 24/02/2021
 */
@TestConfiguration
public class TestConfig {
    @Bean
    @Profile("offline")
    public CacheManager cacheManager(Cache fakeCache) {
        SimpleCacheManager cacheManager = new SimpleCacheManager();
        cacheManager.setCaches(Collections.singletonList(fakeCache));
        return cacheManager;
    }

    @Bean
    @Profile("offline")
    public Cache fakeCache() {
        ConcurrentMapCacheFactoryBean cacheFactoryBean = new ConcurrentMapCacheFactoryBean();
        cacheFactoryBean.setName("fakeCache");
        cacheFactoryBean.afterPropertiesSet();
        return cacheFactoryBean.getObject();
    }

    @Bean
    @Profile("offline")
    public IdMappingJobCacheService cacheService(CacheManager cacheManager) {
        Cache cache = cacheManager.getCache("fakeCache");
        return new EhCacheMappingJobService(cache);
    }

    @Bean
    @Profile("offline")
    public BlockingQueue<IdMappingJob> testQueue() {
        return new LinkedBlockingQueue<>(5);
    }

    @Bean
    @Profile("offline")
    public AsyncJobProducer testJobProducer(BlockingQueue<IdMappingJob> queue) {
        return new AsyncJobProducer(queue);
    }

    @Bean
    @Profile("offline")
    public ThreadPoolTaskExecutor jobTaskExecutor(){
        return new ThreadPoolTaskExecutor();
    }
}
