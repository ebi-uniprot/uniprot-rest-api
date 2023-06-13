package org.uniprot.api.idmapping.service;

import java.util.HashMap;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

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
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import org.uniprot.api.idmapping.service.impl.RedisCacheMappingJobService;

/**
 * @author sahmad
 * @created 24/02/2021
 */
@TestConfiguration
@Testcontainers
public class TestConfig {
    private static final String REDIS_IMAGE_VERSION = "redis:5.0.3-alpine";

    @Container
    private final GenericContainer redisServer =
            new GenericContainer(DockerImageName.parse(REDIS_IMAGE_VERSION))
                    .withExposedPorts(6379)
                    .withReuse(true);

    @PostConstruct
    public void postConstruct() {
        redisServer.start();
    }

    @PreDestroy
    public void preDestroy() {
        this.redisServer.stop();
    }

    @Bean(destroyMethod = "shutdown")
    @Profile("idmapping")
    RedissonClient redisson() {
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
    @Profile("idmapping | asyncDownload")
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
