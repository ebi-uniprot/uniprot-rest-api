package org.uniprot.api.async.download.messaging.consumer.processor.result.uniparc;

import java.lang.reflect.Type;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.uniprot.api.async.download.messaging.config.uniparc.UniParcDownloadConfigProperties;
import org.uniprot.api.async.download.messaging.consumer.heartbeat.uniparc.UniParcHeartbeatProducer;
import org.uniprot.api.async.download.messaging.consumer.processor.SolrIdResultRequestProcessor;
import org.uniprot.api.async.download.messaging.consumer.streamer.facade.uniparc.UniParcLightSolrIdResultStreamerFacade;
import org.uniprot.api.async.download.model.job.uniparc.UniParcDownloadJob;
import org.uniprot.api.async.download.model.request.uniparc.UniParcDownloadRequest;
import org.uniprot.api.rest.output.context.MessageConverterContext;
import org.uniprot.api.rest.output.converter.UUWMessageConverterFactory;
import org.uniprot.core.uniparc.UniParcEntryLight;

@Component
public class UniParcLightSolrIdResultRequestProcessor
        extends SolrIdResultRequestProcessor<
                UniParcDownloadRequest, UniParcDownloadJob, UniParcEntryLight> {
    private static final Type type =
            (new ParameterizedTypeReference<MessageConverterContext<UniParcEntryLight>>() {})
                    .getType();

    public UniParcLightSolrIdResultRequestProcessor(
            UniParcDownloadConfigProperties downloadConfigProperties,
            UniParcHeartbeatProducer heartbeatProducer,
            UniParcLightSolrIdResultStreamerFacade uniParcLightSolrIdResultStreamerFacade,
            UUWMessageConverterFactory uuwMessageConverterFactory) {
        super(
                downloadConfigProperties,
                heartbeatProducer,
                uniParcLightSolrIdResultStreamerFacade,
                uuwMessageConverterFactory);
    }

    @Override
    protected Type getType() {
        return type;
    }
}
