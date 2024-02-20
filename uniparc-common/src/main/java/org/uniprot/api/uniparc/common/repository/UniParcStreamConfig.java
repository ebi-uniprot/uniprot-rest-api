package org.uniprot.api.uniparc.common.repository;

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
import org.uniprot.api.uniparc.common.repository.store.UniParcStoreClient;
import org.uniprot.core.uniparc.UniParcEntry;
import org.uniprot.store.search.SolrCollection;

/**
 * @author lgonzales
 * @since 2020-03-04
 */
@Configuration
@Import(RepositoryConfig.class)
@Slf4j
public class UniParcStreamConfig {

    @Bean("uniParcTupleStreamTemplate")
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
    public StoreStreamer<UniParcEntry> uniParcEntryStoreStreamer(
            UniParcStoreClient uniParcClient,
            TupleStreamTemplate tupleStreamTemplate,
            StreamerConfigProperties streamConfig,
            TupleStreamDocumentIdStream documentIdStream) {

        RetryPolicy<Object> storeRetryPolicy =
                new RetryPolicy<>()
                        .handle(IOException.class)
                        .withDelay(Duration.ofMillis(streamConfig.getStoreFetchRetryDelayMillis()))
                        .withMaxRetries(streamConfig.getStoreFetchMaxRetries());

        StoreStreamerConfig<UniParcEntry> storeStreamerConfig =
                StoreStreamerConfig.<UniParcEntry>builder()
                        .storeClient(uniParcClient)
                        .streamConfig(streamConfig)
                        .tupleStreamTemplate(tupleStreamTemplate)
                        .storeFetchRetryPolicy(storeRetryPolicy)
                        .documentIdStream(documentIdStream)
                        .build();
        return new StoreStreamer<>(storeStreamerConfig);
    }

    @Bean("uniParcStreamerConfigProperties")
    @ConfigurationProperties(prefix = "streamer.uniparc")
    public StreamerConfigProperties resultsConfigProperties() {
        return new StreamerConfigProperties();
    }

    @Bean("uniParcFacetTupleStreamTemplate")
    public FacetTupleStreamTemplate facetTupleStreamTemplate(
            RepositoryConfigProperties configProperties, HttpClient httpClient) {
        return FacetTupleStreamTemplate.builder()
                .collection(SolrCollection.uniparc.name())
                .zookeeperHost(configProperties.getZkHost())
                .build();
    }

    @Bean("uniParcTupleStreamDocumentIdStream")
    public TupleStreamDocumentIdStream documentIdStream(
            TupleStreamTemplate tupleStreamTemplate, StreamerConfigProperties streamConfig) {
        return TupleStreamDocumentIdStream.builder()
                .tupleStreamTemplate(tupleStreamTemplate)
                .streamConfig(streamConfig)
                .build();
    }
}
