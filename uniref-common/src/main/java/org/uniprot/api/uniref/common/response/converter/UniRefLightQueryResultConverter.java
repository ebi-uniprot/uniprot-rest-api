package org.uniprot.api.uniref.common.response.converter;

import java.io.IOException;
import java.time.Duration;
import java.util.function.Function;

import net.jodah.failsafe.Failsafe;
import net.jodah.failsafe.RetryPolicy;

import org.springframework.stereotype.Component;
import org.uniprot.api.uniref.common.repository.store.UniRefLightStoreClient;
import org.uniprot.core.uniref.UniRefEntryLight;
import org.uniprot.store.search.document.uniref.UniRefDocument;

/**
 * @author jluo
 * @date: 20 Aug 2019
 */
@Component
public class UniRefLightQueryResultConverter implements Function<UniRefDocument, UniRefEntryLight> {

    private final UniRefLightStoreClient entryStore;
    private final RetryPolicy<Object> retryPolicy =
            new RetryPolicy<>()
                    .handle(IOException.class)
                    .withDelay(Duration.ofMillis(100))
                    .withMaxRetries(5);

    public UniRefLightQueryResultConverter(UniRefLightStoreClient entryStore) {
        this.entryStore = entryStore;
    }

    @Override
    public UniRefEntryLight apply(UniRefDocument doc) {
        return Failsafe.with(retryPolicy).get(() -> entryStore.getEntry(doc.getId()).orElse(null));
    }
}
