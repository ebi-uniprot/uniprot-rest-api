package org.uniprot.api.idmapping.service;

import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.uniprot.api.idmapping.service.cache.IdMappingJobCacheService;
import org.uniprot.api.idmapping.service.cache.impl.EhCacheMappingJobService;

@Configuration
@EnableCaching
public class CacheConfig {
    private static final String PIR_ID_MAPPING_CACHE = "pirIDMappingCache";

    @Bean
    @Profile("live")
    public CacheManager cacheManager() {
        return new ConcurrentMapCacheManager(PIR_ID_MAPPING_CACHE);
    }

    @Bean
    @Profile("live")
    public IdMappingJobCacheService cacheService(CacheManager cacheManager) {
        Cache mappingCache = cacheManager.getCache(PIR_ID_MAPPING_CACHE);
        return new EhCacheMappingJobService(mappingCache);
    }
}
