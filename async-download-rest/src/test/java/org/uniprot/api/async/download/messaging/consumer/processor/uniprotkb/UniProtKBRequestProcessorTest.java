package org.uniprot.api.async.download.messaging.consumer.processor.uniprotkb;

import static org.mockito.Mockito.when;
import static org.uniprot.api.async.download.messaging.repository.JobFields.*;
import static org.uniprot.api.rest.download.model.JobStatus.FINISHED;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.internal.InOrderImpl;
import org.mockito.junit.jupiter.MockitoExtension;
import org.uniprot.api.async.download.messaging.consumer.processor.uniprotkb.composite.UniProtKBCompositeRequestProcessor;
import org.uniprot.api.async.download.messaging.consumer.processor.uniprotkb.id.UniProtKBSolrIdHD5RequestProcessor;
import org.uniprot.api.async.download.model.request.uniprotkb.UniProtKBDownloadRequest;
import org.uniprot.api.async.download.service.uniprotkb.UniProtKBJobService;
import org.uniprot.api.rest.download.model.JobStatus;

@ExtendWith(MockitoExtension.class)
class UniProtKBRequestProcessorTest {
    private static final String JSON = "application/json";
    private static final String HD5 = "application/x-hdf5";
    public static final String ID = "someId";
    @Mock private UniProtKBDownloadRequest uniProtKBDownloadRequest;
    @Mock private UniProtKBCompositeRequestProcessor uniProtKBCompositeRequestProcessor;
    @Mock private UniProtKBSolrIdHD5RequestProcessor uniProtKBSolrIdHD5RequestProcessor;
    @Mock private UniProtKBJobService uniProtKBJobService;
    @InjectMocks private UniProtKBRequestProcessor uniProtKBRequestProcessor;

    @BeforeEach
    void setUp() {
        when(uniProtKBDownloadRequest.getDownloadJobId()).thenReturn(ID);
    }

    @Test
    void process() {
        when(uniProtKBDownloadRequest.getFormat()).thenReturn(JSON);

        uniProtKBRequestProcessor.process(uniProtKBDownloadRequest);

        InOrderImpl inOrder =
                new InOrderImpl(List.of(uniProtKBJobService, uniProtKBCompositeRequestProcessor));
        inOrder.verify(uniProtKBJobService).update(ID, Map.of(STATUS.getName(), JobStatus.RUNNING));
        inOrder.verify(uniProtKBCompositeRequestProcessor).process(uniProtKBDownloadRequest);
        inOrder.verify(uniProtKBJobService)
                .update(ID, Map.of(STATUS.getName(), FINISHED, RESULT_FILE.getName(), ID));
    }

    @Test
    void process_hd5Format() {
        when(uniProtKBDownloadRequest.getFormat()).thenReturn(HD5);

        uniProtKBRequestProcessor.process(uniProtKBDownloadRequest);

        InOrderImpl inOrder =
                new InOrderImpl(List.of(uniProtKBJobService, uniProtKBSolrIdHD5RequestProcessor));
        inOrder.verify(uniProtKBJobService).update(ID, Map.of(STATUS.getName(), JobStatus.RUNNING));
        inOrder.verify(uniProtKBSolrIdHD5RequestProcessor).process(uniProtKBDownloadRequest);
    }
}
