package org.uniprot.api.uniparc.service;

import java.io.IOException;
import java.time.Duration;
import java.util.function.Function;

import net.jodah.failsafe.Failsafe;
import net.jodah.failsafe.RetryPolicy;

import org.springframework.stereotype.Component;
import org.uniprot.api.uniparc.repository.store.UniParcStoreClient;
import org.uniprot.core.uniparc.UniParcEntry;
import org.uniprot.store.search.document.uniparc.UniParcDocument;

/**
 * @author lgonzales
 * @since 2020-03-05
 */
@Component
public class UniParcQueryResultConverter implements Function<UniParcDocument, UniParcEntry> {
    private final UniParcStoreClient entryStore;
    private final RetryPolicy<Object> retryPolicy =
            new RetryPolicy<>()
                    .handle(IOException.class)
                    .withDelay(Duration.ofMillis(100))
                    .withMaxRetries(5);

    public UniParcQueryResultConverter(UniParcStoreClient entryStore) {
        this.entryStore = entryStore;
    }

    @Override
    public UniParcEntry apply(UniParcDocument doc) {
        return Failsafe.with(retryPolicy).get(() -> entryStore.getEntry(doc.getUpi()).orElse(null));
    }
}
