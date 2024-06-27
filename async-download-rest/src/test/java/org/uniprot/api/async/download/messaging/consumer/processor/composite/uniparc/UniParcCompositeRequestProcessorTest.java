package org.uniprot.api.async.download.messaging.consumer.processor.composite.uniparc;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.uniprot.api.async.download.messaging.consumer.processor.composite.CompositeRequestProcessorTest;
import org.uniprot.api.async.download.messaging.consumer.processor.id.uniparc.UniParcSolrIdRequestProcessor;
import org.uniprot.api.async.download.messaging.consumer.processor.result.uniparc.UniParcSolrIdResultRequestProcessor;
import org.uniprot.api.async.download.model.request.uniparc.UniParcDownloadRequest;

@ExtendWith(MockitoExtension.class)
class UniParcCompositeRequestProcessorTest
        extends CompositeRequestProcessorTest<UniParcDownloadRequest> {
    @Mock private UniParcSolrIdRequestProcessor uniParcSolrIdRequestProcessor;
    @Mock private UniParcSolrIdResultRequestProcessor uniParcResultRequestProcessor;
    @Mock private UniParcDownloadRequest uniParcDownloadRequest;

    @BeforeEach
    void setUp() {
        downloadRequest = uniParcDownloadRequest;
        requestProcessor1 = uniParcSolrIdRequestProcessor;
        requestProcessor2 = uniParcResultRequestProcessor;
        compositeRequestProcessor =
                new UniParcCompositeRequestProcessor(
                        uniParcSolrIdRequestProcessor, uniParcResultRequestProcessor);
    }
}
