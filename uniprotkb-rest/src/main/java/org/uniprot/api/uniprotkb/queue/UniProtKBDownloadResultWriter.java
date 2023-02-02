package org.uniprot.api.uniprotkb.queue;

import java.lang.reflect.Type;
import java.nio.file.*;
import java.util.Iterator;

import lombok.extern.slf4j.Slf4j;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter;
import org.uniprot.api.common.repository.stream.rdf.RDFStreamer;
import org.uniprot.api.common.repository.stream.store.BatchStoreIterable;
import org.uniprot.api.common.repository.stream.store.StoreRequest;
import org.uniprot.api.common.repository.stream.store.StoreStreamerConfig;
import org.uniprot.api.common.repository.stream.store.uniprotkb.TaxonomyLineageService;
import org.uniprot.api.common.repository.stream.store.uniprotkb.UniProtKBBatchStoreIterable;
import org.uniprot.api.rest.download.AbstractDownloadResultWriter;
import org.uniprot.api.rest.download.queue.DownloadConfigProperties;
import org.uniprot.api.rest.output.context.MessageConverterContext;
import org.uniprot.api.rest.output.context.MessageConverterContextFactory;
import org.uniprot.core.uniprotkb.UniProtKBEntry;

@Component
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
            DownloadConfigProperties downloadConfigProperties,
            TaxonomyLineageService lineageService,
            RDFStreamer uniProtRDFStreamer) {
        super(
                contentAdapter,
                converterContextFactory,
                storeStreamerConfig,
                downloadConfigProperties,
                uniProtRDFStreamer,
                MessageConverterContextFactory.Resource.UNIPROTKB);
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

    @Override
    protected String getEntityId(UniProtKBEntry entity) {
        return entity.getPrimaryAccession().getValue();
    }
}
