package org.uniprot.api.uniparc.repository.store;

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
import org.uniprot.api.uniparc.repository.UniParcQueryRepository;
import org.uniprot.core.uniparc.UniParcEntry;
import org.uniprot.store.search.document.uniparc.UniParcDocument;

/**
 * @author lgonzales
 * @since 2020-03-04
 */
@Configuration
@Import(RepositoryConfig.class)
public class UniParcStreamConfig {

    @Bean
    public StoreStreamer<UniParcDocument, UniParcEntry> uniParcEntryStoreStreamer(
            UniParcStoreClient uniParcClient, UniParcQueryRepository uniParcQueryRepository) {

        RetryPolicy<Object> storeRetryPolicy =
                new RetryPolicy<>()
                        .handle(IOException.class)
                        .withDelay(
                                Duration.ofMillis(
                                        resultsConfigProperties().getStoreFetchRetryDelayMillis()))
                        .withMaxRetries(resultsConfigProperties().getStoreFetchMaxRetries());

        return StoreStreamer.<UniParcDocument, UniParcEntry>builder()
                .storeClient(uniParcClient)
                .storeBatchSize(resultsConfigProperties().getStoreBatchSize())
                .documentToId(UniParcDocument::getUpi)
                .repository(uniParcQueryRepository)
                .storeFetchRetryPolicy(storeRetryPolicy)
                .build();
    }

    @Bean
    @ConfigurationProperties(prefix = "streamer.uniparc")
    public StreamerConfigProperties resultsConfigProperties() {
        return new StreamerConfigProperties();
    }
}
