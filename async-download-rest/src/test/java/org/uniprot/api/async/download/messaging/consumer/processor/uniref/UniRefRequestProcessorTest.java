package org.uniprot.api.async.download.messaging.consumer.processor.uniref;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.internal.InOrderImpl;
import org.mockito.junit.jupiter.MockitoExtension;
import org.uniprot.api.async.download.messaging.consumer.processor.composite.uniref.UniRefCompositeRequestProcessor;
import org.uniprot.api.async.download.model.request.uniref.UniRefDownloadRequest;
import org.uniprot.api.async.download.service.uniref.UniRefJobService;

import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.when;
import static org.uniprot.api.async.download.messaging.consumer.processor.uniref.UniRefRequestProcessor.STATUS;
import static org.uniprot.api.async.download.messaging.consumer.processor.uniref.UniRefRequestProcessor.RESULT_FILE;
import static org.uniprot.api.rest.download.model.JobStatus.FINISHED;
import static org.uniprot.api.rest.download.model.JobStatus.RUNNING;

@ExtendWith(MockitoExtension.class)
class UniRefRequestProcessorTest {
    public static final String ID = "someId";
    @Mock
    private UniRefDownloadRequest unirefDownloadRequest;
    @Mock
    private UniRefCompositeRequestProcessor unirefCompositeRequestProcessor;
    @Mock
    private UniRefJobService uniRefJobService;
    @InjectMocks
    private UniRefRequestProcessor unirefRequestProcessor;

    @Test
    void process() {
        when(unirefDownloadRequest.getId()).thenReturn(ID);

        unirefRequestProcessor.process(unirefDownloadRequest);

        InOrderImpl inOrder = new InOrderImpl(List.of(uniRefJobService, unirefCompositeRequestProcessor));
        inOrder.verify(uniRefJobService).update(ID, Map.of(STATUS, RUNNING));
        inOrder.verify(unirefCompositeRequestProcessor).process(unirefDownloadRequest);
        inOrder.verify(uniRefJobService).update(ID, Map.of(STATUS, FINISHED, RESULT_FILE, ID));
    }
}
