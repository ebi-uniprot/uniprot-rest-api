package org.uniprot.api.async.download.messaging.consumer.processor.composite.map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.uniprot.api.async.download.messaging.consumer.processor.map.from.UniProtKBMapFromRequestProcessor;
import org.uniprot.api.async.download.messaging.consumer.processor.map.result.UniProtKBToUniRefMapResultRequestProcessor;
import org.uniprot.api.async.download.messaging.consumer.processor.map.to.UniRefMapToRequestProcessor;
import org.uniprot.api.async.download.model.request.map.UniProtKBToUniRefMapDownloadRequest;

@ExtendWith(MockitoExtension.class)
public class UniProtKBToUniRefMapCompositeRequestProcessorTest
        extends MapCompositeRequestProcessorTest<UniProtKBToUniRefMapDownloadRequest> {
    @Mock UniProtKBToUniRefMapDownloadRequest uniProtKBToUniRefMapDownloadRequest;
    @Mock private UniProtKBMapFromRequestProcessor uniProtKBMapFromRequestProcessor;
    @Mock private UniRefMapToRequestProcessor uniRefMapToRequestProcessor;

    @Mock
    private UniProtKBToUniRefMapResultRequestProcessor uniProtKBToUniRefMapResultRequestProcessor;

    @BeforeEach
    void setUp() {
        downloadRequest = uniProtKBToUniRefMapDownloadRequest;
        requestProcessor1 = uniProtKBMapFromRequestProcessor;
        requestProcessor2 = uniRefMapToRequestProcessor;
        requestProcessor3 = uniProtKBToUniRefMapResultRequestProcessor;
        compositeRequestProcessor =
                new UniProtKBToUniRefMapCompositeRequestProcessor(
                        uniProtKBMapFromRequestProcessor,
                        uniRefMapToRequestProcessor,
                        uniProtKBToUniRefMapResultRequestProcessor);
    }
}
