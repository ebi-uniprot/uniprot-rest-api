package org.uniprot.api.async.download.messaging.consumer.processor.result.idmapping;

import org.springframework.stereotype.Component;
import org.uniprot.api.async.download.messaging.consumer.processor.RequestProcessor;
import org.uniprot.api.async.download.model.request.idmapping.IdMappingDownloadRequest;
import org.uniprot.api.idmapping.common.model.IdMappingJob;
import org.uniprot.api.idmapping.common.service.IdMappingJobCacheService;

@Component
public class IdMappingRequestProcessor implements RequestProcessor<IdMappingDownloadRequest> {
    private final IdMappingResultRequestProcessorFactory idMappingResultRequestProcessorFactory;
    private final IdMappingJobCacheService idMappingJobCacheService;

    public IdMappingRequestProcessor(IdMappingResultRequestProcessorFactory idMappingResultRequestProcessorFactory, IdMappingJobCacheService idMappingJobCacheService) {
        this.idMappingResultRequestProcessorFactory = idMappingResultRequestProcessorFactory;
        this.idMappingJobCacheService = idMappingJobCacheService;
    }

    @Override
    public void process(IdMappingDownloadRequest request) {
        IdMappingJob idMappingJob = idMappingJobCacheService.get(request.getJobId());
        idMappingResultRequestProcessorFactory.getRequestProcessor(idMappingJob.getIdMappingRequest().getTo()).process(request);
    }
}
