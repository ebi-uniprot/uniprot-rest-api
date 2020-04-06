package org.uniprot.api.uniref.service;

import java.io.IOException;
import java.time.Duration;
import java.util.function.Function;

import net.jodah.failsafe.Failsafe;
import net.jodah.failsafe.RetryPolicy;

import org.springframework.stereotype.Component;
import org.uniprot.api.uniref.repository.store.UniRefStoreClient;
import org.uniprot.core.uniref.UniRefEntry;
import org.uniprot.store.search.document.uniref.UniRefDocument;

/**
 * @author jluo
 * @date: 20 Aug 2019
 */
@Component
public class UniRefQueryResultConverter implements Function<UniRefDocument, UniRefEntry> {

    private final UniRefStoreClient entryStore;
    private final RetryPolicy<Object> retryPolicy =
            new RetryPolicy<>()
                    .handle(IOException.class)
                    .withDelay(Duration.ofMillis(100))
                    .withMaxRetries(5);

    public UniRefQueryResultConverter(UniRefStoreClient entryStore) {
        this.entryStore = entryStore;
    }

    @Override
    public UniRefEntry apply(UniRefDocument doc) {
        return Failsafe.with(retryPolicy).get(() -> entryStore.getEntry(doc.getId()).orElse(null));
    }
}
