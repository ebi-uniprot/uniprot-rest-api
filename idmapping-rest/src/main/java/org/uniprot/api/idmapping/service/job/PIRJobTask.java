package org.uniprot.api.idmapping.service.job;

import lombok.extern.slf4j.Slf4j;

import org.springframework.web.client.RestClientException;
import org.uniprot.api.common.repository.search.ProblemPair;
import org.uniprot.api.idmapping.model.IdMappingJob;
import org.uniprot.api.idmapping.model.IdMappingResult;
import org.uniprot.api.idmapping.service.IdMappingJobCacheService;
import org.uniprot.api.idmapping.service.IdMappingPIRService;

@Slf4j
public class PIRJobTask extends JobTask {
    private static final int REST_EXCEPTION_CODE = 50;
    private final IdMappingPIRService pirService;

    public PIRJobTask(
            IdMappingJob job,
            IdMappingJobCacheService cacheService,
            IdMappingPIRService pirService) {
        super(job, cacheService);
        this.pirService = pirService;
    }

    @Override
    protected IdMappingResult processTask(IdMappingJob job) {
        try {
            return pirService.mapIds(job.getIdMappingRequest(), job.getJobId());
        } catch (RestClientException restException) {
            return IdMappingResult.builder()
                    .error(new ProblemPair(REST_EXCEPTION_CODE, restException.getMessage()))
                    .build();
        } catch (Exception ex) {
            log.error("Error while processing PIR response {}", ex.getCause());
            return IdMappingResult.builder()
                    .error(new ProblemPair(REST_EXCEPTION_CODE, "Internal server error."))
                    .build();
        }
    }
}
