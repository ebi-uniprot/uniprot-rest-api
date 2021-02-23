package org.uniprot.api.idmapping.service.cache.impl;

import org.springframework.cache.Cache;
import org.uniprot.api.idmapping.model.IdMappingJob;
import org.uniprot.api.idmapping.service.cache.IdMappingJobService;

import java.util.Objects;

/**
 * Created 23/02/2021
 *
 * @author Edd
 */
public class EhCacheMappingJobService implements IdMappingJobService {
    private final Cache cache;

    public EhCacheMappingJobService(Cache cache) {
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
        return Objects.isNull(this.cache.get(key, IdMappingJob.class));
    }

    @Override
    public void delete(String key) {
        this.cache.evictIfPresent(key);
    }
}
