package org.uniprot.api.async.download.refactor.consumer.processor.uniprotkb;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.uniprot.api.async.download.refactor.consumer.processor.composite.uniprotkb.UniProtKBCompositeRequestProcessor;
import org.uniprot.api.async.download.refactor.consumer.processor.id.uniprotkb.UniProtKBSolrIdHD5RequestProcessor;
import org.uniprot.api.async.download.refactor.request.uniprotkb.UniProtKBDownloadRequest;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UniProtKBRequestProcessorTest {
    private static final String JSON = "application/json";
    private static final String HD5 = "application/x-hdf5";
    @Mock
    private UniProtKBDownloadRequest uniProtKBDownloadRequest;
    @Mock
    private UniProtKBCompositeRequestProcessor uniProtKBCompositeRequestProcessor;
    @Mock
    private UniProtKBSolrIdHD5RequestProcessor uniProtKBSolrIdHD5RequestProcessor;
    @InjectMocks
    private UniProtKBRequestProcessor uniProtKBRequestProcessor;

    @Test
    void process() {
        when(uniProtKBDownloadRequest.getFormat()).thenReturn(JSON);

        uniProtKBRequestProcessor.process(uniProtKBDownloadRequest);

        verify(uniProtKBCompositeRequestProcessor).process(uniProtKBDownloadRequest);
    }

    @Test
    void process_hd5Format() {
        when(uniProtKBDownloadRequest.getFormat()).thenReturn(HD5);

        uniProtKBRequestProcessor.process(uniProtKBDownloadRequest);

        verify(uniProtKBSolrIdHD5RequestProcessor).process(uniProtKBDownloadRequest);
    }
}