package org.uniprot.api.async.download.messaging.consumer.processor.composite.uniref;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.uniprot.api.async.download.messaging.consumer.processor.composite.CompositeRequestProcessorTest;
import org.uniprot.api.async.download.messaging.consumer.processor.id.uniref.UniRefSolrIdRequestProcessor;
import org.uniprot.api.async.download.messaging.consumer.processor.result.uniref.UniRefSolrIdResultRequestProcessor;
import org.uniprot.api.async.download.model.request.uniref.UniRefDownloadRequest;

@ExtendWith(MockitoExtension.class)
class UniRefCompositeRequestProcessorTest
        extends CompositeRequestProcessorTest<UniRefDownloadRequest> {
    @Mock private UniRefSolrIdRequestProcessor uniRefSolrIdRequestProcessor;
    @Mock private UniRefSolrIdResultRequestProcessor uniRefResultRequestProcessor;
    @Mock private UniRefDownloadRequest uniRefDownloadRequest;

    @BeforeEach
    void setUp() {
        downloadRequest = uniRefDownloadRequest;
        requestProcessor1 = uniRefSolrIdRequestProcessor;
        requestProcessor2 = uniRefResultRequestProcessor;
        compositeRequestProcessor =
                new UniRefCompositeRequestProcessor(
                        uniRefSolrIdRequestProcessor, uniRefResultRequestProcessor);
    }
}
