package org.uniprot.api.uniref.repository.store;

import java.io.IOException;
import java.time.Duration;
import java.time.temporal.ChronoUnit;

import lombok.extern.slf4j.Slf4j;
import net.jodah.failsafe.RetryPolicy;

import org.apache.http.client.HttpClient;
import org.apache.solr.client.solrj.SolrClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;
import org.uniprot.api.common.repository.search.SolrRequestConverter;
import org.uniprot.api.common.repository.solrstream.FacetTupleStreamTemplate;
import org.uniprot.api.common.repository.stream.common.TupleStreamTemplate;
import org.uniprot.api.common.repository.stream.document.TupleStreamDocumentIdStream;
import org.uniprot.api.common.repository.stream.rdf.RDFStreamer;
import org.uniprot.api.common.repository.stream.rdf.RDFStreamerConfigProperties;
import org.uniprot.api.common.repository.stream.store.StoreStreamer;
import org.uniprot.api.common.repository.stream.store.StreamerConfigProperties;
import org.uniprot.api.rest.respository.RepositoryConfig;
import org.uniprot.api.rest.respository.RepositoryConfigProperties;
import org.uniprot.api.rest.service.RDFPrologs;
import org.uniprot.api.rest.service.RDFService;
import org.uniprot.core.uniref.UniRefEntryLight;
import org.uniprot.store.search.SolrCollection;

/** @author jluo date: 21 Aug 2019 */
@Configuration
@Import(RepositoryConfig.class)
@Slf4j
public class UniRefStreamConfig {

    @Bean
    public TupleStreamTemplate tupleStreamTemplate(
            StreamerConfigProperties configProperties,
            HttpClient httpClient,
            @Qualifier("nonKBSolrClient") SolrClient solrClient,
            SolrRequestConverter requestConverter) {
        return TupleStreamTemplate.builder()
                .streamConfig(configProperties)
                .httpClient(httpClient)
                .solrClient(solrClient)
                .solrRequestConverter(requestConverter)
                .build();
    }

    @Bean
    public StoreStreamer<UniRefEntryLight> unirefEntryStoreStreamer(
            UniRefLightStoreClient uniRefLightStoreClient,
            TupleStreamTemplate tupleStreamTemplate,
            StreamerConfigProperties streamConfig,
            TupleStreamDocumentIdStream documentIdStream) {

        RetryPolicy<Object> storeRetryPolicy =
                new RetryPolicy<>()
                        .handle(IOException.class)
                        .withDelay(Duration.ofMillis(streamConfig.getStoreFetchRetryDelayMillis()))
                        .withMaxRetries(streamConfig.getStoreFetchMaxRetries());

        return StoreStreamer.<UniRefEntryLight>builder()
                .streamConfig(streamConfig)
                .storeClient(uniRefLightStoreClient)
                .tupleStreamTemplate(tupleStreamTemplate)
                .storeFetchRetryPolicy(storeRetryPolicy)
                .documentIdStream(documentIdStream)
                .build();
    }

    @Bean
    public RDFStreamer uniRefRDFStreamer(
            @Qualifier("rdfRestTemplate") RestTemplate restTemplate,
            TupleStreamDocumentIdStream documentIdStream) {

        int rdfRetryDelay = rdfConfigProperties().getRetryDelayMillis();
        int maxRdfRetryDelay = rdfRetryDelay * 8;
        RetryPolicy<Object> rdfRetryPolicy =
                new RetryPolicy<>()
                        .handle(ResourceAccessException.class)
                        .withBackoff(rdfRetryDelay, maxRdfRetryDelay, ChronoUnit.MILLIS)
                        .withMaxRetries(rdfConfigProperties().getMaxRetries())
                        .onRetry(
                                e ->
                                        log.warn(
                                                "Call to RDF server failed. Failure #{}. Retrying...",
                                                e.getAttemptCount()));

        return RDFStreamer.builder()
                .rdfBatchSize(rdfConfigProperties().getBatchSize())
                .rdfFetchRetryPolicy(rdfRetryPolicy)
                .rdfService(new RDFService<>(restTemplate, String.class))
                .rdfProlog(RDFPrologs.UNIREF_RDF_PROLOG)
                .idStream(documentIdStream)
                .build();
    }

    @Bean
    @ConfigurationProperties(prefix = "streamer.uniref")
    public StreamerConfigProperties resultsConfigProperties() {
        return new StreamerConfigProperties();
    }

    @Bean
    public FacetTupleStreamTemplate facetTupleStreamTemplate(
            RepositoryConfigProperties configProperties, HttpClient httpClient) {
        return FacetTupleStreamTemplate.builder()
                .collection(SolrCollection.uniref.name())
                .zookeeperHost(configProperties.getZkHost())
                .httpClient(httpClient)
                .build();
    }

    @Bean
    @ConfigurationProperties(prefix = "streamer.rdf")
    public RDFStreamerConfigProperties rdfConfigProperties() {
        return new RDFStreamerConfigProperties();
    }

    @Bean
    public TupleStreamDocumentIdStream documentIdStream(
            TupleStreamTemplate tupleStreamTemplate, StreamerConfigProperties streamConfig) {
        return TupleStreamDocumentIdStream.builder()
                .tupleStreamTemplate(tupleStreamTemplate)
                .streamConfig(streamConfig)
                .build();
    }
}
