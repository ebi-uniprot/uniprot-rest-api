package org.uniprot.api.idmapping.service.store.impl;

import static org.uniprot.api.common.repository.stream.store.uniprotkb.UniProtKBBatchStoreIterableUtil.populateLineageInEntry;

import java.util.List;
import java.util.Map;
import java.util.Set;

import net.jodah.failsafe.RetryPolicy;

import org.uniprot.api.common.repository.stream.store.uniprotkb.TaxonomyLineageService;
import org.uniprot.api.idmapping.model.IdMappingStringPair;
import org.uniprot.api.idmapping.model.UniProtKBEntryPair;
import org.uniprot.api.idmapping.service.store.BatchStoreEntryPairIterable;
import org.uniprot.core.uniprotkb.UniProtKBEntry;
import org.uniprot.store.datastore.UniProtStoreClient;

/**
 * @author lgonzales
 * @since 05/03/2021
 */
public class UniProtKBBatchStoreEntryPairIterable
        extends BatchStoreEntryPairIterable<UniProtKBEntryPair, UniProtKBEntry> {

    private final TaxonomyLineageService taxonomyLineageService;
    private final boolean addLineage;

    public UniProtKBBatchStoreEntryPairIterable(
            Iterable<IdMappingStringPair> sourceIterable,
            int batchSize,
            UniProtStoreClient<UniProtKBEntry> storeClient,
            RetryPolicy<Object> retryPolicy,
            TaxonomyLineageService taxonomyLineageService,
            boolean addLineage) {
        super(sourceIterable, batchSize, storeClient, retryPolicy);
        this.addLineage = addLineage;
        this.taxonomyLineageService = taxonomyLineageService;
    }

    @Override
    protected UniProtKBEntryPair convertToPair(
            IdMappingStringPair mId, Map<String, UniProtKBEntry> idEntryMap) {
        return UniProtKBEntryPair.builder()
                .from(mId.getFrom())
                .to(idEntryMap.get(mId.getTo()))
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
            entries = populateLineageInEntry(taxonomyLineageService, entries);
        }
        return entries;
    }
}
