package org.uniprot.api.idmapping.service;

import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.uniprot.api.idmapping.service.cache.impl.EhCacheMappingJobService;

@Configuration
@EnableCaching
public class CacheConfig {
    private static final String PIR_ID_MAPPING_CACHE = "pirIDMappingCache";

    @Bean
    public CacheManager cacheManager() {
        return new ConcurrentMapCacheManager(PIR_ID_MAPPING_CACHE);
    }

    @Bean
    public EhCacheMappingJobService ehCacheMappingJobService(CacheManager cacheManager) {
        Cache mappingCache = cacheManager.getCache(PIR_ID_MAPPING_CACHE);
        return new EhCacheMappingJobService(mappingCache);
    }
}
