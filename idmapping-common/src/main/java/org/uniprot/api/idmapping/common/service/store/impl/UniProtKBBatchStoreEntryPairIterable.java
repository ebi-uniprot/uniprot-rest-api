package org.uniprot.api.idmapping.common.service.store.impl;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import lombok.extern.slf4j.Slf4j;
import net.jodah.failsafe.RetryPolicy;

import org.uniprot.api.common.repository.stream.store.uniprotkb.TaxonomyLineageService;
import org.uniprot.api.common.repository.stream.store.uniprotkb.UniProtKBBatchStoreIterableUtil;
import org.uniprot.api.idmapping.common.repository.UniprotKBMappingRepository;
import org.uniprot.api.idmapping.common.response.model.IdMappingStringPair;
import org.uniprot.api.idmapping.common.response.model.UniProtKBEntryPair;
import org.uniprot.api.idmapping.common.service.store.BatchStoreEntryPairIterable;
import org.uniprot.core.uniprotkb.UniProtKBEntry;
import org.uniprot.store.datastore.UniProtStoreClient;

/**
 * @author lgonzales
 * @since 05/03/2021
 */
@Slf4j
public class UniProtKBBatchStoreEntryPairIterable
        extends BatchStoreEntryPairIterable<UniProtKBEntryPair, UniProtKBEntry> {

    private final TaxonomyLineageService taxonomyLineageService;
    private final UniprotKBMappingRepository uniprotKBMappingRepository;
    private final boolean addLineage;

    public UniProtKBBatchStoreEntryPairIterable(
            Iterable<IdMappingStringPair> sourceIterable,
            int batchSize,
            UniProtStoreClient<UniProtKBEntry> storeClient,
            RetryPolicy<Object> retryPolicy,
            TaxonomyLineageService taxonomyLineageService,
            UniprotKBMappingRepository uniprotKBMappingRepository,
            boolean addLineage) {
        super(sourceIterable, batchSize, storeClient, retryPolicy);
        this.addLineage = addLineage;
        this.taxonomyLineageService = taxonomyLineageService;
        this.uniprotKBMappingRepository = uniprotKBMappingRepository;
    }

    public UniProtKBBatchStoreEntryPairIterable(
            Iterator<IdMappingStringPair> sourceIterator,
            int batchSize,
            UniProtStoreClient<UniProtKBEntry> storeClient,
            RetryPolicy<Object> retryPolicy,
            TaxonomyLineageService taxonomyLineageService,
            UniprotKBMappingRepository uniprotKBMappingRepository,
            boolean addLineage) {
        super(sourceIterator, batchSize, storeClient, retryPolicy);
        this.addLineage = addLineage;
        this.taxonomyLineageService = taxonomyLineageService;
        this.uniprotKBMappingRepository = uniprotKBMappingRepository;
    }

    @Override
    protected UniProtKBEntryPair convertToPair(
            IdMappingStringPair mId, Map<String, UniProtKBEntry> idEntryMap) {
        return UniProtKBEntryPair.builder()
                .from(mId.getFrom())
                .to(
                        idEntryMap.computeIfAbsent(
                                mId.getTo(), uniprotKBMappingRepository::getDeletedEntry))
                .build();
    }

    @Override
    protected String getEntryId(UniProtKBEntry entry) {
        return entry.getPrimaryAccession().getValue();
    }

    @Override
    protected List<UniProtKBEntry> getEntriesFromStore(Set<String> tos) {
        List<UniProtKBEntry> entries = super.getEntriesFromStore(tos);
        if (addLineage) {
            entries =
                    UniProtKBBatchStoreIterableUtil.populateLineageInEntry(
                            taxonomyLineageService, entries);
        }
        return entries;
    }

    @Override
    protected void logTiming(int batchSize, long start, long end) {
        log.info(
                "Total {} UniProtKB entries fetched from voldemort in {} ms",
                batchSize,
                (end - start));
    }
}
