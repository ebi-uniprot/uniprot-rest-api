package org.uniprot.api.common.repository.stream.store.uniref;

import java.util.Iterator;

import net.jodah.failsafe.RetryPolicy;

import org.uniprot.api.common.repository.stream.store.BatchStoreIterable;
import org.uniprot.core.uniref.UniRefEntryLight;
import org.uniprot.store.datastore.UniProtStoreClient;

public class UniRefBatchStoreIterable extends BatchStoreIterable<UniRefEntryLight> {

    public UniRefBatchStoreIterable(
            Iterator<String> sourceIterator,
            UniProtStoreClient<UniRefEntryLight> storeClient,
            RetryPolicy<Object> retryPolicy,
            int batchSize) {
        super(sourceIterator, storeClient, retryPolicy, batchSize);
    }
}
