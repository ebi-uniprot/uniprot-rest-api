package org.uniprot.api.idmapping.queue;

import static org.uniprot.api.idmapping.service.impl.UniProtKBIdService.*;

import java.lang.reflect.Type;
import java.util.Iterator;

import lombok.extern.slf4j.Slf4j;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter;
import org.uniprot.api.common.repository.stream.rdf.RdfStreamer;
import org.uniprot.api.common.repository.stream.store.StoreStreamerConfig;
import org.uniprot.api.common.repository.stream.store.uniprotkb.TaxonomyLineageService;
import org.uniprot.api.idmapping.model.IdMappingStringPair;
import org.uniprot.api.idmapping.model.UniProtKBEntryPair;
import org.uniprot.api.idmapping.service.store.BatchStoreEntryPairIterable;
import org.uniprot.api.idmapping.service.store.impl.UniProtKBBatchStoreEntryPairIterable;
import org.uniprot.api.rest.download.queue.DownloadConfigProperties;
import org.uniprot.api.rest.output.context.MessageConverterContext;
import org.uniprot.api.rest.output.context.MessageConverterContextFactory;
import org.uniprot.core.uniprotkb.UniProtKBEntry;
import org.uniprot.store.config.UniProtDataType;
import org.uniprot.store.config.returnfield.config.ReturnFieldConfig;
import org.uniprot.store.config.returnfield.factory.ReturnFieldConfigFactory;

@Component
@Slf4j
public class UniProtKBIdMappingDownloadResultWriter
        extends AbstractIdMappingDownloadResultWriter<UniProtKBEntryPair, UniProtKBEntry> {

    private final TaxonomyLineageService taxonomyLineageService;
    private final ReturnFieldConfig returnFieldConfig;
    private static final Type type =
            (new ParameterizedTypeReference<MessageConverterContext<UniProtKBEntryPair>>() {})
                    .getType();

    public UniProtKBIdMappingDownloadResultWriter(
            RequestMappingHandlerAdapter contentAdapter,
            MessageConverterContextFactory<UniProtKBEntryPair> converterContextFactory,
            StoreStreamerConfig<UniProtKBEntry> storeStreamerConfig,
            DownloadConfigProperties downloadConfigProperties,
            RdfStreamer uniProtKBRDFStreamer,
            TaxonomyLineageService taxonomyLineageService) {
        super(
                contentAdapter,
                converterContextFactory,
                storeStreamerConfig,
                downloadConfigProperties,
                uniProtKBRDFStreamer,
                MessageConverterContextFactory.Resource.UNIPROTKB);
        this.taxonomyLineageService = taxonomyLineageService;
        this.returnFieldConfig =
                ReturnFieldConfigFactory.getReturnFieldConfig(UniProtDataType.UNIPROTKB);
    }

    @Override
    public BatchStoreEntryPairIterable<UniProtKBEntryPair, UniProtKBEntry>
            getBatchStoreEntryPairIterable(Iterator<IdMappingStringPair> mappedIds, String fields) {
        return new UniProtKBBatchStoreEntryPairIterable(
                mappedIds,
                storeStreamerConfig.getStreamConfig().getStoreBatchSize(),
                storeStreamerConfig.getStoreClient(),
                storeStreamerConfig.getStoreFetchRetryPolicy(),
                taxonomyLineageService,
                isLineageAllowed(fields, returnFieldConfig));
    }

    @Override
    public Type getType() {
        return type;
    }
}