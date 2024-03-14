package org.uniprot.api.async.download.messaging.result.idmapping;

import static org.uniprot.api.idmapping.common.service.impl.UniProtKBIdService.isLineageAllowed;

import java.lang.reflect.Type;
import java.util.Iterator;

import lombok.extern.slf4j.Slf4j;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter;
import org.uniprot.api.async.download.messaging.config.common.DownloadConfigProperties;
import org.uniprot.api.async.download.messaging.listener.common.HeartbeatProducer;
import org.uniprot.api.common.repository.stream.rdf.RdfStreamer;
import org.uniprot.api.common.repository.stream.store.StoreStreamerConfig;
import org.uniprot.api.common.repository.stream.store.uniprotkb.TaxonomyLineageService;
import org.uniprot.api.idmapping.common.repository.UniprotKBMappingRepository;
import org.uniprot.api.idmapping.common.response.model.IdMappingStringPair;
import org.uniprot.api.idmapping.common.response.model.UniProtKBEntryPair;
import org.uniprot.api.idmapping.common.service.store.BatchStoreEntryPairIterable;
import org.uniprot.api.idmapping.common.service.store.impl.UniProtKBBatchStoreEntryPairIterable;
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
    private final UniprotKBMappingRepository uniprotKBMappingRepository;
    private final ReturnFieldConfig returnFieldConfig;
    private static final Type TYPE =
            (new ParameterizedTypeReference<MessageConverterContext<UniProtKBEntryPair>>() {})
                    .getType();

    public UniProtKBIdMappingDownloadResultWriter(
            RequestMappingHandlerAdapter contentAdapter,
            MessageConverterContextFactory<UniProtKBEntryPair> converterContextFactory,
            StoreStreamerConfig<UniProtKBEntry> storeStreamerConfig,
            DownloadConfigProperties idMappingDownloadConfigProperties,
            RdfStreamer idMappingRdfStreamer,
            TaxonomyLineageService taxonomyLineageService,
            UniprotKBMappingRepository uniprotKBMappingRepository,
            HeartbeatProducer heartBeatProducer) {
        super(
                contentAdapter,
                converterContextFactory,
                storeStreamerConfig,
                idMappingDownloadConfigProperties,
                idMappingRdfStreamer,
                MessageConverterContextFactory.Resource.UNIPROTKB,
                heartBeatProducer);
        this.taxonomyLineageService = taxonomyLineageService;
        this.uniprotKBMappingRepository = uniprotKBMappingRepository;
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
                uniprotKBMappingRepository,
                isLineageAllowed(fields, returnFieldConfig));
    }

    @Override
    public Type getType() {
        return TYPE;
    }
}
