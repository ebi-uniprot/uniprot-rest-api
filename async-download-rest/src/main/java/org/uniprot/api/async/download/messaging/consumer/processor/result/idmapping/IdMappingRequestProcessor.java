package org.uniprot.api.async.download.messaging.consumer.processor.result.idmapping;

import org.springframework.stereotype.Component;
import org.uniprot.api.async.download.messaging.consumer.processor.RequestProcessor;
import org.uniprot.api.async.download.model.request.idmapping.IdMappingDownloadRequest;
import org.uniprot.api.async.download.service.idmapping.IdMappingJobService;
import org.uniprot.api.idmapping.common.model.IdMappingJob;
import org.uniprot.api.idmapping.common.service.IdMappingJobCacheService;

import java.util.Map;

import static org.uniprot.api.rest.download.model.JobStatus.*;

@Component
public class IdMappingRequestProcessor implements RequestProcessor<IdMappingDownloadRequest> {
    protected static final String TOTAL_ENTRIES = "totalEntries";
    protected static final String RESULT_FILE = "resultFile";
    protected static final String STATUS = "status";
    private final IdMappingResultRequestProcessorFactory idMappingResultRequestProcessorFactory;
    private final IdMappingJobCacheService idMappingJobCacheService;
    private final IdMappingJobService jobService;

    public IdMappingRequestProcessor(
            IdMappingResultRequestProcessorFactory idMappingResultRequestProcessorFactory,
            IdMappingJobCacheService idMappingJobCacheService, IdMappingJobService jobService) {
        this.idMappingResultRequestProcessorFactory = idMappingResultRequestProcessorFactory;
        this.idMappingJobCacheService = idMappingJobCacheService;
        this.jobService = jobService;
    }

    @Override
    public void process(IdMappingDownloadRequest request) {
        IdMappingJob idMappingJob = idMappingJobCacheService.get(request.getJobId());
        jobService.update(request.getId(), Map.of(STATUS, RUNNING,TOTAL_ENTRIES, (long)(idMappingJob.getIdMappingResult().getMappedIds().size())));
        idMappingResultRequestProcessorFactory
                .getRequestProcessor(idMappingJob.getIdMappingRequest().getTo())
                .process(request);
        jobService.update(request.getId(), Map.of(STATUS, FINISHED, RESULT_FILE, request.getId()));
    }
}
