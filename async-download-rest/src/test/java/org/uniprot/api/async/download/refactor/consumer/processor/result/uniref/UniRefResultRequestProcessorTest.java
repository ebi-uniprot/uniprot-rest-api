package org.uniprot.api.async.download.refactor.consumer.processor.result.uniref;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.uniprot.api.async.download.messaging.config.uniref.UniRefDownloadConfigProperties;
import org.uniprot.api.async.download.messaging.listener.uniref.UniRefHeartbeatProducer;
import org.uniprot.api.async.download.messaging.result.uniref.UniRefAsyncDownloadFileHandler;
import org.uniprot.api.async.download.model.uniref.UniRefDownloadJob;
import org.uniprot.api.async.download.refactor.consumer.processor.result.ResultRequestProcessorTest;
import org.uniprot.api.async.download.refactor.consumer.processor.result.uniref.UniRefResultRequestProcessor;
import org.uniprot.api.async.download.refactor.consumer.streamer.facade.uniref.UniRefResultStreamerFacade;
import org.uniprot.api.async.download.refactor.request.uniref.UniRefDownloadRequest;
import org.uniprot.api.rest.output.converter.UUWMessageConverterFactory;
import org.uniprot.core.uniref.UniRefEntryLight;

@ExtendWith(MockitoExtension.class)
public class UniRefResultRequestProcessorTest extends ResultRequestProcessorTest<UniRefDownloadRequest, UniRefDownloadJob, UniRefEntryLight> {
    @Mock private UniRefDownloadConfigProperties uniRefDownloadConfigProperties;
    @Mock private UniRefHeartbeatProducer uniRefHeartbeatProducer;
    @Mock private UniRefAsyncDownloadFileHandler uniRefAsyncDownloadFileHandler;
    @Mock private UniRefResultStreamerFacade uniRefResultStreamerFacade;
    @Mock private UUWMessageConverterFactory uuwMessageConverterFactory;
    @Mock private UniRefDownloadRequest uniRefDownloadRequest;

    @BeforeEach
    void setUp() {
        downloadConfigProperties = uniRefDownloadConfigProperties;
        heartbeatProducer = uniRefHeartbeatProducer;
        fileHandler = uniRefAsyncDownloadFileHandler;
        resultStreamerFacade = uniRefResultStreamerFacade;
        messageConverterFactory = uuwMessageConverterFactory;
        resultRequestProcessor = new UniRefResultRequestProcessor(uniRefDownloadConfigProperties, uniRefHeartbeatProducer, uniRefAsyncDownloadFileHandler, uniRefResultStreamerFacade, uuwMessageConverterFactory);
        request = uniRefDownloadRequest;
    }
}
