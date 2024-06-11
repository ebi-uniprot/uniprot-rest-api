package org.uniprot.api.async.download.messaging.consumer.processor.result.idmapping;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.uniprot.api.async.download.messaging.consumer.processor.result.IdMappingResultRequestProcessor;
import org.uniprot.api.async.download.model.request.idmapping.IdMappingDownloadRequest;
import org.uniprot.api.idmapping.common.model.IdMappingJob;
import org.uniprot.api.idmapping.common.request.IdMappingJobRequest;
import org.uniprot.api.idmapping.common.service.IdMappingJobCacheService;

@ExtendWith(MockitoExtension.class)
class IdMappingRequestProcessorTest {
    public static final String JOB_ID = "someJobId";
    public static final String TO = "to";
    @Mock private IdMappingResultRequestProcessorFactory idMappingResultRequestProcessorFactory;
    @Mock private IdMappingJobCacheService idMappingJobCacheService;
    @InjectMocks private IdMappingRequestProcessor idMappingRequestProcessor;
    @Mock private IdMappingJob idMappingJob;
    @Mock private IdMappingDownloadRequest request;
    @Mock private IdMappingJobRequest idMappingRequest;
    @Mock private IdMappingResultRequestProcessor requestProcessor;

    @Test
    void process() {
        when(idMappingJobCacheService.get(JOB_ID)).thenReturn(idMappingJob);
        when(request.getJobId()).thenReturn(JOB_ID);
        when(idMappingJob.getIdMappingRequest()).thenReturn(idMappingRequest);
        when(idMappingRequest.getTo()).thenReturn(TO);
        when(idMappingResultRequestProcessorFactory.getRequestProcessor(TO))
                .thenReturn(requestProcessor);

        idMappingRequestProcessor.process(request);

        verify(requestProcessor).process(request);
    }
}
