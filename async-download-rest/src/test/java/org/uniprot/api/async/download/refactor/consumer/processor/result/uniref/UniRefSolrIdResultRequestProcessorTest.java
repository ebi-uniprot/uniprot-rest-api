package org.uniprot.api.async.download.refactor.consumer.processor.result.uniref;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.uniprot.api.async.download.messaging.config.uniref.UniRefDownloadConfigProperties;
import org.uniprot.api.async.download.messaging.listener.uniref.UniRefHeartbeatProducer;
import org.uniprot.api.async.download.messaging.result.uniref.UniRefAsyncDownloadFileHandler;
import org.uniprot.api.async.download.model.uniref.UniRefDownloadJob;
import org.uniprot.api.async.download.refactor.consumer.processor.result.SolrIdResultRequestProcessorTest;
import org.uniprot.api.async.download.refactor.consumer.streamer.facade.uniref.UniRefSolrIdResultStreamerFacade;
import org.uniprot.api.async.download.refactor.request.uniref.UniRefDownloadRequest;
import org.uniprot.api.rest.output.converter.UUWMessageConverterFactory;
import org.uniprot.core.uniref.UniRefEntryLight;

@ExtendWith(MockitoExtension.class)
public class UniRefSolrIdResultRequestProcessorTest
        extends SolrIdResultRequestProcessorTest<
                        UniRefDownloadRequest, UniRefDownloadJob, UniRefEntryLight> {
    @Mock private UniRefDownloadConfigProperties uniRefDownloadConfigProperties;
    @Mock private UniRefHeartbeatProducer uniRefHeartbeatProducer;
    @Mock private UniRefSolrIdResultStreamerFacade uniRefResultStreamerFacade;
    @Mock private UUWMessageConverterFactory uuwMessageConverterFactory;
    @Mock private UniRefDownloadRequest uniRefDownloadRequest;

    @BeforeEach
    void setUp() {
        downloadConfigProperties = uniRefDownloadConfigProperties;
        heartbeatProducer = uniRefHeartbeatProducer;
        solrIdResultStreamerFacade = uniRefResultStreamerFacade;
        messageConverterFactory = uuwMessageConverterFactory;
        solrIdResultRequestProcessor =
                new UniRefSolrIdResultRequestProcessor(
                        uniRefDownloadConfigProperties,
                        uniRefHeartbeatProducer,
                        uniRefResultStreamerFacade,
                        uuwMessageConverterFactory);
        request = uniRefDownloadRequest;
    }
}
