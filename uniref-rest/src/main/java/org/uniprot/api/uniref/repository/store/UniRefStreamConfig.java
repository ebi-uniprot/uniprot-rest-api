package org.uniprot.api.uniref.repository.store;

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
import org.uniprot.api.common.repository.search.SolrRequestConverter;
import org.uniprot.api.common.repository.solrstream.FacetTupleStreamTemplate;
import org.uniprot.api.common.repository.stream.common.TupleStreamTemplate;
import org.uniprot.api.common.repository.stream.document.TupleStreamDocumentIdStream;
import org.uniprot.api.common.repository.stream.store.StoreStreamer;
import org.uniprot.api.common.repository.stream.store.StoreStreamerConfig;
import org.uniprot.api.common.repository.stream.store.StreamerConfigProperties;
import org.uniprot.api.rest.respository.RepositoryConfig;
import org.uniprot.api.rest.respository.RepositoryConfigProperties;
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
            SolrClient solrClient,
            SolrRequestConverter requestConverter) {
        return TupleStreamTemplate.builder()
                .streamConfig(configProperties)
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

        StoreStreamerConfig<UniRefEntryLight> storeStreamerConfig =
                StoreStreamerConfig.<UniRefEntryLight>builder()
                        .streamConfig(streamConfig)
                        .storeClient(uniRefLightStoreClient)
                        .tupleStreamTemplate(tupleStreamTemplate)
                        .storeFetchRetryPolicy(storeRetryPolicy)
                        .documentIdStream(documentIdStream)
                        .build();
        return new StoreStreamer<>(storeStreamerConfig);
    }

    @Bean
    public StoreStreamerConfig<UniRefEntryLight> storeStreamerConfig(
            UniRefLightStoreClient uniRefLightStoreClient,
            TupleStreamTemplate tupleStreamTemplate,
            StreamerConfigProperties streamConfig,
            TupleStreamDocumentIdStream documentIdStream) {

        RetryPolicy<Object> storeRetryPolicy =
                new RetryPolicy<>()
                        .handle(IOException.class)
                        .withDelay(Duration.ofMillis(streamConfig.getStoreFetchRetryDelayMillis()))
                        .withMaxRetries(streamConfig.getStoreFetchMaxRetries());

        return StoreStreamerConfig.<UniRefEntryLight>builder()
                .streamConfig(streamConfig)
                .storeClient(uniRefLightStoreClient)
                .tupleStreamTemplate(tupleStreamTemplate)
                .storeFetchRetryPolicy(storeRetryPolicy)
                .documentIdStream(documentIdStream)
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
}
