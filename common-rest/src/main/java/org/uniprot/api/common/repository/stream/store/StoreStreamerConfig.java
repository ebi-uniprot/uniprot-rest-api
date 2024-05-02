package org.uniprot.api.common.repository.stream.store;

import org.uniprot.api.common.repository.stream.common.TupleStreamTemplate;
import org.uniprot.api.common.repository.stream.document.DocumentIdStream;
import org.uniprot.store.datastore.UniProtStoreClient;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import net.jodah.failsafe.RetryPolicy;

@Builder
@Getter
@AllArgsConstructor
public class StoreStreamerConfig<T> {

    private final UniProtStoreClient<T> storeClient;
    private final TupleStreamTemplate tupleStreamTemplate;
    private final StreamerConfigProperties streamConfig;
    private final RetryPolicy<Object> storeFetchRetryPolicy;
    private final DocumentIdStream documentIdStream;
}
