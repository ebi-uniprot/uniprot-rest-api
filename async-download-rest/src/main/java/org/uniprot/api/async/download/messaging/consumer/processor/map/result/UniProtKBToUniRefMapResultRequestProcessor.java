package org.uniprot.api.async.download.messaging.consumer.processor.map.result;

import java.lang.reflect.Type;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.uniprot.api.async.download.messaging.config.map.MapDownloadConfigProperties;
import org.uniprot.api.async.download.messaging.consumer.heartbeat.map.MapHeartbeatProducer;
import org.uniprot.api.async.download.messaging.consumer.streamer.facade.map.UniProtKBToUniRefMapSolrIdResultStreamerFacade;
import org.uniprot.api.async.download.model.request.map.UniProtKBToUniRefMapDownloadRequest;
import org.uniprot.api.rest.output.context.MessageConverterContext;
import org.uniprot.api.rest.output.converter.UUWMessageConverterFactory;
import org.uniprot.core.uniref.UniRefEntryLight;

@Component
public class UniProtKBToUniRefMapResultRequestProcessor
        extends UniRefMapResultRequestProcessor<UniProtKBToUniRefMapDownloadRequest> {
    private static final Type type =
            (new ParameterizedTypeReference<MessageConverterContext<UniRefEntryLight>>() {})
                    .getType();

    protected UniProtKBToUniRefMapResultRequestProcessor(
            MapDownloadConfigProperties downloadConfigProperties,
            MapHeartbeatProducer heartbeatProducer,
            UniProtKBToUniRefMapSolrIdResultStreamerFacade solrIdResultStreamerFacade,
            UUWMessageConverterFactory uuwMessageConverterFactory) {
        super(
                downloadConfigProperties,
                heartbeatProducer,
                solrIdResultStreamerFacade,
                uuwMessageConverterFactory);
    }

    @Override
    protected Type getType() {
        return type;
    }
}
