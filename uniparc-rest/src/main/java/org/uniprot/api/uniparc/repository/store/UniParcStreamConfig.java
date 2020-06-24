package org.uniprot.api.uniparc.repository.store;

import java.io.IOException;
import java.time.Duration;

import net.jodah.failsafe.RetryPolicy;

import org.apache.http.client.HttpClient;
import org.apache.solr.client.solrj.SolrClient;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.uniprot.api.common.repository.solrstream.FacetTupleStreamTemplate;
import org.uniprot.api.common.repository.store.StoreStreamer;
import org.uniprot.api.common.repository.store.StreamerConfigProperties;
import org.uniprot.api.common.repository.store.TupleStreamTemplate;
import org.uniprot.api.rest.respository.RepositoryConfig;
import org.uniprot.api.rest.respository.RepositoryConfigProperties;
import org.uniprot.core.uniparc.UniParcEntry;
import org.uniprot.store.search.SolrCollection;

/**
 * @author lgonzales
 * @since 2020-03-04
 */
@Configuration
@Import(RepositoryConfig.class)
public class UniParcStreamConfig {

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
    public StoreStreamer<UniParcEntry> uniParcEntryStoreStreamer(
            UniParcStoreClient uniParcClient,
            TupleStreamTemplate tupleStreamTemplate,
            StreamerConfigProperties streamConfig) {

        RetryPolicy<Object> storeRetryPolicy =
                new RetryPolicy<>()
                        .handle(IOException.class)
                        .withDelay(Duration.ofMillis(streamConfig.getStoreFetchRetryDelayMillis()))
                        .withMaxRetries(streamConfig.getStoreFetchMaxRetries());

        return StoreStreamer.<UniParcEntry>builder()
                .storeClient(uniParcClient)
                .streamConfig(streamConfig)
                .tupleStreamTemplate(tupleStreamTemplate)
                .storeFetchRetryPolicy(storeRetryPolicy)
                .build();
    }

    @Bean
    @ConfigurationProperties(prefix = "streamer.uniparc")
    public StreamerConfigProperties resultsConfigProperties() {
        return new StreamerConfigProperties();
    }

    @Bean
    public FacetTupleStreamTemplate facetTupleStreamTemplate(
            RepositoryConfigProperties configProperties, HttpClient httpClient) {
        return FacetTupleStreamTemplate.builder()
                .collection(SolrCollection.uniparc.name())
                .zookeeperHost(configProperties.getZkHost())
                .httpClient(httpClient)
                .build();
    }
}
