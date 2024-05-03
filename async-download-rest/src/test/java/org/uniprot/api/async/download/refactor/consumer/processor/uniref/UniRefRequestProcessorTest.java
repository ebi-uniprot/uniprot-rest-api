package org.uniprot.api.async.download.refactor.consumer.processor.uniref;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.uniprot.api.async.download.refactor.consumer.processor.composite.uniref.UniRefCompositeRequestProcessor;
import org.uniprot.api.async.download.refactor.request.uniref.UniRefDownloadRequest;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class UniRefRequestProcessorTest {
    @Mock
    private UniRefDownloadRequest unirefDownloadRequest;
    @Mock
    private UniRefCompositeRequestProcessor unirefCompositeRequestProcessor;
    @InjectMocks
    private UniRefRequestProcessor unirefRequestProcessor;

    @Test
    void process() {
        unirefRequestProcessor.process(unirefDownloadRequest);

        verify(unirefCompositeRequestProcessor).process(unirefDownloadRequest);
    }
}