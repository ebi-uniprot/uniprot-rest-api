package org.uniprot.api.async.download.messaging.consumer.processor.composite.mapto;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.uniprot.api.async.download.messaging.consumer.processor.mapto.from.UniProtKBToUniRefFromIdRequestProcessor;
import org.uniprot.api.async.download.messaging.consumer.processor.mapto.result.UniProtKBToUniRefResultRequestProcessor;
import org.uniprot.api.async.download.messaging.consumer.processor.mapto.to.UniProtKBToUniRefToIdRequestProcessor;
import org.uniprot.api.async.download.model.request.mapto.UniProtKBToUniRefDownloadRequest;

@ExtendWith(MockitoExtension.class)
public class UniProtKBToUniRefCompositeRequestProcessorTest
        extends MapCompositeRequestProcessorTest<UniProtKBToUniRefDownloadRequest> {
    @Mock UniProtKBToUniRefDownloadRequest uniProtKBToUniRefMapDownloadRequest;
    @Mock private UniProtKBToUniRefFromIdRequestProcessor uniProtKBMapFromRequestProcessor;
    @Mock private UniProtKBToUniRefToIdRequestProcessor uniRefMapToRequestProcessor;

    @Mock private UniProtKBToUniRefResultRequestProcessor uniProtKBToUniRefResultRequestProcessor;

    @BeforeEach
    void setUp() {
        downloadRequest = uniProtKBToUniRefMapDownloadRequest;
        requestProcessor1 = uniProtKBMapFromRequestProcessor;
        requestProcessor2 = uniRefMapToRequestProcessor;
        requestProcessor3 = uniProtKBToUniRefResultRequestProcessor;
        compositeRequestProcessor =
                new UniProtKBToUniRefCompositeRequestProcessor(
                        uniProtKBMapFromRequestProcessor,
                        uniRefMapToRequestProcessor,
                        uniProtKBToUniRefResultRequestProcessor);
    }
}
