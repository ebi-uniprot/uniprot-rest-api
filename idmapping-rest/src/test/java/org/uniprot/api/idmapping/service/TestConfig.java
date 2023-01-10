package org.uniprot.api.idmapping.service;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.redisson.api.RedissonClient;
import org.redisson.spring.cache.CacheConfig;
import org.redisson.spring.cache.RedissonSpringCacheManager;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.uniprot.api.idmapping.service.impl.RedisCacheMappingJobService;

/**
 * @author sahmad
 * @created 24/02/2021
 */
@TestConfiguration
public class TestConfig {
    //    private final RedisServer redisServer;
    //
    //    public TestConfig() {
    //        this.redisServer = new RedisServer(6379);
    //    }
    //
    //    @PostConstruct
    //    public void postConstruct() {
    //        try {
    //            this.redisServer.start();
    //        } catch (RuntimeException rte) {
    //            // already running
    //        }
    //    }
    //
    //    @PreDestroy
    //    public void preDestroy() {
    //        this.redisServer.stop();
    //    }
    //
    //    @Bean(destroyMethod = "shutdown")
    //    @Profile("offline")
    //    RedissonClient redisson() throws IOException {
    //        Config config = new Config();
    //        config.useSingleServer().setAddress("redis://127.0.0.1:6379");
    //        return Redisson.create(config);
    //    }

    @Bean
    @Profile("offline")
    public IdMappingJobCacheService idMappingJobCacheService(RedissonClient redissonClient)
            throws IOException {
        Map<String, CacheConfig> config = new HashMap<>();
        config.put("testMap", null);
        CacheManager cacheManager = new RedissonSpringCacheManager(redissonClient, config);
        Cache mappingCache = cacheManager.getCache("testMap");
        return new RedisCacheMappingJobService(mappingCache);
    }

    @Bean
    @Profile("offline")
    public ThreadPoolTaskExecutor jobTaskExecutor() {
        return new ThreadPoolTaskExecutor();
    }
}
