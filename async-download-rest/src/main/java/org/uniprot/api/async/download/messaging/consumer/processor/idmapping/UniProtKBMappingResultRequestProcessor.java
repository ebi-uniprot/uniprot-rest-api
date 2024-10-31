package org.uniprot.api.async.download.messaging.consumer.processor.idmapping;

import java.lang.reflect.Type;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.uniprot.api.async.download.messaging.config.idmapping.IdMappingDownloadConfigProperties;
import org.uniprot.api.async.download.messaging.consumer.heartbeat.idmapping.IdMappingHeartbeatProducer;
import org.uniprot.api.async.download.messaging.consumer.streamer.facade.idmapping.UniProtKBIdMappingResultStreamerFacade;
import org.uniprot.api.idmapping.common.response.model.UniProtKBEntryPair;
import org.uniprot.api.rest.output.context.MessageConverterContext;
import org.uniprot.api.rest.output.converter.UUWMessageConverterFactory;
import org.uniprot.core.uniprotkb.UniProtKBEntry;

@Component
public class UniProtKBMappingResultRequestProcessor
        extends IdMappingResultRequestProcessor<UniProtKBEntry, UniProtKBEntryPair> {
    protected UniProtKBMappingResultRequestProcessor(
            IdMappingDownloadConfigProperties downloadConfigProperties,
            IdMappingHeartbeatProducer heartbeatProducer,
            UniProtKBIdMappingResultStreamerFacade resultStreamerFacade,
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
                MessageConverterContext<UniProtKBEntryPair>>() {}.getType();
    }
}
