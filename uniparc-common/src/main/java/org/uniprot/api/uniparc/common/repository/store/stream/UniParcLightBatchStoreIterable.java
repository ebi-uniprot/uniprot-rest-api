package org.uniprot.api.uniparc.common.repository.store.stream;

import java.util.Iterator;
import java.util.List;

import org.uniprot.api.common.repository.stream.store.BatchStoreIterable;
import org.uniprot.api.uniparc.common.repository.store.crossref.UniParcCrossReferenceLazyLoader;
import org.uniprot.core.uniparc.UniParcEntryLight;
import org.uniprot.core.util.Utils;
import org.uniprot.store.datastore.UniProtStoreClient;

import net.jodah.failsafe.RetryPolicy;

public class UniParcLightBatchStoreIterable extends BatchStoreIterable<UniParcEntryLight> {

    private final UniParcCrossReferenceLazyLoader lazyLoader;

    private final String fields;

    public UniParcLightBatchStoreIterable(
            Iterable<String> sourceIterable,
            UniProtStoreClient<UniParcEntryLight> storeClient,
            RetryPolicy<Object> retryPolicy,
            int batchSize,
            UniParcCrossReferenceLazyLoader lazyLoader,
            String fields) {
        super(sourceIterable, storeClient, retryPolicy, batchSize);
        this.lazyLoader = lazyLoader;
        this.fields = fields;
    }

    public UniParcLightBatchStoreIterable(
            Iterator<String> sourceIterator,
            UniProtStoreClient<UniParcEntryLight> storeClient,
            RetryPolicy<Object> retryPolicy,
            int batchSize,
            UniParcCrossReferenceLazyLoader lazyLoader,
            String fields) {
        super(sourceIterator, storeClient, retryPolicy, batchSize);
        this.lazyLoader = lazyLoader;
        this.fields = fields;
    }

    @Override
    protected List<UniParcEntryLight> convertBatch(List<String> batch) {
        List<UniParcEntryLight> entries = super.convertBatch(batch);
        List<String> lazyFields = lazyLoader.getLazyFields(fields);
        if (Utils.notNullNotEmpty(lazyFields)) {
            entries = lazyLoader.populateLazyFields(entries, lazyFields);
        }
        return entries;
    }
}
