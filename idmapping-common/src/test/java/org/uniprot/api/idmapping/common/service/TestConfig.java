package org.uniprot.api.idmapping.common.service;

import java.util.HashMap;
import java.util.Map;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.redisson.spring.cache.CacheConfig;
import org.redisson.spring.cache.RedissonSpringCacheManager;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.uniprot.api.idmapping.common.service.impl.RedisCacheMappingJobService;

/**
 * @author sahmad
 * @created 24/02/2021
 */
@TestConfiguration
@Testcontainers
public class TestConfig {

    @Bean(destroyMethod = "shutdown")
    @Profile("idmapping")
    RedissonClient redisson() {
        GenericContainer redisServer = RedisTestContainer.getInstance();
        Config config = new Config();
        config.useSingleServer()
                .setAddress(
                        "redis://"
                                + redisServer.getHost()
                                + ":"
                                + redisServer.getFirstMappedPort());
        return Redisson.create(config);
    }

    @Bean
    @Profile("idmapping")
    public IdMappingJobCacheService idMappingJobCacheService(RedissonClient redissonClient) {
        Map<String, CacheConfig> config = new HashMap<>();
        config.put("testMap", null);
        CacheManager cacheManager = new RedissonSpringCacheManager(redissonClient, config);
        Cache mappingCache = cacheManager.getCache("testMap");
        return new RedisCacheMappingJobService(mappingCache);
    }

    @Bean
    @Profile("idmapping")
    public ThreadPoolTaskExecutor jobTaskExecutor() {
        return new ThreadPoolTaskExecutor();
    }
}
