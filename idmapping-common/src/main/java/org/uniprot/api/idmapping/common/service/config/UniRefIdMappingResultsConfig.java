package org.uniprot.api.idmapping.common.service.config;

import java.io.IOException;
import java.time.Duration;

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
import org.uniprot.core.uniref.UniRefEntryLight;
import org.uniprot.store.datastore.UniProtStoreClient;
import org.uniprot.store.datastore.voldemort.VoldemortClient;
import org.uniprot.store.datastore.voldemort.light.uniref.VoldemortRemoteUniRefEntryLightStore;
import org.uniprot.store.search.SolrCollection;

import net.jodah.failsafe.RetryPolicy;

/**
 * @author lgonzales
 * @since 25/02/2021
 */
@Configuration
@Import(RepositoryConfig.class)
public class UniRefIdMappingResultsConfig {

    @Bean("uniRefLightStoreConfigProperties")
    @ConfigurationProperties(prefix = "voldemort.uniref.light")
    public StoreConfigProperties uniRefLightStoreConfigProperties() {
        return new StoreConfigProperties();
    }

    @Bean("uniRefStreamerConfigProperties")
    @ConfigurationProperties(prefix = "id.mapping.streamer.uniref")
    public StreamerConfigProperties uniRefStreamerConfigProperties() {
        return new StreamerConfigProperties();
    }

    @Bean("uniRefTupleStreamTemplate")
    public TupleStreamTemplate uniRefTupleStreamTemplate(
            StreamerConfigProperties uniRefStreamerConfigProperties,
            HttpClient httpClient,
            SolrClient solrClient,
            SolrRequestConverter requestConverter) {
        return TupleStreamTemplate.builder()
                .streamConfig(uniRefStreamerConfigProperties)
                .solrClient(solrClient)
                .solrRequestConverter(requestConverter)
                .build();
    }

    @Bean("uniRefDocumentIdStream")
    public TupleStreamDocumentIdStream uniRefDocumentIdStream(
            TupleStreamTemplate uniRefTupleStreamTemplate,
            StreamerConfigProperties uniRefStreamerConfigProperties) {
        return TupleStreamDocumentIdStream.builder()
                .tupleStreamTemplate(uniRefTupleStreamTemplate)
                .streamConfig(uniRefStreamerConfigProperties)
                .build();
    }

    @Bean("uniRefFacetTupleStreamTemplate")
    public FacetTupleStreamTemplate uniRefFacetTupleStreamTemplate(
            RepositoryConfigProperties configProperties, HttpClient httpClient) {
        return FacetTupleStreamTemplate.builder()
                .collection(SolrCollection.uniref.name())
                .zookeeperHost(configProperties.getZkHost())
                .build();
    }

    @Bean("uniRefEntryStoreStreamer")
    public StoreStreamer<UniRefEntryLight> uniRefEntryStoreStreamer(
            @Qualifier("uniRefStoreStreamerConfig")
                    StoreStreamerConfig<UniRefEntryLight> uniRefStoreStreamerConfig) {
        return new StoreStreamer<>(uniRefStoreStreamerConfig);
    }

    @Bean("uniRefStoreStreamerConfig")
    public StoreStreamerConfig<UniRefEntryLight> uniRefStoreStreamerConfig(
            @Qualifier("uniRefLightStoreClient") UniProtStoreClient<UniRefEntryLight> storeClient,
            @Qualifier("uniRefTupleStreamTemplate") TupleStreamTemplate tupleStreamTemplate,
            @Qualifier("uniRefStreamerConfigProperties") StreamerConfigProperties streamConfig,
            @Qualifier("uniRefDocumentIdStream") TupleStreamDocumentIdStream documentIdStream,
            @Qualifier("uniRefStoreRetryPolicy") RetryPolicy<Object> uniRefStoreRetryPolicy) {
        return StoreStreamerConfig.<UniRefEntryLight>builder()
                .streamConfig(streamConfig)
                .storeClient(storeClient)
                .tupleStreamTemplate(tupleStreamTemplate)
                .storeFetchRetryPolicy(uniRefStoreRetryPolicy)
                .documentIdStream(documentIdStream)
                .build();
    }

    @Bean("uniRefStoreRetryPolicy")
    public RetryPolicy<Object> uniRefStoreRetryPolicy(
            @Qualifier("uniRefStreamerConfigProperties") StreamerConfigProperties streamConfig) {
        return new RetryPolicy<>()
                .handle(IOException.class)
                .withDelay(Duration.ofMillis(streamConfig.getStoreFetchRetryDelayMillis()))
                .withMaxRetries(streamConfig.getStoreFetchMaxRetries());
    }

    @Bean("uniRefLightStoreClient")
    @Profile("live")
    public UniProtStoreClient<UniRefEntryLight> uniRefLightStoreClient(
            StoreConfigProperties uniRefLightStoreConfigProperties) {
        VoldemortClient<UniRefEntryLight> client =
                new VoldemortRemoteUniRefEntryLightStore(
                        uniRefLightStoreConfigProperties.getNumberOfConnections(),
                        uniRefLightStoreConfigProperties.isBrotliEnabled(),
                        uniRefLightStoreConfigProperties.getStoreName(),
                        uniRefLightStoreConfigProperties.getHost());
        return new UniProtStoreClient<>(client);
    }
}
