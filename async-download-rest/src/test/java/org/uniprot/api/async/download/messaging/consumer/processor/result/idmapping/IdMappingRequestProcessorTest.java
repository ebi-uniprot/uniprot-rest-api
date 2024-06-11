package org.uniprot.api.async.download.messaging.consumer.processor.result.idmapping;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.internal.InOrderImpl;
import org.mockito.junit.jupiter.MockitoExtension;
import org.uniprot.api.async.download.messaging.consumer.processor.result.IdMappingResultRequestProcessor;
import org.uniprot.api.async.download.model.request.idmapping.IdMappingDownloadRequest;
import org.uniprot.api.async.download.service.idmapping.IdMappingJobService;
import org.uniprot.api.idmapping.common.model.IdMappingJob;
import org.uniprot.api.idmapping.common.model.IdMappingResult;
import org.uniprot.api.idmapping.common.request.IdMappingJobRequest;
import org.uniprot.api.idmapping.common.response.model.IdMappingStringPair;
import org.uniprot.api.idmapping.common.service.IdMappingJobCacheService;

import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.when;
import static org.uniprot.api.async.download.messaging.consumer.processor.result.idmapping.IdMappingRequestProcessor.*;
import static org.uniprot.api.rest.download.model.JobStatus.FINISHED;
import static org.uniprot.api.rest.download.model.JobStatus.RUNNING;

@ExtendWith(MockitoExtension.class)
class IdMappingRequestProcessorTest {
    private static final String JOB_ID = "someJobId";
    private static final String TO = "to";
    private static final String ID = "id";
    private static final int SIZE = 5;
    @Mock
    private IdMappingResultRequestProcessorFactory idMappingResultRequestProcessorFactory;
    @Mock
    private IdMappingJobCacheService idMappingJobCacheService;
    @Mock
    private IdMappingJobService idMappingJobService;
    @InjectMocks
    private IdMappingRequestProcessor idMappingRequestProcessor;
    @Mock
    private IdMappingJob idMappingJob;
    @Mock
    private IdMappingDownloadRequest request;
    @Mock
    private IdMappingJobRequest idMappingRequest;
    @Mock
    private IdMappingResultRequestProcessor requestProcessor;
    @Mock
    private IdMappingResult idMappingResult;
    @Mock
    private List<IdMappingStringPair> mappedIds;

    @Test
    void process() {
        when(idMappingJobCacheService.get(JOB_ID)).thenReturn(idMappingJob);
        when(request.getJobId()).thenReturn(JOB_ID);
        when(request.getId()).thenReturn(ID);
        when(idMappingJob.getIdMappingRequest()).thenReturn(idMappingRequest);
        when(idMappingJob.getIdMappingResult()).thenReturn(idMappingResult);
        when(idMappingResult.getMappedIds()).thenReturn(mappedIds);
        when(idMappingRequest.getTo()).thenReturn(TO);
        when(mappedIds.size()).thenReturn(SIZE);
        when(idMappingResultRequestProcessorFactory.getRequestProcessor(TO))
                .thenReturn(requestProcessor);

        idMappingRequestProcessor.process(request);

        InOrderImpl inOrder = new InOrderImpl(List.of(idMappingJobService, requestProcessor));
        inOrder.verify(idMappingJobService).update(ID, Map.of(STATUS, RUNNING,TOTAL_ENTRIES, (long)SIZE));
        inOrder.verify(requestProcessor).process(request);
        inOrder.verify(idMappingJobService).update(request.getId(), Map.of(STATUS, FINISHED, RESULT_FILE, request.getId()));
    }
}
