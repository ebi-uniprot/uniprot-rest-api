package org.uniprot.api.async.download.refactor.consumer.processor.result.uniref;

import java.lang.reflect.Type;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.uniprot.api.async.download.messaging.config.uniref.UniRefDownloadConfigProperties;
import org.uniprot.api.async.download.messaging.listener.uniref.UniRefHeartbeatProducer;
import org.uniprot.api.async.download.messaging.result.uniref.UniRefAsyncDownloadFileHandler;
import org.uniprot.api.async.download.model.uniref.UniRefDownloadJob;
import org.uniprot.api.async.download.refactor.consumer.processor.result.SolrIdResultRequestProcessor;
import org.uniprot.api.async.download.refactor.consumer.streamer.facade.uniref.UniRefSolrIdResultStreamerFacade;
import org.uniprot.api.async.download.refactor.request.uniref.UniRefDownloadRequest;
import org.uniprot.api.rest.output.context.MessageConverterContext;
import org.uniprot.api.rest.output.converter.UUWMessageConverterFactory;
import org.uniprot.core.uniref.UniRefEntryLight;

@Component
public class UniRefSolrIdResultRequestProcessor
        extends SolrIdResultRequestProcessor<UniRefDownloadRequest, UniRefDownloadJob, UniRefEntryLight> {
    private static final Type type =
            (new ParameterizedTypeReference<MessageConverterContext<UniRefEntryLight>>() {})
                    .getType();

    public UniRefSolrIdResultRequestProcessor(
            UniRefDownloadConfigProperties downloadConfigProperties,
            UniRefHeartbeatProducer heartbeatProducer,
            UniRefSolrIdResultStreamerFacade uniProtKBResultStreamerFacade,
            UUWMessageConverterFactory uuwMessageConverterFactory) {
        super(
                downloadConfigProperties,
                heartbeatProducer,
                uniProtKBResultStreamerFacade,
                uuwMessageConverterFactory);
    }

    @Override
    protected Type getType() {
        return type;
    }
}