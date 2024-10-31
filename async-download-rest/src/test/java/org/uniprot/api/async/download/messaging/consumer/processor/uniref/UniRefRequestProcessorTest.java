package org.uniprot.api.async.download.messaging.consumer.processor.uniref;

import static org.mockito.Mockito.when;
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
import org.uniprot.api.async.download.messaging.consumer.processor.uniref.composite.UniRefCompositeRequestProcessor;
import org.uniprot.api.async.download.messaging.repository.JobFields;
import org.uniprot.api.async.download.model.request.uniref.UniRefDownloadRequest;
import org.uniprot.api.async.download.service.uniref.UniRefJobService;

@ExtendWith(MockitoExtension.class)
class UniRefRequestProcessorTest {
    public static final String ID = "someId";
    @Mock private UniRefDownloadRequest unirefDownloadRequest;
    @Mock private UniRefCompositeRequestProcessor unirefCompositeRequestProcessor;
    @Mock private UniRefJobService uniRefJobService;
    @InjectMocks private UniRefRequestProcessor unirefRequestProcessor;

    @Test
    void process() {
        when(unirefDownloadRequest.getDownloadJobId()).thenReturn(ID);

        unirefRequestProcessor.process(unirefDownloadRequest);

        InOrderImpl inOrder =
                new InOrderImpl(List.of(uniRefJobService, unirefCompositeRequestProcessor));
        inOrder.verify(uniRefJobService).update(ID, Map.of(JobFields.STATUS.getName(), RUNNING));
        inOrder.verify(unirefCompositeRequestProcessor).process(unirefDownloadRequest);
        inOrder.verify(uniRefJobService)
                .update(
                        ID,
                        Map.of(
                                JobFields.STATUS.getName(),
                                FINISHED,
                                JobFields.RESULT_FILE.getName(),
                                ID));
    }
}
