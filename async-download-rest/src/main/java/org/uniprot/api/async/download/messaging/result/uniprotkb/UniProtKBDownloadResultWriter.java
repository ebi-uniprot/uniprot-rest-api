package org.uniprot.api.async.download.messaging.result.uniprotkb;

import java.lang.reflect.Type;
import java.util.Iterator;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter;
import org.uniprot.api.async.download.messaging.config.uniprotkb.UniProtKBDownloadConfigProperties;
import org.uniprot.api.async.download.messaging.listener.uniprotkb.UniProtKBHeartbeatProducer;
import org.uniprot.api.async.download.messaging.result.common.AbstractDownloadResultWriter;
import org.uniprot.api.common.repository.stream.rdf.RdfStreamer;
import org.uniprot.api.common.repository.stream.store.BatchStoreIterable;
import org.uniprot.api.common.repository.stream.store.StoreRequest;
import org.uniprot.api.common.repository.stream.store.StoreStreamerConfig;
import org.uniprot.api.common.repository.stream.store.uniprotkb.TaxonomyLineageService;
import org.uniprot.api.common.repository.stream.store.uniprotkb.UniProtKBBatchStoreIterable;
import org.uniprot.api.rest.output.context.MessageConverterContext;
import org.uniprot.api.rest.output.context.MessageConverterContextFactory;
import org.uniprot.core.uniprotkb.UniProtKBEntry;

import lombok.extern.slf4j.Slf4j;

@Component("uniProtKBDownloadResultWriter")
@Slf4j
public class UniProtKBDownloadResultWriter extends AbstractDownloadResultWriter<UniProtKBEntry> {

    protected final TaxonomyLineageService lineageService;
    private static final Type type =
            (new ParameterizedTypeReference<MessageConverterContext<UniProtKBEntry>>() {})
                    .getType();

    public UniProtKBDownloadResultWriter(
            RequestMappingHandlerAdapter contentAdapter,
            MessageConverterContextFactory<UniProtKBEntry> converterContextFactory,
            StoreStreamerConfig<UniProtKBEntry> storeStreamerConfig,
            UniProtKBDownloadConfigProperties uniProtKBDownloadConfigProperties,
            TaxonomyLineageService lineageService,
            RdfStreamer uniProtRdfStreamer,
            UniProtKBHeartbeatProducer heartBeatProducer) {
        super(
                contentAdapter,
                converterContextFactory,
                storeStreamerConfig,
                uniProtKBDownloadConfigProperties,
                uniProtRdfStreamer,
                MessageConverterContextFactory.Resource.UNIPROTKB,
                heartBeatProducer);
        this.lineageService = lineageService;
    }

    @Override
    public BatchStoreIterable<UniProtKBEntry> getBatchStoreIterable(
            Iterator<String> idsIterator, StoreRequest storeRequest) {
        return new UniProtKBBatchStoreIterable(
                idsIterator,
                storeStreamerConfig.getStoreClient(),
                storeStreamerConfig.getStoreFetchRetryPolicy(),
                storeStreamerConfig.getStreamConfig().getStoreBatchSize(),
                lineageService,
                storeRequest.isAddLineage());
    }

    @Override
    public Type getType() {
        return type;
    }
}