package org.uniprot.api.async.download.messaging.consumer.processor.mapto.result;

import java.lang.reflect.Type;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.uniprot.api.async.download.messaging.config.mapto.MapToDownloadConfigProperties;
import org.uniprot.api.async.download.messaging.consumer.heartbeat.mapto.MapToHeartbeatProducer;
import org.uniprot.api.async.download.messaging.consumer.streamer.facade.map.UniProtKBToUniRefSolrIdResultStreamerFacade;
import org.uniprot.api.async.download.model.request.mapto.UniProtKBToUniRefDownloadRequest;
import org.uniprot.api.rest.output.context.MessageConverterContext;
import org.uniprot.api.rest.output.converter.UUWMessageConverterFactory;
import org.uniprot.core.uniref.UniRefEntryLight;

@Component
public class UniProtKBToUniRefResultRequestProcessor
        extends UniRefMapToResultRequestProcessor<UniProtKBToUniRefDownloadRequest> {
    private static final Type type =
            (new ParameterizedTypeReference<MessageConverterContext<UniRefEntryLight>>() {})
                    .getType();

    protected UniProtKBToUniRefResultRequestProcessor(
            MapToDownloadConfigProperties downloadConfigProperties,
            MapToHeartbeatProducer heartbeatProducer,
            UniProtKBToUniRefSolrIdResultStreamerFacade solrIdResultStreamerFacade,
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
