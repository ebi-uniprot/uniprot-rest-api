package org.uniprot.api.async.download.messaging.consumer.processor.map.result;

import org.springframework.core.ParameterizedTypeReference;
import org.uniprot.api.async.download.messaging.config.map.MapDownloadConfigProperties;
import org.uniprot.api.async.download.messaging.consumer.heartbeat.map.MapHeartbeatProducer;
import org.uniprot.api.async.download.messaging.consumer.processor.result.SolrIdResultRequestProcessor;
import org.uniprot.api.async.download.messaging.consumer.streamer.facade.map.UniRefMapSolrIdResultStreamerFacade;
import org.uniprot.api.async.download.model.job.map.MapDownloadJob;
import org.uniprot.api.async.download.model.request.map.MapDownloadRequest;
import org.uniprot.api.rest.output.context.MessageConverterContext;
import org.uniprot.api.rest.output.converter.UUWMessageConverterFactory;
import org.uniprot.core.uniref.UniRefEntryLight;

import java.lang.reflect.Type;

public abstract class UniRefMapResultRequestProcessor<T extends MapDownloadRequest> extends SolrIdResultRequestProcessor<T, MapDownloadJob, UniRefEntryLight> {
    private static final Type type = (new ParameterizedTypeReference<MessageConverterContext<UniRefEntryLight>>() {
    }).getType();

    protected UniRefMapResultRequestProcessor(MapDownloadConfigProperties downloadConfigProperties, MapHeartbeatProducer heartbeatProducer, UniRefMapSolrIdResultStreamerFacade<T> solrIdResultStreamerFacade, UUWMessageConverterFactory uuwMessageConverterFactory) {
        super(downloadConfigProperties, heartbeatProducer, solrIdResultStreamerFacade, uuwMessageConverterFactory);
    }

    @Override
    protected Type getType() {
        return type;
    }
}
