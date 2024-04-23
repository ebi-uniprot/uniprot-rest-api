package org.uniprot.api.async.download.refactor.consumer.processor.result.uniref;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.stereotype.Component;
import org.uniprot.api.async.download.messaging.config.uniref.UniRefDownloadConfigProperties;
import org.uniprot.api.async.download.messaging.listener.uniref.UniRefHeartbeatProducer;
import org.uniprot.api.async.download.messaging.result.uniref.UniRefAsyncDownloadFileHandler;
import org.uniprot.api.async.download.model.uniref.UniRefDownloadJob;
import org.uniprot.api.async.download.refactor.consumer.processor.result.ResultRequestProcessor;
import org.uniprot.api.async.download.refactor.request.uniref.UniRefDownloadRequest;
import org.uniprot.api.async.download.refactor.consumer.streamer.facade.uniref.UniRefResultStreamerFacade;
import org.uniprot.api.rest.output.context.MessageConverterContext;
import org.uniprot.core.uniref.UniRefEntryLight;

import java.lang.reflect.Type;
import java.util.List;

@Component
public class UniRefResultRequestProcessor extends ResultRequestProcessor<UniRefDownloadRequest, UniRefDownloadJob, UniRefEntryLight> {
    private static final Type type = (new ParameterizedTypeReference<MessageConverterContext<UniRefEntryLight>>() {})
                    .getType();

    public UniRefResultRequestProcessor(UniRefDownloadConfigProperties downloadConfigProperties, UniRefHeartbeatProducer heartbeatProducer, List<HttpMessageConverter<?>> messageConverters, UniRefAsyncDownloadFileHandler fileHandler, UniRefResultStreamerFacade resultStreamerFactory) {
        super(downloadConfigProperties, heartbeatProducer, messageConverters, fileHandler, resultStreamerFactory);
    }

    @Override
    protected Type getType() {
        return type;
    }
}
