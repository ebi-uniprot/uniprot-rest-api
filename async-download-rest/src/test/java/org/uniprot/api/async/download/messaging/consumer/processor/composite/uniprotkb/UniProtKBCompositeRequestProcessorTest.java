package org.uniprot.api.async.download.messaging.consumer.processor.composite.uniprotkb;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.uniprot.api.async.download.messaging.consumer.processor.composite.CompositeRequestProcessorTest;
import org.uniprot.api.async.download.messaging.consumer.processor.id.uniprotkb.UniProtKBSolrIdRequestProcessor;
import org.uniprot.api.async.download.messaging.consumer.processor.result.uniprotkb.UniProtKBSolrIdResultRequestProcessor;
import org.uniprot.api.async.download.model.request.uniprotkb.UniProtKBDownloadRequest;

@ExtendWith(MockitoExtension.class)
class UniProtKBCompositeRequestProcessorTest
        extends CompositeRequestProcessorTest<UniProtKBDownloadRequest> {
    @Mock private UniProtKBSolrIdRequestProcessor uniProtKBSolrIdRequestProcessor;
    @Mock private UniProtKBSolrIdResultRequestProcessor uniProtKBResultRequestProcessor;
    @Mock private UniProtKBDownloadRequest uniProtKBDownloadRequest;

    @BeforeEach
    void setUp() {
        downloadRequest = uniProtKBDownloadRequest;
        requestProcessor1 = uniProtKBSolrIdRequestProcessor;
        requestProcessor2 = uniProtKBResultRequestProcessor;
        compositeRequestProcessor =
                new UniProtKBCompositeRequestProcessor(
                        uniProtKBSolrIdRequestProcessor, uniProtKBResultRequestProcessor);
    }
}
