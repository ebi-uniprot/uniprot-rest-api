package org.uniprot.api.async.download.refactor.consumer.processor.result.uniprotkb;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.uniprot.api.async.download.messaging.config.uniprotkb.UniProtKBDownloadConfigProperties;
import org.uniprot.api.async.download.messaging.listener.uniprotkb.UniProtKBHeartbeatProducer;
import org.uniprot.api.async.download.messaging.result.uniprotkb.UniProtKBAsyncDownloadFileHandler;
import org.uniprot.api.async.download.model.uniprotkb.UniProtKBDownloadJob;
import org.uniprot.api.async.download.refactor.consumer.processor.result.SolrIdResultRequestProcessorTest;
import org.uniprot.api.async.download.refactor.consumer.streamer.facade.uniprotkb.UniProtKBSolrIdResultStreamerFacade;
import org.uniprot.api.async.download.refactor.request.uniprotkb.UniProtKBDownloadRequest;
import org.uniprot.api.rest.output.converter.UUWMessageConverterFactory;
import org.uniprot.core.uniprotkb.UniProtKBEntry;

@ExtendWith(MockitoExtension.class)
public class UniProtKBSolrIdResultRequestProcessorTest
        extends SolrIdResultRequestProcessorTest<
                        UniProtKBDownloadRequest, UniProtKBDownloadJob, UniProtKBEntry> {
    @Mock private UniProtKBDownloadConfigProperties uniProtKBDownloadConfigProperties;
    @Mock private UniProtKBHeartbeatProducer uniProtKBHeartbeatProducer;
    @Mock private UniProtKBSolrIdResultStreamerFacade uniProtKBResultStreamerFacade;
    @Mock private UUWMessageConverterFactory uuwMessageConverterFactory;
    @Mock private UniProtKBDownloadRequest uniProtKBDownloadRequest;

    @BeforeEach
    void setUp() {
        downloadConfigProperties = uniProtKBDownloadConfigProperties;
        heartbeatProducer = uniProtKBHeartbeatProducer;
        solrIdResultStreamerFacade = uniProtKBResultStreamerFacade;
        messageConverterFactory = uuwMessageConverterFactory;
        solrIdResultRequestProcessor =
                new UniProtKBSolrIdResultRequestProcessor(
                        uniProtKBDownloadConfigProperties,
                        uniProtKBHeartbeatProducer,
                        uniProtKBResultStreamerFacade,
                        uuwMessageConverterFactory);
        request = uniProtKBDownloadRequest;
    }
}
