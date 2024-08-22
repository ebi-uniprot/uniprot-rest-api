package org.uniprot.api.async.download.messaging.consumer.processor.uniparc;

import static org.mockito.Mockito.when;
import static org.uniprot.api.async.download.messaging.consumer.processor.uniparc.UniParcRequestProcessor.RESULT_FILE;
import static org.uniprot.api.async.download.messaging.consumer.processor.uniparc.UniParcRequestProcessor.STATUS;
import static org.uniprot.api.rest.download.model.JobStatus.FINISHED;
import static org.uniprot.api.rest.download.model.JobStatus.RUNNING;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.internal.InOrderImpl;
import org.mockito.junit.jupiter.MockitoExtension;
import org.uniprot.api.async.download.messaging.consumer.processor.composite.uniparc.UniParcCompositeRequestProcessor;
import org.uniprot.api.async.download.model.request.uniparc.UniParcDownloadRequest;
import org.uniprot.api.async.download.service.uniparc.UniParcJobService;

@ExtendWith(MockitoExtension.class)
class UniParcRequestProcessorTest {
    public static final String ID = "someId";
    @Mock private UniParcDownloadRequest uniparcDownloadRequest;
    @Mock private UniParcCompositeRequestProcessor uniparcCompositeRequestProcessor;
    @Mock private UniParcJobService uniParcJobService;
    @InjectMocks private UniParcRequestProcessor uniparcRequestProcessor;

    @Test
    void process() {
        when(uniparcDownloadRequest.getDownloadJobId()).thenReturn(ID);

        uniparcRequestProcessor.process(uniparcDownloadRequest);

        InOrderImpl inOrder =
                new InOrderImpl(List.of(uniParcJobService, uniparcCompositeRequestProcessor));
        inOrder.verify(uniParcJobService).update(ID, Map.of(STATUS, RUNNING));
        inOrder.verify(uniparcCompositeRequestProcessor).process(uniparcDownloadRequest);
        inOrder.verify(uniParcJobService).update(ID, Map.of(STATUS, FINISHED, RESULT_FILE, ID));
    }
}
