package org.uniprot.api.idmapping.controller.utils;

import static org.uniprot.api.idmapping.controller.utils.IdMappingUniParcITUtils.*;
import static org.uniprot.api.idmapping.controller.utils.IdMappingUniProtKBITUtils.*;

import java.util.LinkedHashMap;
import java.util.Map;

import org.uniprot.api.idmapping.controller.response.JobStatus;
import org.uniprot.api.idmapping.model.IdMappingJob;
import org.uniprot.api.idmapping.service.IdMappingJobCacheService;

/**
 * @author sahmad
 * @created 03/03/2021
 */
public class UniParcIdMappingResultsJobOperation extends AbstractJobOperation {

    public UniParcIdMappingResultsJobOperation(IdMappingJobCacheService cacheService) {
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
            String toId = String.format(UPI_PREF + "%02d", i);
            ids.put(fromId, toId);
        }
        return createAndPutJobInCache(UNIPROTKB_AC_ID_STR, "UniParc", ids, jobStatus);
    }
}
