package org.uniprot.api.uniref.repository.store;

import java.io.IOException;
import java.time.Duration;

import net.jodah.failsafe.RetryPolicy;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.uniprot.api.common.repository.store.StoreStreamer;
import org.uniprot.api.common.repository.store.StreamerConfigProperties;
import org.uniprot.api.rest.respository.RepositoryConfig;
import org.uniprot.api.uniref.repository.UniRefQueryRepository;
import org.uniprot.core.uniref.UniRefEntry;
import org.uniprot.store.search.document.uniref.UniRefDocument;

/** @author jluo date: 21 Aug 2019 */
@Configuration
@Import(RepositoryConfig.class)
public class UniRefStreamConfig {

    @Bean
    public StoreStreamer<UniRefDocument, UniRefEntry> unirefEntryStoreStreamer(
            UniRefStoreClient unirefClient, UniRefQueryRepository uniRefQueryRepository) {

        RetryPolicy<Object> storeRetryPolicy =
                new RetryPolicy<>()
                        .handle(IOException.class)
                        .withDelay(
                                Duration.ofMillis(
                                        resultsConfigProperties().getStoreFetchRetryDelayMillis()))
                        .withMaxRetries(resultsConfigProperties().getStoreFetchMaxRetries());

        return StoreStreamer.<UniRefDocument, UniRefEntry>builder()
                .storeBatchSize(resultsConfigProperties().getStoreBatchSize())
                .storeClient(unirefClient)
                .documentToId(UniRefDocument::getId)
                .repository(uniRefQueryRepository)
                .storeFetchRetryPolicy(storeRetryPolicy)
                .build();
    }

    @Bean
    @ConfigurationProperties(prefix = "streamer.uniref")
    public StreamerConfigProperties resultsConfigProperties() {
        return new StreamerConfigProperties();
    }
}
