package org.uniprot.api.async.download.common;

import java.util.HashMap;
import java.util.Map;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.redisson.spring.cache.CacheConfig;
import org.redisson.spring.cache.RedissonSpringCacheManager;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.uniprot.api.idmapping.common.model.IdMappingResult;
import org.uniprot.api.idmapping.common.request.IdMappingJobRequest;
import org.uniprot.api.idmapping.common.service.IdMappingJobCacheService;
import org.uniprot.api.idmapping.common.service.IdMappingPIRService;
import org.uniprot.api.idmapping.common.service.impl.RedisCacheMappingJobService;
import org.uniprot.core.uniparc.UniParcEntryLight;
import org.uniprot.core.uniprotkb.UniProtKBEntry;
import org.uniprot.core.uniref.UniRefEntryLight;
import org.uniprot.store.datastore.UniProtStoreClient;
import org.uniprot.store.datastore.voldemort.light.uniparc.VoldemortInMemoryUniParcEntryLightStore;
import org.uniprot.store.datastore.voldemort.light.uniref.VoldemortInMemoryUniRefEntryLightStore;
import org.uniprot.store.datastore.voldemort.uniprot.VoldemortInMemoryUniprotEntryStore;

@TestConfiguration
public class RedisConfigTest {

    @Bean(destroyMethod = "shutdown")
    RedissonClient redissonClient() {
        Config config = new Config();
        config.useSingleServer()
                .setAddress(
                        "redis://"
                                + System.getProperty("uniprot.redis.host")
                                + ":"
                                + System.getProperty("uniprot.redis.port"));
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

    @Bean
    @Profile("offline")
    public IdMappingPIRService pirService(
            @Value("${search.request.converter.defaultRestPageSize:#{null}}")
                    Integer defaultPageSize) {
        return new IdMappingPIRService(defaultPageSize) {
            @Override
            public IdMappingResult mapIds(IdMappingJobRequest request, String jobId) {
                return IdMappingResult.builder().build();
            }
        };
    }

    @Bean("uniProtStoreClient")
    @Profile("offline")
    public UniProtStoreClient<UniProtKBEntry> uniProtStoreClient() {
        return new UniProtStoreClient<>(
                VoldemortInMemoryUniprotEntryStore.getInstance("avro-uniprot"));
    }

    @Bean("uniRefLightStoreClient")
    @Profile("offline")
    public UniProtStoreClient<UniRefEntryLight> uniRefLightStoreClient() {
        return new UniProtStoreClient<>(
                VoldemortInMemoryUniRefEntryLightStore.getInstance("avro-uniprot"));
    }

    @Bean("uniParcLightStoreClient")
    @Profile("offline")
    public UniProtStoreClient<UniParcEntryLight> uniParcLightStoreClient() {
        return new UniProtStoreClient<>(
                VoldemortInMemoryUniParcEntryLightStore.getInstance("uniparc-light"));
    }
}
