package org.uniprot.api.idmapping.common;

import java.util.LinkedHashMap;
import java.util.Map;

import org.uniprot.api.idmapping.common.model.IdMappingJob;
import org.uniprot.api.idmapping.common.service.IdMappingJobCacheService;
import org.uniprot.api.rest.download.model.JobStatus;

/**
 * @author sahmad
 * @created 03/03/2021
 */
public class IdMappingResultsJobOperation extends AbstractJobOperation {
    public IdMappingResultsJobOperation(IdMappingJobCacheService cacheService) {
        super(cacheService);
    }

    @Override
    public IdMappingJob createAndPutJobInCache(int idsCount) throws Exception {
        return createAndPutJobInCache(idsCount, JobStatus.FINISHED);
    }

    @Override
    public IdMappingJob createAndPutJobInCache(int idsCount, JobStatus jobStatus) throws Exception {
        Map<String, String> ids = new LinkedHashMap<>();
        for (int i = 1; i <= idsCount; i++) {
            String fromId = String.format("Q%05d", i);
            String toId = String.format("I%05d", i);
            ids.put(fromId, toId);
        }
        return createAndPutJobInCache("ACC", "PIR", ids, jobStatus);
    }

    @Override
    public IdMappingJob createAndPutJobInCacheWithOneToManyMapping(
            int idsCount, JobStatus jobStatus) throws Exception {
        Map<String, String> ids = new LinkedHashMap<>();
        for (int i = 1; i <= idsCount; i++) {
            String fromId = String.format("Q%05d", i);
            // each id maps to 6 to ids
            String toId = (";" + String.format("I%05d", i)).repeat(6).replaceFirst(";", "");
            ids.put(fromId, toId);
        }
        return createAndPutJobInCache("ACC", "PIR", ids, jobStatus);
    }
}
