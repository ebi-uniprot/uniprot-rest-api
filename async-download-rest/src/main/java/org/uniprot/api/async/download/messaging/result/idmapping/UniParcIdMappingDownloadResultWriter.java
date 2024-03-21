package org.uniprot.api.async.download.messaging.result.idmapping;

import java.lang.reflect.Type;
import java.util.Iterator;

import lombok.extern.slf4j.Slf4j;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter;
import org.uniprot.api.async.download.messaging.config.common.DownloadConfigProperties;
import org.uniprot.api.async.download.messaging.listener.idmapping.IdMappingHeartbeatProducer;
import org.uniprot.api.common.repository.stream.rdf.RdfStreamer;
import org.uniprot.api.common.repository.stream.store.StoreStreamerConfig;
import org.uniprot.api.idmapping.common.response.model.IdMappingStringPair;
import org.uniprot.api.idmapping.common.response.model.UniParcEntryPair;
import org.uniprot.api.idmapping.common.service.store.BatchStoreEntryPairIterable;
import org.uniprot.api.idmapping.common.service.store.impl.UniParcBatchStoreEntryPairIterable;
import org.uniprot.api.rest.output.context.MessageConverterContext;
import org.uniprot.api.rest.output.context.MessageConverterContextFactory;
import org.uniprot.core.uniparc.UniParcEntry;

@Component
@Slf4j
public class UniParcIdMappingDownloadResultWriter
        extends AbstractIdMappingDownloadResultWriter<UniParcEntryPair, UniParcEntry> {

    private static final Type TYPE =
            (new ParameterizedTypeReference<MessageConverterContext<UniParcEntryPair>>() {})
                    .getType();

    public UniParcIdMappingDownloadResultWriter(
            RequestMappingHandlerAdapter contentAdapter,
            MessageConverterContextFactory<UniParcEntryPair> converterContextFactory,
            StoreStreamerConfig<UniParcEntry> storeStreamerConfig,
            DownloadConfigProperties idMappingDownloadConfigProperties,
            RdfStreamer idMappingRdfStreamer,
            IdMappingHeartbeatProducer heartBeatProducer) {
        super(
                contentAdapter,
                converterContextFactory,
                storeStreamerConfig,
                idMappingDownloadConfigProperties,
                idMappingRdfStreamer,
                MessageConverterContextFactory.Resource.UNIPARC,
                heartBeatProducer);
    }

    @Override
    public BatchStoreEntryPairIterable<UniParcEntryPair, UniParcEntry>
            getBatchStoreEntryPairIterable(Iterator<IdMappingStringPair> mappedIds, String fields) {
        return new UniParcBatchStoreEntryPairIterable(
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
