package org.uniprot.api.idmapping.service;

import java.io.IOException;
import java.time.Duration;

import lombok.extern.slf4j.Slf4j;
import net.jodah.failsafe.RetryPolicy;

import org.apache.http.client.HttpClient;
import org.apache.solr.client.solrj.SolrClient;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Profile;
import org.uniprot.api.common.repository.search.SolrRequestConverter;
import org.uniprot.api.common.repository.solrstream.FacetTupleStreamTemplate;
import org.uniprot.api.common.repository.stream.common.TupleStreamTemplate;
import org.uniprot.api.common.repository.stream.document.TupleStreamDocumentIdStream;
import org.uniprot.api.common.repository.stream.store.StoreStreamer;
import org.uniprot.api.common.repository.stream.store.StreamerConfigProperties;
import org.uniprot.api.rest.respository.RepositoryConfig;
import org.uniprot.api.rest.respository.RepositoryConfigProperties;
import org.uniprot.core.uniprotkb.UniProtKBEntry;
import org.uniprot.store.datastore.UniProtStoreClient;
import org.uniprot.store.datastore.voldemort.VoldemortClient;
import org.uniprot.store.datastore.voldemort.uniprot.VoldemortRemoteUniProtKBEntryStore;
import org.uniprot.store.search.SolrCollection;

/**
 * @author sahmad
 * @created 18/02/2021
 */
@Configuration
@Import(RepositoryConfig.class)
@Slf4j
public class IdMappingResultsConfig {

    @Bean
    @ConfigurationProperties(prefix = "id.mapping.streamer.uniprot")
    public StreamerConfigProperties streamerConfigProperties() {
        return new StreamerConfigProperties();
    }

    @Bean
    public TupleStreamTemplate tupleStreamTemplate(
            StreamerConfigProperties configProperties,
            HttpClient httpClient,
            SolrClient solrClient,
            SolrRequestConverter requestConverter) {
        return TupleStreamTemplate.builder()
                .streamConfig(configProperties)
                .httpClient(httpClient)
                .solrClient(solrClient)
                .solrRequestConverter(requestConverter)
                .build();
    }

    @Bean
    public StoreStreamer<UniProtKBEntry> uniProtKBEntryStoreStreamer(
            UniProtStoreClient<UniProtKBEntry> storeClient,
            TupleStreamTemplate tupleStreamTemplate,
            StreamerConfigProperties streamConfig,
            TupleStreamDocumentIdStream documentIdStream) {

        RetryPolicy<Object> storeRetryPolicy =
                new RetryPolicy<>()
                        .handle(IOException.class)
                        .withDelay(Duration.ofMillis(streamConfig.getStoreFetchRetryDelayMillis()))
                        .withMaxRetries(streamConfig.getStoreFetchMaxRetries());

        return StoreStreamer.<UniProtKBEntry>builder()
                .streamConfig(streamConfig)
                .storeClient(storeClient)
                .tupleStreamTemplate(tupleStreamTemplate)
                .storeFetchRetryPolicy(storeRetryPolicy)
                .documentIdStream(documentIdStream)
                .build();
    }

    @Bean
    public TupleStreamDocumentIdStream documentIdStream(
            TupleStreamTemplate tupleStreamTemplate, StreamerConfigProperties streamConfig) {
        return TupleStreamDocumentIdStream.builder()
                .tupleStreamTemplate(tupleStreamTemplate)
                .streamConfig(streamConfig)
                .build();
    }

    @Bean
    public FacetTupleStreamTemplate facetTupleStreamTemplate(
            RepositoryConfigProperties configProperties, HttpClient httpClient) {
        return FacetTupleStreamTemplate.builder()
                .collection(SolrCollection.uniprot.name())
                .zookeeperHost(configProperties.getZkHost())
                .httpClient(httpClient)
                .build();
    }

    @Bean
    @Profile("live")
    public UniProtStoreClient<UniProtKBEntry> uniProtStoreClient(
            UniProtStoreConfigProperties uniProtStoreConfigProperties) {
        VoldemortClient<UniProtKBEntry> client =
                new VoldemortRemoteUniProtKBEntryStore(
                        uniProtStoreConfigProperties.getNumberOfConnections(),
                        uniProtStoreConfigProperties.getStoreName(),
                        uniProtStoreConfigProperties.getHost());
        return new UniProtStoreClient<>(client);
    }

    @Bean
    public UniProtStoreConfigProperties storeConfigProperties() {
        return new UniProtStoreConfigProperties();
    }
}
