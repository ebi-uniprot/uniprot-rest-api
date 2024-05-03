package org.uniprot.api.async.download.refactor.consumer.processor.result.uniprotkb;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.uniprot.api.async.download.messaging.config.uniprotkb.UniProtKBDownloadConfigProperties;
import org.uniprot.api.async.download.messaging.listener.uniprotkb.UniProtKBHeartbeatProducer;
import org.uniprot.api.async.download.messaging.result.uniprotkb.UniProtKBAsyncDownloadFileHandler;
import org.uniprot.api.async.download.model.uniprotkb.UniProtKBDownloadJob;
import org.uniprot.api.async.download.refactor.consumer.processor.result.ResultRequestProcessorTest;
import org.uniprot.api.async.download.refactor.consumer.processor.result.uniprotkb.UniProtKBResultRequestProcessor;
import org.uniprot.api.async.download.refactor.consumer.streamer.facade.uniprotkb.UniProtKBResultStreamerFacade;
import org.uniprot.api.async.download.refactor.request.uniprotkb.UniProtKBDownloadRequest;
import org.uniprot.api.rest.output.converter.UUWMessageConverterFactory;
import org.uniprot.core.uniprotkb.UniProtKBEntry;

@ExtendWith(MockitoExtension.class)
public class UniProtKBResultRequestProcessorTest extends ResultRequestProcessorTest<UniProtKBDownloadRequest, UniProtKBDownloadJob, UniProtKBEntry> {
    @Mock private UniProtKBDownloadConfigProperties uniProtKBDownloadConfigProperties;
    @Mock private UniProtKBHeartbeatProducer uniProtKBHeartbeatProducer;
    @Mock private UniProtKBAsyncDownloadFileHandler uniProtKBAsyncDownloadFileHandler;
    @Mock private UniProtKBResultStreamerFacade uniProtKBResultStreamerFacade;
    @Mock private UUWMessageConverterFactory uuwMessageConverterFactory;
    @Mock private UniProtKBDownloadRequest uniProtKBDownloadRequest;

    @BeforeEach
    void setUp() {
        downloadConfigProperties = uniProtKBDownloadConfigProperties;
        heartbeatProducer = uniProtKBHeartbeatProducer;
        fileHandler = uniProtKBAsyncDownloadFileHandler;
        resultStreamerFacade = uniProtKBResultStreamerFacade;
        messageConverterFactory = uuwMessageConverterFactory;
        resultRequestProcessor = new UniProtKBResultRequestProcessor(uniProtKBDownloadConfigProperties, uniProtKBHeartbeatProducer, uniProtKBAsyncDownloadFileHandler, uniProtKBResultStreamerFacade, uuwMessageConverterFactory);
        request = uniProtKBDownloadRequest;
    }
}
