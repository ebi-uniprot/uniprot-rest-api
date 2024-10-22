package org.uniprot.api.async.download.messaging.consumer.processor.uniprotkb.result;

import java.lang.reflect.Type;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.uniprot.api.async.download.messaging.config.uniprotkb.UniProtKBDownloadConfigProperties;
import org.uniprot.api.async.download.messaging.consumer.heartbeat.uniprotkb.UniProtKBHeartbeatProducer;
import org.uniprot.api.async.download.messaging.consumer.processor.SolrIdResultRequestProcessor;
import org.uniprot.api.async.download.messaging.consumer.streamer.facade.uniprotkb.UniProtKBSolrIdResultStreamerFacade;
import org.uniprot.api.async.download.model.job.uniprotkb.UniProtKBDownloadJob;
import org.uniprot.api.async.download.model.request.uniprotkb.UniProtKBDownloadRequest;
import org.uniprot.api.rest.output.context.MessageConverterContext;
import org.uniprot.api.rest.output.converter.UUWMessageConverterFactory;
import org.uniprot.core.uniprotkb.UniProtKBEntry;

@Component
public class UniProtKBSolrIdResultRequestProcessor
        extends SolrIdResultRequestProcessor<
                UniProtKBDownloadRequest, UniProtKBDownloadJob, UniProtKBEntry> {
    private static final Type type =
            (new ParameterizedTypeReference<MessageConverterContext<UniProtKBEntry>>() {})
                    .getType();

    public UniProtKBSolrIdResultRequestProcessor(
            UniProtKBDownloadConfigProperties downloadConfigProperties,
            UniProtKBHeartbeatProducer heartbeatProducer,
            UniProtKBSolrIdResultStreamerFacade uniProtKBResultStreamerFacade,
            UUWMessageConverterFactory uuwMessageConverterFactory) {
        super(
                downloadConfigProperties,
                heartbeatProducer,
                uniProtKBResultStreamerFacade,
                uuwMessageConverterFactory);
    }

    @Override
    protected Type getType() {
        return type;
    }
}
