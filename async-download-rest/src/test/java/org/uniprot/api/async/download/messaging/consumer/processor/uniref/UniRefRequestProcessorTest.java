package org.uniprot.api.async.download.messaging.consumer.processor.uniref;

import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.uniprot.api.async.download.messaging.consumer.processor.composite.uniref.UniRefCompositeRequestProcessor;
import org.uniprot.api.async.download.model.request.uniref.UniRefDownloadRequest;

@ExtendWith(MockitoExtension.class)
class UniRefRequestProcessorTest {
    @Mock private UniRefDownloadRequest unirefDownloadRequest;
    @Mock private UniRefCompositeRequestProcessor unirefCompositeRequestProcessor;
    @InjectMocks private UniRefRequestProcessor unirefRequestProcessor;

    @Test
    void process() {
        unirefRequestProcessor.process(unirefDownloadRequest);

        verify(unirefCompositeRequestProcessor).process(unirefDownloadRequest);
    }
}
