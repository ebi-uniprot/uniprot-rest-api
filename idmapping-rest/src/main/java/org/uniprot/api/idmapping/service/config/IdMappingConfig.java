package org.uniprot.api.idmapping.service.config;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.redisson.spring.cache.CacheConfig;
import org.redisson.spring.cache.RedissonSpringCacheManager;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.web.client.RestTemplate;
import org.uniprot.api.idmapping.service.IdMappingJobCacheService;
import org.uniprot.api.idmapping.service.impl.EhCacheMappingJobService;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Created 15/02/2021
 *
 * @author Edd
 */
@Configuration
@EnableCaching
@ConfigurationProperties(prefix = "id.mapping.job")
public class IdMappingConfig {
    private static final String PIR_ID_MAPPING_CACHE = "pirIDMappingCache";

    @Bean
    public RestTemplate idMappingRestTemplate(RestTemplateBuilder restTemplateBuilder) {
        return restTemplateBuilder.build();
    }

//    @Bean("in-memory")
//    public IdMappingJobCacheService cacheService(CacheManager cacheManager) {
//        Cache mappingCache = cacheManager.getCache(PIR_ID_MAPPING_CACHE);
//        return new EhCacheMappingJobService(mappingCache);
//    }

    @Bean(destroyMethod="shutdown")
    RedissonClient redisson() throws IOException {
        Config config = new Config();
        config.useClusterServers()
                .addNodeAddress("redis://127.0.0.1:30001");
        return Redisson.create(config);
    }

    @Bean
    @Profile("live")
    public IdMappingJobCacheService idMappingJobCacheService(RedissonClient redissonClient) {
        Map<String, CacheConfig> config = new HashMap<>();
        // create "testMap" cache with ttl = 24 minutes and maxIdleTime = 12 minutes
        config.put("testMap", new CacheConfig(24*60*1000, 12*60*1000));
        CacheManager cacheManager = new RedissonSpringCacheManager(redissonClient, config);
        Cache mappingCache = cacheManager.getCache("testMap");
        return new EhCacheMappingJobService(mappingCache);
    }
}
