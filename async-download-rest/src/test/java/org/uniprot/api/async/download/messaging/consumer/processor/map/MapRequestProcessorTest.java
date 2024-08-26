package org.uniprot.api.async.download.messaging.consumer.processor.map;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;
import static org.uniprot.api.async.download.messaging.consumer.processor.map.MapRequestProcessor.RESULT_FILE;
import static org.uniprot.api.async.download.messaging.consumer.processor.map.MapRequestProcessor.STATUS;
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
import org.uniprot.api.async.download.messaging.consumer.processor.composite.map.UniProtKBToUniRefMapCompositeRequestProcessor;
import org.uniprot.api.async.download.model.request.map.UniProtKBMapDownloadRequest;
import org.uniprot.api.async.download.service.map.MapJobService;

@ExtendWith(MockitoExtension.class)
class MapRequestProcessorTest {
    public static final String ID = "someId";
    public static final String UNI_PROT_KB = "UniProtKB";
    public static final String UNI_REF = "UniRef";
    @Mock private UniProtKBMapDownloadRequest uniProtKBMapDownloadRequest;

    @Mock
    private UniProtKBToUniRefMapCompositeRequestProcessor
            uniProtKBToUniRefMapCompositeRequestProcessor;

    @Mock private MapJobService mapJobService;
    @InjectMocks private MapRequestProcessor mapRequestProcessor;

    @Test
    void process_mapFromUniProtKBToUniRef() {
        when(uniProtKBMapDownloadRequest.getDownloadJobId()).thenReturn(ID);
        when(uniProtKBMapDownloadRequest.getFrom()).thenReturn(UNI_PROT_KB);
        when(uniProtKBMapDownloadRequest.getTo()).thenReturn(UNI_REF);

        mapRequestProcessor.process(uniProtKBMapDownloadRequest);

        InOrderImpl inOrder =
                new InOrderImpl(
                        List.of(mapJobService, uniProtKBToUniRefMapCompositeRequestProcessor));
        inOrder.verify(mapJobService).update(ID, Map.of(STATUS, RUNNING));
        inOrder.verify(uniProtKBToUniRefMapCompositeRequestProcessor)
                .process(uniProtKBMapDownloadRequest);
        inOrder.verify(mapJobService).update(ID, Map.of(STATUS, FINISHED, RESULT_FILE, ID));
    }

    @Test
    void process_mapUnknown() {
        when(uniProtKBMapDownloadRequest.getDownloadJobId()).thenReturn(ID);
        when(uniProtKBMapDownloadRequest.getFrom()).thenReturn("unknown");

        assertThrows(
                IllegalArgumentException.class,
                () -> mapRequestProcessor.process(uniProtKBMapDownloadRequest));
    }
}
