package org.uniprot.api.uniprotkb.repository.store;

import java.io.IOException;
import java.time.Duration;
import java.time.temporal.ChronoUnit;

import lombok.extern.slf4j.Slf4j;
import net.jodah.failsafe.RetryPolicy;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.web.client.RestTemplate;
import org.uniprot.api.common.repository.store.RDFStreamerConfigProperties;
import org.uniprot.api.common.repository.store.StoreStreamer;
import org.uniprot.api.common.repository.store.StreamerConfigProperties;
import org.uniprot.api.rest.respository.RepositoryConfig;
import org.uniprot.api.rest.service.RDFService;
import org.uniprot.api.uniprotkb.repository.search.impl.UniprotQueryRepository;
import org.uniprot.core.uniprotkb.UniProtkbEntry;
import org.uniprot.store.search.document.uniprot.UniProtDocument;

/**
 * Created 21/08/18
 *
 * @author Edd
 */
@Configuration
@Import(RepositoryConfig.class)
@Slf4j
public class ResultsConfig {
    @Bean
    public StoreStreamer<UniProtDocument, UniProtkbEntry> uniProtEntryStoreStreamer(
            UniProtKBStoreClient uniProtClient,
            UniprotQueryRepository uniprotQueryRepository,
            @Qualifier("rdfRestTemplate") RestTemplate restTemplate) {

        RetryPolicy<Object> storeRetryPolicy =
                new RetryPolicy<>()
                        .handle(IOException.class)
                        .withDelay(
                                Duration.ofMillis(
                                        resultsConfigProperties().getStoreFetchRetryDelayMillis()))
                        .withMaxRetries(resultsConfigProperties().getStoreFetchMaxRetries());

        int rdfRetryDelay = rdfConfigProperties().getRetryDelayMillis();
        int maxRdfRetryDelay = rdfRetryDelay * 8;
        RetryPolicy<Object> rdfRetryPolicy =
                new RetryPolicy<>()
                        .handle(IOException.class)
                        .withBackoff(rdfRetryDelay, maxRdfRetryDelay, ChronoUnit.MILLIS)
                        .withMaxRetries(rdfConfigProperties().getMaxRetries())
                        .onRetry(
                                e ->
                                        log.warn(
                                                "Call to RDF server failed. Failure #{}. Retrying...",
                                                e.getAttemptCount()));

        return StoreStreamer.<UniProtDocument, UniProtkbEntry>builder()
                .storeBatchSize(resultsConfigProperties().getStoreBatchSize())
                .rdfBatchSize(rdfConfigProperties().getBatchSize())
                .storeClient(uniProtClient)
                .documentToId(doc -> doc.accession)
                .repository(uniprotQueryRepository)
                .storeFetchRetryPolicy(storeRetryPolicy)
                .rdfFetchRetryPolicy(rdfRetryPolicy)
                .rdfStoreClient(new RDFService<>(restTemplate, String.class))
                .build();
    }

    @Bean
    @ConfigurationProperties(prefix = "streamer.uniprot")
    public StreamerConfigProperties resultsConfigProperties() {
        return new StreamerConfigProperties();
    }

    @Bean
    @ConfigurationProperties(prefix = "streamer.rdf")
    public RDFStreamerConfigProperties rdfConfigProperties() {
        return new RDFStreamerConfigProperties();
    }
}
