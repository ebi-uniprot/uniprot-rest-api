package org.uniprot.api.async.download.refactor.consumer.streamer.batch.idmapping;

import org.springframework.stereotype.Component;
import org.uniprot.api.async.download.messaging.listener.idmapping.IdMappingHeartbeatProducer;
import org.uniprot.api.async.download.refactor.consumer.streamer.batch.IdMappingBatchResultStreamer;
import org.uniprot.api.async.download.refactor.request.idmapping.IdMappingDownloadRequest;
import org.uniprot.api.async.download.refactor.service.idmapping.IdMappingJobService;
import org.uniprot.api.common.repository.stream.store.StoreStreamerConfig;
import org.uniprot.api.common.repository.stream.store.uniprotkb.TaxonomyLineageService;
import org.uniprot.api.idmapping.common.repository.UniprotKBMappingRepository;
import org.uniprot.api.idmapping.common.response.model.IdMappingStringPair;
import org.uniprot.api.idmapping.common.response.model.UniParcEntryPair;
import org.uniprot.api.idmapping.common.response.model.UniProtKBEntryPair;
import org.uniprot.api.idmapping.common.service.store.BatchStoreEntryPairIterable;
import org.uniprot.api.idmapping.common.service.store.impl.UniParcBatchStoreEntryPairIterable;
import org.uniprot.api.idmapping.common.service.store.impl.UniProtKBBatchStoreEntryPairIterable;
import org.uniprot.core.uniparc.UniParcEntry;
import org.uniprot.core.uniprotkb.UniProtKBEntry;
import org.uniprot.store.config.UniProtDataType;
import org.uniprot.store.config.returnfield.factory.ReturnFieldConfigFactory;

import java.util.Iterator;

import static org.uniprot.api.idmapping.common.service.impl.UniProtKBIdService.isLineageAllowed;

@Component
public class UniProtKBIdMappingBatchResultStreamer extends IdMappingBatchResultStreamer<UniProtKBEntry, UniProtKBEntryPair> {
    protected final StoreStreamerConfig<UniProtKBEntry> storeStreamerConfig;
    private final TaxonomyLineageService taxonomyLineageService;
    private final UniprotKBMappingRepository uniprotKBMappingRepository;

    protected UniProtKBIdMappingBatchResultStreamer(IdMappingHeartbeatProducer heartbeatProducer, IdMappingJobService jobService, StoreStreamerConfig<UniProtKBEntry> storeStreamerConfig, TaxonomyLineageService taxonomyLineageService, UniprotKBMappingRepository uniprotKBMappingRepository) {
        super(heartbeatProducer, jobService);
        this.storeStreamerConfig = storeStreamerConfig;
        this.taxonomyLineageService = taxonomyLineageService;
        this.uniprotKBMappingRepository = uniprotKBMappingRepository;
    }

    @Override
    public BatchStoreEntryPairIterable<UniProtKBEntryPair, UniProtKBEntry>
    getBatchStoreEntryPairIterable(Iterator<IdMappingStringPair> mappedIds, IdMappingDownloadRequest request) {
        return new UniProtKBBatchStoreEntryPairIterable(
                mappedIds,
                storeStreamerConfig.getStreamConfig().getStoreBatchSize(),
                storeStreamerConfig.getStoreClient(),
                storeStreamerConfig.getStoreFetchRetryPolicy(),
                taxonomyLineageService,
                uniprotKBMappingRepository,
                isLineageAllowed(request.getFields(), ReturnFieldConfigFactory.getReturnFieldConfig(UniProtDataType.UNIPROTKB)));
    }
}
