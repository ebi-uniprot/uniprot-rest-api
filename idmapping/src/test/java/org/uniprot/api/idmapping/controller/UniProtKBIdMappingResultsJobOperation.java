package org.uniprot.api.idmapping.controller;

import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.uniprot.api.idmapping.model.IdMappingJob;
import org.uniprot.api.idmapping.service.IdMappingJobCacheService;

/**
 * @author sahmad
 * @created 03/03/2021
 */
public class UniProtKBIdMappingResultsJobOperation extends AbstractJobOperation {
    public UniProtKBIdMappingResultsJobOperation(IdMappingJobCacheService cacheService) {
        super(cacheService);
    }

    @Override
    public IdMappingJob createAndPutJobInCache() throws Exception {
        String ids =
                IntStream.rangeClosed(1, 20)
                        .mapToObj(i -> String.format("Q%05d", i))
                        .collect(Collectors.joining(","));
        return createAndPutJobInCache("ACC", "ACC", ids);
    }
}
