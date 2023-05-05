package org.uniprot.api.idmapping.service.config;

import java.io.IOException;
import java.time.Duration;

import net.jodah.failsafe.RetryPolicy;

import org.apache.http.client.HttpClient;
import org.apache.solr.client.solrj.SolrClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Profile;
import org.uniprot.api.common.repository.search.SolrRequestConverter;
import org.uniprot.api.common.repository.solrstream.FacetTupleStreamTemplate;
import org.uniprot.api.common.repository.stream.common.TupleStreamTemplate;
import org.uniprot.api.common.repository.stream.document.TupleStreamDocumentIdStream;
import org.uniprot.api.common.repository.stream.store.StoreConfigProperties;
import org.uniprot.api.common.repository.stream.store.StoreStreamer;
import org.uniprot.api.common.repository.stream.store.StoreStreamerConfig;
import org.uniprot.api.common.repository.stream.store.StreamerConfigProperties;
import org.uniprot.api.rest.respository.RepositoryConfig;
import org.uniprot.api.rest.respository.RepositoryConfigProperties;
import org.uniprot.core.uniparc.UniParcEntry;
import org.uniprot.store.datastore.UniProtStoreClient;
import org.uniprot.store.datastore.voldemort.VoldemortClient;
import org.uniprot.store.datastore.voldemort.uniparc.VoldemortRemoteUniParcEntryStore;
import org.uniprot.store.search.SolrCollection;

/**
 * @author lgonzales
 * @since 25/02/2021
 */
@Configuration
@Import(RepositoryConfig.class)
public class UniParcIdMappingResultsConfig {

    @Bean("uniParcStreamerConfigProperties")
    @ConfigurationProperties(prefix = "id.mapping.streamer.uniparc")
    public StreamerConfigProperties uniParcStreamerConfigProperties() {
        return new StreamerConfigProperties();
    }

    @Bean("uniParcStoreConfigProperties")
    @ConfigurationProperties(prefix = "voldemort.uniparc")
    public StoreConfigProperties uniParcStoreConfigProperties() {
        return new StoreConfigProperties();
    }

    @Bean("uniParcTupleStreamTemplate")
    public TupleStreamTemplate uniParcTupleStreamTemplate(
            StreamerConfigProperties uniParcStreamerConfigProperties,
            HttpClient httpClient,
            SolrClient solrClient,
            SolrRequestConverter requestConverter) {
        return TupleStreamTemplate.builder()
                .streamConfig(uniParcStreamerConfigProperties)
                .solrClient(solrClient)
                .solrRequestConverter(requestConverter)
                .build();
    }

    @Bean("uniParcDocumentIdStream")
    public TupleStreamDocumentIdStream uniParcDocumentIdStream(
            TupleStreamTemplate uniParcTupleStreamTemplate,
            StreamerConfigProperties uniParcStreamerConfigProperties) {
        return TupleStreamDocumentIdStream.builder()
                .tupleStreamTemplate(uniParcTupleStreamTemplate)
                .streamConfig(uniParcStreamerConfigProperties)
                .build();
    }

    @Bean("uniParcFacetTupleStreamTemplate")
    public FacetTupleStreamTemplate uniParcFacetTupleStreamTemplate(
            RepositoryConfigProperties configProperties, HttpClient httpClient) {
        return FacetTupleStreamTemplate.builder()
                .collection(SolrCollection.uniparc.name())
                .zookeeperHost(configProperties.getZkHost())
                .build();
    }

    @Bean("uniParcEntryStoreStreamer")
    public StoreStreamer<UniParcEntry> uniParcEntryStoreStreamer(
            @Qualifier("uniParcStoreStreamerConfig")
                    StoreStreamerConfig<UniParcEntry> uniParcStoreStreamerConfig) {
        return new StoreStreamer<>(uniParcStoreStreamerConfig);
    }

    @Bean("uniParcStoreStreamerConfig")
    public StoreStreamerConfig<UniParcEntry> uniParcStoreStreamerConfig(
            @Qualifier("uniParcStoreClient") UniProtStoreClient<UniParcEntry> storeClient,
            @Qualifier("uniParcTupleStreamTemplate") TupleStreamTemplate tupleStreamTemplate,
            @Qualifier("uniParcStreamerConfigProperties") StreamerConfigProperties streamConfig,
            @Qualifier("uniParcDocumentIdStream") TupleStreamDocumentIdStream documentIdStream,
            @Qualifier("uniParcStoreRetryPolicy") RetryPolicy<Object> uniParcStoreRetryPolicy) {
        return StoreStreamerConfig.<UniParcEntry>builder()
                .streamConfig(streamConfig)
                .storeClient(storeClient)
                .tupleStreamTemplate(tupleStreamTemplate)
                .storeFetchRetryPolicy(uniParcStoreRetryPolicy)
                .documentIdStream(documentIdStream)
                .build();
    }

    @Bean("uniParcStoreRetryPolicy")
    public RetryPolicy<Object> uniParcStoreRetryPolicy(
            @Qualifier("uniParcStreamerConfigProperties") StreamerConfigProperties streamConfig) {
        return new RetryPolicy<>()
                .handle(IOException.class)
                .withDelay(Duration.ofMillis(streamConfig.getStoreFetchRetryDelayMillis()))
                .withMaxRetries(streamConfig.getStoreFetchMaxRetries());
    }

    @Bean("uniParcStoreClient")
    @Profile("live")
    public UniProtStoreClient<UniParcEntry> uniParcStoreClient(
            StoreConfigProperties uniParcStoreConfigProperties) {
        VoldemortClient<UniParcEntry> client =
                new VoldemortRemoteUniParcEntryStore(
                        uniParcStoreConfigProperties.getNumberOfConnections(),
                        uniParcStoreConfigProperties.getStoreName(),
                        uniParcStoreConfigProperties.getHost());
        return new UniProtStoreClient<>(client);
    }
}
