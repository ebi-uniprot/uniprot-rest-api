package org.uniprot.api.async.download.messaging.consumer.processor.result.idmapping;

import static org.uniprot.api.async.download.messaging.repository.JobFields.*;
import static org.uniprot.api.rest.download.model.JobStatus.*;

import java.util.Map;

import org.springframework.stereotype.Component;
import org.uniprot.api.async.download.messaging.consumer.processor.RequestProcessor;
import org.uniprot.api.async.download.model.request.idmapping.IdMappingDownloadRequest;
import org.uniprot.api.async.download.service.idmapping.IdMappingJobService;
import org.uniprot.api.idmapping.common.model.IdMappingJob;
import org.uniprot.api.idmapping.common.service.IdMappingJobCacheService;

@Component
public class IdMappingRequestProcessor implements RequestProcessor<IdMappingDownloadRequest> {
    private final IdMappingResultRequestProcessorFactory idMappingResultRequestProcessorFactory;
    private final IdMappingJobCacheService idMappingJobCacheService;
    private final IdMappingJobService jobService;

    public IdMappingRequestProcessor(
            IdMappingResultRequestProcessorFactory idMappingResultRequestProcessorFactory,
            IdMappingJobCacheService idMappingJobCacheService,
            IdMappingJobService jobService) {
        this.idMappingResultRequestProcessorFactory = idMappingResultRequestProcessorFactory;
        this.idMappingJobCacheService = idMappingJobCacheService;
        this.jobService = jobService;
    }

    @Override
    public void process(IdMappingDownloadRequest request) {
        IdMappingJob idMappingJob = idMappingJobCacheService.get(request.getIdMappingJobId());
        jobService.update(
                request.getDownloadJobId(),
                Map.of(
                        STATUS.getName(),
                        RUNNING,
                        TOTAL_ENTRIES.getName(),
                        (long) (idMappingJob.getIdMappingResult().getMappedIds().size())));
        idMappingResultRequestProcessorFactory
                .getRequestProcessor(idMappingJob.getIdMappingRequest().getTo())
                .process(request);
        jobService.update(
                request.getDownloadJobId(),
                Map.of(
                        STATUS.getName(),
                        FINISHED,
                        RESULT_FILE.getName(),
                        request.getDownloadJobId()));
    }
}
