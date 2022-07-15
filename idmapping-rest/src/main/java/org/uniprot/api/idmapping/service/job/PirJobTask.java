package org.uniprot.api.idmapping.service.job;

import org.springframework.web.client.RestClientException;
import org.uniprot.api.common.repository.search.ProblemPair;
import org.uniprot.api.idmapping.model.IdMappingJob;
import org.uniprot.api.idmapping.model.IdMappingResult;
import org.uniprot.api.idmapping.service.IdMappingPIRService;

public class PirJobTask extends JobTask {
    private static final int REST_EXCEPTION_CODE = 50;
    private final IdMappingPIRService pirService;

    public PirJobTask(IdMappingJob job, IdMappingPIRService pirService) {
        super(job);
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
        }
    }
}
