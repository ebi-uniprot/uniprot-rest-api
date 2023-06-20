package org.uniprot.api.idmapping.controller.utils;

import static org.uniprot.api.idmapping.controller.utils.IdMappingUniProtKBITUtils.UNIPROTKB_AC_ID_STR;
import static org.uniprot.api.idmapping.controller.utils.IdMappingUniProtKBITUtils.UNIPROTKB_STR;

import java.util.LinkedHashMap;
import java.util.Map;

import org.uniprot.api.idmapping.model.IdMappingJob;
import org.uniprot.api.idmapping.service.IdMappingJobCacheService;
import org.uniprot.api.rest.download.model.JobStatus;

/**
 * @author sahmad
 * @created 03/03/2021
 */
public class UniProtKBIdMappingResultsJobOperation extends AbstractJobOperation {
    public UniProtKBIdMappingResultsJobOperation(IdMappingJobCacheService cacheService) {
        super(cacheService);
    }

    @Override
    public IdMappingJob createAndPutJobInCacheForAllFields() throws Exception {
        Map<String, String> ids = new LinkedHashMap<>();
        ids.put("I8FBX0", "I8FBX0");
        ids.putAll(getActiveIdsInMap(DEFAULT_IDS_COUNT - 1));
        return createAndPutJobInCache(UNIPROTKB_AC_ID_STR, UNIPROTKB_STR, ids, JobStatus.FINISHED);
    }

    @Override
    public IdMappingJob createAndPutJobInCache(int idsCount) throws Exception {
        return createAndPutJobInCache(idsCount, JobStatus.FINISHED);
    }

    @Override
    public IdMappingJob createAndPutJobInCache(int idsCount, JobStatus jobStatus) throws Exception {
        Map<String, String> ids = getActiveIdsInMap(idsCount);
        return createAndPutJobInCache(UNIPROTKB_AC_ID_STR, UNIPROTKB_STR, ids, jobStatus);
    }

    @Override
    public IdMappingJob createAndPutJobInCacheWithOneToManyMapping(
            int idsCount, JobStatus jobStatus) throws Exception {
        Map<String, String> ids = new LinkedHashMap<>();
        for (int i = 1; i <= idsCount; i++) {
            String fromId = String.format("Q%05d", i);
            // each id maps to 6 to ids
            String toId = (";" + String.format("Q%05d", i)).repeat(6).replaceFirst(";", "");
            ids.put(fromId, toId);
        }
        return createAndPutJobInCache(UNIPROTKB_AC_ID_STR, UNIPROTKB_STR, ids, jobStatus);
    }

    private Map<String, String> getActiveIdsInMap(int idsCount) {
        Map<String, String> ids = new LinkedHashMap<>();
        for (int i = 1; i <= idsCount; i++) {
            String id = String.format("Q%05d", i);
            ids.put(id, id);
        }
        return ids;
    }
}
