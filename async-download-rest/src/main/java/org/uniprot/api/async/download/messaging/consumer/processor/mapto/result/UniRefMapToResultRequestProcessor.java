package org.uniprot.api.async.download.messaging.consumer.processor.mapto.result;

import java.lang.reflect.Type;

import org.springframework.core.ParameterizedTypeReference;
import org.uniprot.api.async.download.messaging.config.mapto.MapToDownloadConfigProperties;
import org.uniprot.api.async.download.messaging.consumer.heartbeat.mapto.MapToHeartbeatProducer;
import org.uniprot.api.async.download.messaging.consumer.processor.SolrIdResultRequestProcessor;
import org.uniprot.api.async.download.messaging.consumer.streamer.facade.mapto.UniRefMapToSolrIdResultStreamerFacade;
import org.uniprot.api.async.download.model.job.mapto.MapToDownloadJob;
import org.uniprot.api.async.download.model.request.mapto.MapToDownloadRequest;
import org.uniprot.api.rest.output.context.MessageConverterContext;
import org.uniprot.api.rest.output.converter.UUWMessageConverterFactory;
import org.uniprot.core.uniref.UniRefEntryLight;

public abstract class UniRefMapToResultRequestProcessor<T extends MapToDownloadRequest>
        extends SolrIdResultRequestProcessor<T, MapToDownloadJob, UniRefEntryLight> {
    private static final Type type =
            (new ParameterizedTypeReference<MessageConverterContext<UniRefEntryLight>>() {})
                    .getType();

    protected UniRefMapToResultRequestProcessor(
            MapToDownloadConfigProperties downloadConfigProperties,
            MapToHeartbeatProducer heartbeatProducer,
            UniRefMapToSolrIdResultStreamerFacade<T> solrIdResultStreamerFacade,
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
