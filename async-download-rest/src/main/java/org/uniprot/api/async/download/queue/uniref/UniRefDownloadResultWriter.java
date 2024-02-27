package org.uniprot.api.async.download.queue.uniref;

import java.lang.reflect.Type;
import java.util.Iterator;

import lombok.extern.slf4j.Slf4j;

import org.springframework.context.annotation.Profile;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter;
import org.uniprot.api.async.download.queue.common.AbstractDownloadResultWriter;
import org.uniprot.api.async.download.queue.common.DownloadConfigProperties;
import org.uniprot.api.async.download.queue.common.HeartBeatProducer;
import org.uniprot.api.common.repository.stream.rdf.RdfStreamer;
import org.uniprot.api.common.repository.stream.store.BatchStoreIterable;
import org.uniprot.api.common.repository.stream.store.StoreRequest;
import org.uniprot.api.common.repository.stream.store.StoreStreamerConfig;
import org.uniprot.api.rest.output.context.MessageConverterContext;
import org.uniprot.api.rest.output.context.MessageConverterContextFactory;
import org.uniprot.core.uniref.UniRefEntryLight;

@Component("uniRefDownloadResultWriter")
@Slf4j
@Profile({"live", "asyncDownload"})
public class UniRefDownloadResultWriter extends AbstractDownloadResultWriter<UniRefEntryLight> {

    private static final Type type =
            (new ParameterizedTypeReference<MessageConverterContext<UniRefEntryLight>>() {})
                    .getType();

    public UniRefDownloadResultWriter(
            RequestMappingHandlerAdapter contentAdapter,
            MessageConverterContextFactory<UniRefEntryLight> converterContextFactory,
            StoreStreamerConfig<UniRefEntryLight> storeStreamerConfig,
            DownloadConfigProperties uniRefDownloadConfigProperties,
            RdfStreamer uniRefRdfStreamer,
            HeartBeatProducer heartBeatProducer) {
        super(
                contentAdapter,
                converterContextFactory,
                storeStreamerConfig,
                uniRefDownloadConfigProperties,
                uniRefRdfStreamer,
                MessageConverterContextFactory.Resource.UNIREF,
                heartBeatProducer);
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
