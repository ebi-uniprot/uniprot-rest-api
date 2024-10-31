package org.uniprot.api.async.download.messaging.consumer.processor.uniparc.composite;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.uniprot.api.async.download.messaging.consumer.processor.CompositeRequestProcessorTest;
import org.uniprot.api.async.download.messaging.consumer.processor.uniparc.id.UniParcSolrIdRequestProcessor;
import org.uniprot.api.async.download.messaging.consumer.processor.uniparc.result.UniParcSolrIdResultRequestProcessor;
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
