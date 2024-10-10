package org.uniprot.api.idmapping.common.service.config;

import java.io.IOException;
import java.time.Duration;

import org.apache.solr.client.solrj.SolrClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
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
import org.uniprot.api.common.repository.stream.store.uniparc.UniParcCrossReferenceLazyLoader;
import org.uniprot.api.common.repository.stream.store.uniparc.UniParcCrossReferenceStoreConfigProperties;
import org.uniprot.api.common.repository.stream.store.uniparc.UniParcLightStoreStreamer;
import org.uniprot.api.rest.respository.RepositoryConfig;
import org.uniprot.api.rest.respository.RepositoryConfigProperties;
import org.uniprot.core.uniparc.UniParcEntryLight;
import org.uniprot.core.uniparc.impl.UniParcCrossReferencePair;
import org.uniprot.store.datastore.UniProtStoreClient;
import org.uniprot.store.datastore.voldemort.VoldemortClient;
import org.uniprot.store.datastore.voldemort.light.uniparc.VoldemortRemoteUniParcEntryLightStore;
import org.uniprot.store.datastore.voldemort.light.uniparc.crossref.VoldemortRemoteUniParcCrossReferenceStore;
import org.uniprot.store.search.SolrCollection;

import net.jodah.failsafe.RetryPolicy;

/**
 * @author lgonzales
 * @since 25/02/2021
 */
@Configuration
@Import(RepositoryConfig.class)
@EnableConfigurationProperties({UniParcCrossReferenceStoreConfigProperties.class})
public class UniParcIdMappingResultsConfig {

    @Bean("uniParcStreamerConfigProperties")
    @ConfigurationProperties(prefix = "id.mapping.streamer.uniparc")
    public StreamerConfigProperties uniParcStreamerConfigProperties() {
        return new StreamerConfigProperties();
    }

    @Bean("uniParcStoreConfigProperties")
    @ConfigurationProperties(prefix = "voldemort.uniparc.light")
    public StoreConfigProperties uniParcStoreConfigProperties() {
        return new StoreConfigProperties();
    }

    @Bean("uniParcTupleStreamTemplate")
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
            RepositoryConfigProperties configProperties) {
        return FacetTupleStreamTemplate.builder()
                .collection(SolrCollection.uniparc.name())
                .zookeeperHost(configProperties.getZkHost())
                .build();
    }

    @Bean("uniParcEntryStoreStreamer")
    public StoreStreamer<UniParcEntryLight> uniParcEntryStoreStreamer(
            @Qualifier("uniParcStoreStreamerConfig")
                    StoreStreamerConfig<UniParcEntryLight> uniParcStoreStreamerConfig) {
        return new StoreStreamer<>(uniParcStoreStreamerConfig);
    }

    @Bean("uniParcStoreStreamerConfig")
    public StoreStreamerConfig<UniParcEntryLight> uniParcStoreStreamerConfig(
            @Qualifier("uniParcLightStoreClient") UniProtStoreClient<UniParcEntryLight> storeClient,
            @Qualifier("uniParcTupleStreamTemplate") TupleStreamTemplate tupleStreamTemplate,
            @Qualifier("uniParcStreamerConfigProperties") StreamerConfigProperties streamConfig,
            @Qualifier("uniParcDocumentIdStream") TupleStreamDocumentIdStream documentIdStream,
            @Qualifier("uniParcStoreRetryPolicy") RetryPolicy<Object> uniParcStoreRetryPolicy) {
        return StoreStreamerConfig.<UniParcEntryLight>builder()
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

    @Bean("uniParcLightStoreClient")
    @Profile("live")
    public UniProtStoreClient<UniParcEntryLight> uniParcStoreClient(
            StoreConfigProperties uniParcStoreConfigProperties) {
        VoldemortClient<UniParcEntryLight> client =
                new VoldemortRemoteUniParcEntryLightStore(
                        uniParcStoreConfigProperties.getNumberOfConnections(),
                        uniParcStoreConfigProperties.isBrotliEnabled(),
                        uniParcStoreConfigProperties.getStoreName(),
                        uniParcStoreConfigProperties.getHost());
        return new UniProtStoreClient<>(client);
    }

    @Bean
    public StoreStreamer<UniParcEntryLight> uniParcEntryLightStoreStreamer(
            StoreStreamerConfig<UniParcEntryLight> storeLightStreamerConfig,
            UniParcCrossReferenceLazyLoader uniParcCrossReferenceLazyLoader) {
        return new UniParcLightStoreStreamer(
                storeLightStreamerConfig, uniParcCrossReferenceLazyLoader);
    }

    @Bean
    public UniParcCrossReferenceLazyLoader uniParcCrossReferenceLazyLoader(
            UniParcCrossReferenceStoreConfigProperties configProperties,
            UniProtStoreClient<UniParcCrossReferencePair> uniParcCrossReferenceStoreClient) {
        return new UniParcCrossReferenceLazyLoader(
                uniParcCrossReferenceStoreClient, configProperties);
    }

    @Bean
    @Profile("live")
    public UniProtStoreClient<UniParcCrossReferencePair> uniParcCrossReferenceStoreClient(
            UniParcCrossReferenceStoreConfigProperties configProperties) {
        VoldemortClient<UniParcCrossReferencePair> client =
                new VoldemortRemoteUniParcCrossReferenceStore(
                        configProperties.getNumberOfConnections(),
                        configProperties.isBrotliEnabled(),
                        configProperties.getStoreName(),
                        configProperties.getHost());
        return new UniProtStoreClient<>(client);
    }
}
