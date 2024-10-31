package org.uniprot.api.async.download.messaging.consumer.processor.idmapping;

import java.lang.reflect.Type;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.uniprot.api.async.download.messaging.config.idmapping.IdMappingDownloadConfigProperties;
import org.uniprot.api.async.download.messaging.consumer.heartbeat.idmapping.IdMappingHeartbeatProducer;
import org.uniprot.api.async.download.messaging.consumer.streamer.facade.idmapping.UniRefIdMappingResultStreamerFacade;
import org.uniprot.api.idmapping.common.response.model.UniRefEntryPair;
import org.uniprot.api.rest.output.context.MessageConverterContext;
import org.uniprot.api.rest.output.converter.UUWMessageConverterFactory;
import org.uniprot.core.uniref.UniRefEntryLight;

@Component
public class UniRefIdMappingResultRequestProcessor
        extends IdMappingResultRequestProcessor<UniRefEntryLight, UniRefEntryPair> {
    protected UniRefIdMappingResultRequestProcessor(
            IdMappingDownloadConfigProperties downloadConfigProperties,
            IdMappingHeartbeatProducer heartbeatProducer,
            UniRefIdMappingResultStreamerFacade resultStreamerFacade,
            UUWMessageConverterFactory uuwMessageConverterFactory) {
        super(
                downloadConfigProperties,
                heartbeatProducer,
                resultStreamerFacade,
                uuwMessageConverterFactory);
    }

    @Override
    protected Type getType() {
        return new ParameterizedTypeReference<
                MessageConverterContext<UniRefEntryPair>>() {}.getType();
    }
}
