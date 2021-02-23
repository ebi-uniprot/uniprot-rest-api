package org.uniprot.api.idmapping.service.cache;

import org.uniprot.api.idmapping.model.IdMappingJob;

/**
 * @author sahmad
 * @created 23/02/2021
 */
public interface IdMappingJobService {
    void put(String key, IdMappingJob value);

    IdMappingJob get(String key);

    boolean exists(String key);

    void delete(String key);
}
