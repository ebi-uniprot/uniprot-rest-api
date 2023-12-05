package org.uniprot.api.uniref.queue;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter;
import org.uniprot.api.common.repository.stream.rdf.RdfStreamer;
import org.uniprot.api.common.repository.stream.store.BatchStoreIterable;
import org.uniprot.api.common.repository.stream.store.StoreRequest;
import org.uniprot.api.common.repository.stream.store.StoreStreamerConfig;
import org.uniprot.api.rest.download.AbstractDownloadResultWriter;
import org.uniprot.api.rest.download.heartbeat.HeartBeatProducer;
import org.uniprot.api.rest.download.queue.DownloadConfigProperties;
import org.uniprot.api.rest.output.context.MessageConverterContext;
import org.uniprot.api.rest.output.context.MessageConverterContextFactory;
import org.uniprot.core.uniref.UniRefEntryLight;

import java.lang.reflect.Type;
import java.util.Iterator;

@Component
@Slf4j
public class UniRefDownloadResultWriter extends AbstractDownloadResultWriter<UniRefEntryLight> {

    private static final Type type =
            (new ParameterizedTypeReference<MessageConverterContext<UniRefEntryLight>>() {})
                    .getType();

    public UniRefDownloadResultWriter(
            RequestMappingHandlerAdapter contentAdapter,
            MessageConverterContextFactory<UniRefEntryLight> converterContextFactory,
            StoreStreamerConfig<UniRefEntryLight> storeStreamerConfig,
            DownloadConfigProperties downloadConfigProperties,
            RdfStreamer uniRefRdfStreamer, HeartBeatProducer heartBeatProducer) {
        super(
                contentAdapter,
                converterContextFactory,
                storeStreamerConfig,
                downloadConfigProperties,
                uniRefRdfStreamer,
                MessageConverterContextFactory.Resource.UNIREF, heartBeatProducer);
    }

    @Override
    public BatchStoreIterable<UniRefEntryLight> getBatchStoreIterable(
            Iterator<String> idsIterator, StoreRequest storeRequest) {
        return new BatchStoreIterable<>(
                idsIterator,
                storeStreamerConfig.getStoreClient(),
                storeStreamerConfig.getStoreFetchRetryPolicy(),
                storeStreamerConfig.getStreamConfig().getStoreBatchSize());
    }

    @Override
    public Type getType() {
        return type;
    }
}
