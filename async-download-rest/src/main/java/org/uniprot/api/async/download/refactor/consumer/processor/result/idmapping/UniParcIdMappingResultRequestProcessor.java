package org.uniprot.api.async.download.refactor.consumer.processor.result.idmapping;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.uniprot.api.async.download.messaging.config.idmapping.IdMappingDownloadConfigProperties;
import org.uniprot.api.async.download.messaging.listener.idmapping.IdMappingHeartbeatProducer;
import org.uniprot.api.async.download.refactor.consumer.processor.result.IdMappingResultRequestProcessor;
import org.uniprot.api.async.download.refactor.consumer.streamer.facade.idmapping.UniParcIdMappingResultStreamerFacade;
import org.uniprot.api.idmapping.common.response.model.UniParcEntryPair;
import org.uniprot.api.rest.output.context.MessageConverterContext;
import org.uniprot.api.rest.output.converter.UUWMessageConverterFactory;
import org.uniprot.core.uniparc.UniParcEntry;

import java.lang.reflect.Type;

@Component
public class UniParcIdMappingResultRequestProcessor extends IdMappingResultRequestProcessor<UniParcEntry, UniParcEntryPair> {
    protected UniParcIdMappingResultRequestProcessor(IdMappingDownloadConfigProperties downloadConfigProperties, IdMappingHeartbeatProducer heartbeatProducer, UniParcIdMappingResultStreamerFacade resultStreamerFacade, UUWMessageConverterFactory uuwMessageConverterFactory) {
        super(downloadConfigProperties, heartbeatProducer, resultStreamerFacade, uuwMessageConverterFactory);
    }

    @Override
    protected Type getType() {
        return new ParameterizedTypeReference<MessageConverterContext<UniParcEntryPair>>() {
        }.getType();
    }
}
