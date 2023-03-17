package org.uniprot.api.common.repository.stream.store.uniprotkb;

import static org.uniprot.api.common.repository.stream.store.uniprotkb.UniProtKBBatchStoreIterableUtil.*;

import java.util.Iterator;
import java.util.List;

import net.jodah.failsafe.RetryPolicy;

import org.uniprot.api.common.repository.stream.store.BatchStoreIterable;
import org.uniprot.core.uniprotkb.UniProtKBEntry;
import org.uniprot.store.datastore.UniProtStoreClient;

public class UniProtKBBatchStoreIterable extends BatchStoreIterable<UniProtKBEntry> {

    private final TaxonomyLineageService taxonomyLineageService;
    private final boolean addLineage;

    public UniProtKBBatchStoreIterable(
            Iterable<String> sourceIterable,
            UniProtStoreClient<UniProtKBEntry> storeClient,
            RetryPolicy<Object> retryPolicy,
            int batchSize,
            TaxonomyLineageService taxonomyLineageService,
            boolean addLineage) {
        super(sourceIterable, storeClient, retryPolicy, batchSize);
        this.taxonomyLineageService = taxonomyLineageService;
        this.addLineage = addLineage;
    }

    public UniProtKBBatchStoreIterable(
            Iterator<String> sourceIterator,
            UniProtStoreClient<UniProtKBEntry> storeClient,
            RetryPolicy<Object> retryPolicy,
            int batchSize,
            TaxonomyLineageService taxonomyLineageService,
            boolean addLineage) {
        super(sourceIterator, storeClient, retryPolicy, batchSize);
        this.taxonomyLineageService = taxonomyLineageService;
        this.addLineage = addLineage;
    }

    @Override
    protected List<UniProtKBEntry> convertBatch(List<String> batch) {
        List<UniProtKBEntry> entries = super.convertBatch(batch);
        if (addLineage) {
            entries = populateLineageInEntry(taxonomyLineageService, entries);
        }
        return entries;
    }
}
