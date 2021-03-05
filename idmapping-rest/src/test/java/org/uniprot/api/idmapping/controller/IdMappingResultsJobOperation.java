package org.uniprot.api.idmapping.controller;

import java.util.LinkedHashMap;
import java.util.Map;

import org.uniprot.api.idmapping.controller.response.JobStatus;
import org.uniprot.api.idmapping.model.IdMappingJob;
import org.uniprot.api.idmapping.service.IdMappingJobCacheService;

/**
 * @author sahmad
 * @created 03/03/2021
 */
public class IdMappingResultsJobOperation extends AbstractJobOperation {
    public IdMappingResultsJobOperation(IdMappingJobCacheService cacheService) {
        super(cacheService);
    }

    @Override
    public IdMappingJob createAndPutJobInCache() throws Exception {
        return createAndPutJobInCache(JobStatus.FINISHED);
    }

    @Override
    public IdMappingJob createAndPutJobInCache(JobStatus jobStatus) throws Exception {
        Map<String, String> ids = new LinkedHashMap<>();
        for (int i = 1; i <= 20; i++) {
            String fromId = String.format("Q%05d", i);
            String toId = String.format("I%05d", i);
            ids.put(fromId, toId);
        }
        return createAndPutJobInCache("ACC", "PIR", ids, jobStatus);
    }
}
