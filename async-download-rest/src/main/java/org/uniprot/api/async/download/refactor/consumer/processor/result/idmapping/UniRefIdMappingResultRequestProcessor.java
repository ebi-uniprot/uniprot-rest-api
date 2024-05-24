package org.uniprot.api.async.download.refactor.consumer.processor.result.idmapping;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.uniprot.api.async.download.messaging.config.idmapping.IdMappingDownloadConfigProperties;
import org.uniprot.api.async.download.messaging.listener.idmapping.IdMappingHeartbeatProducer;
import org.uniprot.api.async.download.refactor.consumer.processor.result.IdMappingResultRequestProcessor;
import org.uniprot.api.async.download.refactor.consumer.streamer.facade.idmapping.UniRefIdMappingResultStreamerFacade;
import org.uniprot.api.idmapping.common.response.model.UniRefEntryPair;
import org.uniprot.api.rest.output.context.MessageConverterContext;
import org.uniprot.api.rest.output.converter.UUWMessageConverterFactory;
import org.uniprot.core.uniref.UniRefEntryLight;

import java.lang.reflect.Type;

@Component
public class UniRefIdMappingResultRequestProcessor extends IdMappingResultRequestProcessor<UniRefEntryLight, UniRefEntryPair> {
    protected UniRefIdMappingResultRequestProcessor(IdMappingDownloadConfigProperties downloadConfigProperties, IdMappingHeartbeatProducer heartbeatProducer, UniRefIdMappingResultStreamerFacade resultStreamerFacade, UUWMessageConverterFactory uuwMessageConverterFactory) {
        super(downloadConfigProperties, heartbeatProducer, resultStreamerFacade, uuwMessageConverterFactory);
    }

    @Override
    protected Type getType() {
        return new ParameterizedTypeReference<MessageConverterContext<UniRefEntryPair>>() {
        }.getType();
    }
}
