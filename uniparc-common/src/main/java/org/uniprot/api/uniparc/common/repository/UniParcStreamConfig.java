package org.uniprot.api.uniparc.common.repository;

import java.io.IOException;
import java.time.Duration;

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

import lombok.extern.slf4j.Slf4j;
import net.jodah.failsafe.RetryPolicy;

/**
 * @author lgonzales
 * @since 2020-03-04
 */
@Configuration
@Import(RepositoryConfig.class)
@Slf4j
public class UniParcStreamConfig {

    @Bean
    public TupleStreamTemplate uniParcTupleStreamTemplate(
            StreamerConfigProperties uniParcStreamerConfigProperties,
            SolrClient solrClient,
            SolrRequestConverter requestConverter) {
        return TupleStreamTemplate.builder()
                .streamConfig(uniParcStreamerConfigProperties)
                .solrClient(solrClient)
                .solrRequestConverter(requestConverter)
                .build();
    }

    @Bean
    public StoreStreamer<UniParcEntry> uniParcEntryStoreStreamer(
            StoreStreamerConfig<UniParcEntry> storeStreamerConfig) {
        return new StoreStreamer<>(storeStreamerConfig);
    }

    @Bean
    public StoreStreamerConfig<UniParcEntry> storeStreamerConfig(
            UniParcStoreClient uniParcClient,
            TupleStreamTemplate uniParcTupleStreamTemplate,
            StreamerConfigProperties uniParcStreamerConfigProperties,
            TupleStreamDocumentIdStream uniParcTupleStreamDocumentIdStream) {
        RetryPolicy<Object> storeRetryPolicy =
                new RetryPolicy<>()
                        .handle(IOException.class)
                        .withDelay(
                                Duration.ofMillis(
                                        uniParcStreamerConfigProperties
                                                .getStoreFetchRetryDelayMillis()))
                        .withMaxRetries(uniParcStreamerConfigProperties.getStoreFetchMaxRetries());
        return StoreStreamerConfig.<UniParcEntry>builder()
                .storeClient(uniParcClient)
                .streamConfig(uniParcStreamerConfigProperties)
                .tupleStreamTemplate(uniParcTupleStreamTemplate)
                .storeFetchRetryPolicy(storeRetryPolicy)
                .documentIdStream(uniParcTupleStreamDocumentIdStream)
                .build();
    }

    @Bean
    @ConfigurationProperties(prefix = "streamer.uniparc")
    public StreamerConfigProperties uniParcStreamerConfigProperties() {
        return new StreamerConfigProperties();
    }

    @Bean
    public FacetTupleStreamTemplate uniParcFacetTupleStreamTemplate(
            RepositoryConfigProperties configProperties, HttpClient httpClient) {
        return FacetTupleStreamTemplate.builder()
                .collection(SolrCollection.uniparc.name())
                .zookeeperHost(configProperties.getZkHost())
                .build();
    }

    @Bean
    public TupleStreamDocumentIdStream uniParcTupleStreamDocumentIdStream(
            TupleStreamTemplate uniParcTupleStreamTemplate,
            StreamerConfigProperties uniParcStreamerConfigProperties) {
        return TupleStreamDocumentIdStream.builder()
                .tupleStreamTemplate(uniParcTupleStreamTemplate)
                .streamConfig(uniParcStreamerConfigProperties)
                .build();
    }
}
