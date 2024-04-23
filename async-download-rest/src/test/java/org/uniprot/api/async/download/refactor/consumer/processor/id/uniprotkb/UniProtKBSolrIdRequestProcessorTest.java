package org.uniprot.api.async.download.refactor.consumer.processor.id.uniprotkb;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.uniprot.api.async.download.refactor.request.uniprotkb.UniProtKBDownloadRequest;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UniProtKBSolrIdRequestProcessorTest {
    @Mock
    private UniProtKBDefaultSolrIdRequestProcessor uniProtKBDefaultSolrIdRequestProcessor;
    @Mock
    private UniProtKBSolrIdH5RequestProcessor uniProtKBSolrIdH5RequestProcessor;
    @InjectMocks
    private UniProtKBSolrIdRequestProcessor uniProtKBSolrIdRequestProcessor;
    @Mock
    private UniProtKBDownloadRequest request;


    @Test
    void process_hd5ContentType() {
        when(request.getFormat()).thenReturn("application/x-hdf5");

        uniProtKBSolrIdRequestProcessor.process(request);

        verify(uniProtKBSolrIdH5RequestProcessor).process(request);
    }


    @Test
    void process() {
        when(request.getFormat()).thenReturn("application/json");

        uniProtKBSolrIdRequestProcessor.process(request);

        verify(uniProtKBDefaultSolrIdRequestProcessor).process(request);
    }
}