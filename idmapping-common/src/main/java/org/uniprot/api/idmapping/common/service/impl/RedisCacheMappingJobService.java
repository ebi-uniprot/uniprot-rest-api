package org.uniprot.api.idmapping.common.service.impl;

import org.springframework.cache.Cache;
import org.uniprot.api.idmapping.common.model.IdMappingJob;
import org.uniprot.api.idmapping.common.service.IdMappingJobCacheService;
import org.uniprot.core.util.Utils;

/**
 * Created 23/02/2021
 *
 * @author Edd
 */
public class RedisCacheMappingJobService implements IdMappingJobCacheService {
    private final Cache cache;

    public RedisCacheMappingJobService(Cache cache) {
        this.cache = cache;
    }

    @Override
    public void put(String key, IdMappingJob value) {
        this.cache.put(key, value);
    }

    @Override
    public IdMappingJob get(String key) {
        return this.cache.get(key, IdMappingJob.class);
    }

    @Override
    public boolean exists(String key) {
        return Utils.notNull(this.cache.get(key, IdMappingJob.class));
    }

    @Override
    public void delete(String key) {
        this.cache.evictIfPresent(key);
    }
}
