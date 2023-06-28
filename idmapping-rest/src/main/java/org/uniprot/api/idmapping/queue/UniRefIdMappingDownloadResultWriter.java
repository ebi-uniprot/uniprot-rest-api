package org.uniprot.api.idmapping.queue;

import java.lang.reflect.Type;
import java.util.Iterator;

import lombok.extern.slf4j.Slf4j;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter;
import org.uniprot.api.common.repository.stream.rdf.RdfStreamer;
import org.uniprot.api.common.repository.stream.store.StoreStreamerConfig;
import org.uniprot.api.idmapping.model.IdMappingStringPair;
import org.uniprot.api.idmapping.model.UniRefEntryPair;
import org.uniprot.api.idmapping.service.store.BatchStoreEntryPairIterable;
import org.uniprot.api.idmapping.service.store.impl.UniRefBatchStoreEntryPairIterable;
import org.uniprot.api.rest.download.queue.DownloadConfigProperties;
import org.uniprot.api.rest.output.context.MessageConverterContext;
import org.uniprot.api.rest.output.context.MessageConverterContextFactory;
import org.uniprot.core.uniref.UniRefEntryLight;

@Component
@Slf4j
public class UniRefIdMappingDownloadResultWriter
        extends AbstractIdMappingDownloadResultWriter<UniRefEntryPair, UniRefEntryLight> {

    private static final Type TYPE =
            (new ParameterizedTypeReference<MessageConverterContext<UniRefEntryPair>>() {})
                    .getType();

    public UniRefIdMappingDownloadResultWriter(
            RequestMappingHandlerAdapter contentAdapter,
            MessageConverterContextFactory<UniRefEntryPair> converterContextFactory,
            StoreStreamerConfig<UniRefEntryLight> storeStreamerConfig,
            DownloadConfigProperties downloadConfigProperties,
            RdfStreamer idMappingRdfStreamer) {
        super(
                contentAdapter,
                converterContextFactory,
                storeStreamerConfig,
                downloadConfigProperties,
                idMappingRdfStreamer,
                MessageConverterContextFactory.Resource.UNIREF);
    }

    @Override
    public BatchStoreEntryPairIterable<UniRefEntryPair, UniRefEntryLight>
            getBatchStoreEntryPairIterable(Iterator<IdMappingStringPair> mappedIds, String fields) {
        return new UniRefBatchStoreEntryPairIterable(
                mappedIds,
                storeStreamerConfig.getStreamConfig().getStoreBatchSize(),
                storeStreamerConfig.getStoreClient(),
                storeStreamerConfig.getStoreFetchRetryPolicy());
    }

    @Override
    public Type getType() {
        return TYPE;
    }
}
