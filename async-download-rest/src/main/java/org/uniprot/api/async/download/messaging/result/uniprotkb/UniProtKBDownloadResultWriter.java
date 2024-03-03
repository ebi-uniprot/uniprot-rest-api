package org.uniprot.api.async.download.messaging.result.uniprotkb;

import java.lang.reflect.Type;
import java.util.Iterator;

import lombok.extern.slf4j.Slf4j;

import org.springframework.context.annotation.Profile;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter;
import org.uniprot.api.async.download.messaging.config.common.DownloadConfigProperties;
import org.uniprot.api.async.download.messaging.listener.common.HeartbeatProducer;
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

@Component("uniProtKBDownloadResultWriter")
@Slf4j
@Profile({"live", "asyncDownload"})
public class UniProtKBDownloadResultWriter extends AbstractDownloadResultWriter<UniProtKBEntry> {

    protected final TaxonomyLineageService lineageService;
    private static final Type type =
            (new ParameterizedTypeReference<MessageConverterContext<UniProtKBEntry>>() {})
                    .getType();

    public UniProtKBDownloadResultWriter(
            RequestMappingHandlerAdapter contentAdapter,
            MessageConverterContextFactory<UniProtKBEntry> converterContextFactory,
            StoreStreamerConfig<UniProtKBEntry> storeStreamerConfig,
            DownloadConfigProperties uniProtKBDownloadConfigProperties,
            TaxonomyLineageService lineageService,
            RdfStreamer uniProtRdfStreamer,
            HeartbeatProducer heartBeatProducer) {
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
