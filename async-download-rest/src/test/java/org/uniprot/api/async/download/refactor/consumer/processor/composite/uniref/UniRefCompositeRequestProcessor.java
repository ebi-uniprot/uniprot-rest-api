package org.uniprot.api.async.download.refactor.consumer.processor.composite.uniref;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.uniprot.api.async.download.refactor.consumer.processor.composite.CompositeRequestProcessorTest;
import org.uniprot.api.async.download.refactor.consumer.processor.id.uniref.UniRefSolrIdRequestProcessor;
import org.uniprot.api.async.download.refactor.consumer.processor.result.uniref.UniRefResultRequestProcessor;
import org.uniprot.api.async.download.refactor.request.uniref.UniRefDownloadRequest;

@ExtendWith(MockitoExtension.class)
class UniRefCompositeRequestProcessorTest extends CompositeRequestProcessorTest<UniRefDownloadRequest> {
    @Mock private UniRefSolrIdRequestProcessor uniRefSolrIdRequestProcessor;
    @Mock private UniRefResultRequestProcessor uniRefResultRequestProcessor;
    @Mock
    private UniRefDownloadRequest uniRefDownloadRequest;



    @BeforeEach
    void setUp() {
        downloadRequest = uniRefDownloadRequest;
        requestProcessor1 = uniRefSolrIdRequestProcessor;
        requestProcessor2 = uniRefResultRequestProcessor;
        compositeRequestProcessor = new UniRefCompositeRequestProcessor(uniRefSolrIdRequestProcessor, uniRefResultRequestProcessor);
    }
}
