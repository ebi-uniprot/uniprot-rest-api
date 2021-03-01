package org.uniprot.api.idmapping.service.job;

import java.util.Date;

import org.springframework.web.client.RestClientException;
import org.uniprot.api.idmapping.controller.response.JobStatus;
import org.uniprot.api.idmapping.model.IdMappingJob;
import org.uniprot.api.idmapping.model.IdMappingResult;
import org.uniprot.api.idmapping.service.IdMappingPIRService;

/**
 * @author sahmad
 * @created 23/02/2021
 */
public class JobTask implements Runnable {
    private final IdMappingJob job;
    private final IdMappingPIRService pirService;

    public JobTask(IdMappingJob job, IdMappingPIRService pirService) {
        this.job = job;
        this.pirService = pirService;
    }

    @Override
    public void run() {
        this.job.setJobStatus(JobStatus.RUNNING);
        this.job.setUpdated(new Date());
        try {
            IdMappingResult pirResponse = pirService.mapIds(this.job.getIdMappingRequest());
            this.job.setJobStatus(JobStatus.FINISHED);
            this.job.setIdMappingResult(pirResponse);
            this.job.setUpdated(new Date());
        } catch (RestClientException restException) {
            this.job.setErrorMessage(restException.getMessage());
            this.job.setJobStatus(JobStatus.ERROR);
            this.job.setUpdated(new Date());
        }
    }
}
