package org.uniprot.api.async.download.messaging.consumer.processor.mapto;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;
import static org.uniprot.api.async.download.messaging.consumer.processor.mapto.MapToRequestProcessor.RESULT_FILE;
import static org.uniprot.api.async.download.messaging.consumer.processor.mapto.MapToRequestProcessor.STATUS;
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
import org.uniprot.api.async.download.messaging.consumer.processor.composite.map.UniProtKBToUniRefCompositeRequestProcessor;
import org.uniprot.api.async.download.model.request.mapto.UniProtKBToUniRefDownloadRequest;
import org.uniprot.api.async.download.service.mapto.MapToJobService;

@ExtendWith(MockitoExtension.class)
class MapToRequestProcessorTest {
    public static final String ID = "someId";
    public static final String UNI_PROT_KB = "UniProtKB";
    public static final String UNI_REF = "UniRef";
    @Mock private UniProtKBToUniRefDownloadRequest uniProtKBToUniRefMapDownloadRequest;

    @Mock
    private UniProtKBToUniRefCompositeRequestProcessor uniProtKBToUniRefCompositeRequestProcessor;

    @Mock private MapToJobService mapToJobService;
    @InjectMocks private MapToRequestProcessor mapToRequestProcessor;

    @Test
    void process_mapFromUniProtKBToUniRef() {
        when(uniProtKBToUniRefMapDownloadRequest.getDownloadJobId()).thenReturn(ID);
        when(uniProtKBToUniRefMapDownloadRequest.getFrom()).thenReturn(UNI_PROT_KB);
        when(uniProtKBToUniRefMapDownloadRequest.getTo()).thenReturn(UNI_REF);

        mapToRequestProcessor.process(uniProtKBToUniRefMapDownloadRequest);

        InOrderImpl inOrder =
                new InOrderImpl(
                        List.of(mapToJobService, uniProtKBToUniRefCompositeRequestProcessor));
        inOrder.verify(mapToJobService).update(ID, Map.of(STATUS, RUNNING));
        inOrder.verify(uniProtKBToUniRefCompositeRequestProcessor)
                .process(uniProtKBToUniRefMapDownloadRequest);
        inOrder.verify(mapToJobService).update(ID, Map.of(STATUS, FINISHED, RESULT_FILE, ID));
    }

    @Test
    void process_mapUnknown() {
        when(uniProtKBToUniRefMapDownloadRequest.getDownloadJobId()).thenReturn(ID);
        when(uniProtKBToUniRefMapDownloadRequest.getFrom()).thenReturn("unknown");

        assertThrows(
                IllegalArgumentException.class,
                () -> mapToRequestProcessor.process(uniProtKBToUniRefMapDownloadRequest));
    }
}
