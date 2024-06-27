package org.uniprot.api.async.download.messaging.consumer.processor.result.uniparc;

import java.lang.reflect.Type;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.uniprot.api.async.download.messaging.config.uniparc.UniParcDownloadConfigProperties;
import org.uniprot.api.async.download.messaging.consumer.heartbeat.uniparc.UniParcHeartbeatProducer;
import org.uniprot.api.async.download.messaging.consumer.processor.result.SolrIdResultRequestProcessor;
import org.uniprot.api.async.download.messaging.consumer.streamer.facade.uniparc.UniParcSolrIdResultStreamerFacade;
import org.uniprot.api.async.download.model.job.uniparc.UniParcDownloadJob;
import org.uniprot.api.async.download.model.request.uniparc.UniParcDownloadRequest;
import org.uniprot.api.rest.output.context.MessageConverterContext;
import org.uniprot.api.rest.output.converter.UUWMessageConverterFactory;
import org.uniprot.core.uniparc.UniParcEntry;

@Component
public class UniParcSolrIdResultRequestProcessor
        extends SolrIdResultRequestProcessor<
                UniParcDownloadRequest, UniParcDownloadJob, UniParcEntry> {
    private static final Type type =
            (new ParameterizedTypeReference<MessageConverterContext<UniParcEntry>>() {}).getType();

    public UniParcSolrIdResultRequestProcessor(
            UniParcDownloadConfigProperties downloadConfigProperties,
            UniParcHeartbeatProducer heartbeatProducer,
            UniParcSolrIdResultStreamerFacade uniParcSolrIdResultStreamerFacade,
            UUWMessageConverterFactory uuwMessageConverterFactory) {
        super(
                downloadConfigProperties,
                heartbeatProducer,
                uniParcSolrIdResultStreamerFacade,
                uuwMessageConverterFactory);
    }

    @Override
    protected Type getType() {
        return type;
    }
}
