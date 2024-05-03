package org.uniprot.api.async.download.refactor.consumer.processor.result.uniprotkb;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.uniprot.api.async.download.messaging.config.uniprotkb.UniProtKBDownloadConfigProperties;
import org.uniprot.api.async.download.messaging.listener.uniprotkb.UniProtKBHeartbeatProducer;
import org.uniprot.api.async.download.messaging.result.uniprotkb.UniProtKBAsyncDownloadFileHandler;
import org.uniprot.api.async.download.model.uniprotkb.UniProtKBDownloadJob;
import org.uniprot.api.async.download.refactor.consumer.processor.result.ResultRequestProcessor;
import org.uniprot.api.async.download.refactor.request.uniprotkb.UniProtKBDownloadRequest;
import org.uniprot.api.async.download.refactor.consumer.streamer.facade.uniprotkb.UniProtKBResultStreamerFacade;
import org.uniprot.api.rest.output.context.MessageConverterContext;
import org.uniprot.api.rest.output.converter.UUWMessageConverterFactory;
import org.uniprot.core.uniprotkb.UniProtKBEntry;

import java.lang.reflect.Type;

@Component
public class UniProtKBResultRequestProcessor extends ResultRequestProcessor<UniProtKBDownloadRequest, UniProtKBDownloadJob, UniProtKBEntry> {
    private static final Type type = (new ParameterizedTypeReference<MessageConverterContext<UniProtKBEntry>>() {}).getType();

    public UniProtKBResultRequestProcessor(UniProtKBDownloadConfigProperties downloadConfigProperties, UniProtKBHeartbeatProducer heartbeatProducer, UniProtKBAsyncDownloadFileHandler fileHandler, UniProtKBResultStreamerFacade uniProtKBResultStreamerFacade, UUWMessageConverterFactory uuwMessageConverterFactory) {
        super(downloadConfigProperties, heartbeatProducer, fileHandler, uniProtKBResultStreamerFacade, uuwMessageConverterFactory);
    }

    @Override
    protected Type getType() {
        return type;
    }
}
