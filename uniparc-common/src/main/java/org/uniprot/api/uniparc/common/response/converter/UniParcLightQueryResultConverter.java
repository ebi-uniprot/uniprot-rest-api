package org.uniprot.api.uniparc.common.response.converter;

import java.io.IOException;
import java.time.Duration;
import java.util.function.Function;

import org.springframework.stereotype.Component;
import org.uniprot.api.uniparc.common.repository.store.light.UniParcLightStoreClient;
import org.uniprot.core.uniparc.UniParcEntryLight;
import org.uniprot.store.search.document.uniparc.UniParcDocument;

import net.jodah.failsafe.Failsafe;
import net.jodah.failsafe.RetryPolicy;

@Component
public class UniParcLightQueryResultConverter
        implements Function<UniParcDocument, UniParcEntryLight> {
    private final UniParcLightStoreClient entryStore;
    private final RetryPolicy<Object> retryPolicy =
            new RetryPolicy<>()
                    .handle(IOException.class)
                    .withDelay(Duration.ofMillis(100))
                    .withMaxRetries(5);

    public UniParcLightQueryResultConverter(UniParcLightStoreClient entryStore) {
        this.entryStore = entryStore;
    }

    @Override
    public UniParcEntryLight apply(UniParcDocument doc) {
        return Failsafe.with(retryPolicy).get(() -> entryStore.getEntry(doc.getUpi()).orElse(null));
    }
}
