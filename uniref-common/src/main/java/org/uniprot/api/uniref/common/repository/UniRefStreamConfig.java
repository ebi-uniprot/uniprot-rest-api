package org.uniprot.api.uniref.common.repository;

import java.io.IOException;
import java.time.Duration;

import org.apache.solr.client.solrj.SolrClient;
import org.springframework.beans.factory.annotation.Qualifier;
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
import org.uniprot.api.uniref.common.repository.store.UniRefLightStoreClient;
import org.uniprot.core.uniref.UniRefEntryLight;
import org.uniprot.store.search.SolrCollection;

import lombok.extern.slf4j.Slf4j;
import net.jodah.failsafe.RetryPolicy;

/**
 * @author jluo date: 21 Aug 2019
 */
@Configuration
@Import(RepositoryConfig.class)
@Slf4j
public class UniRefStreamConfig {

    @Bean(name = "uniRefTupleStreamTemplate")
    public TupleStreamTemplate tupleStreamTemplate(
            @Qualifier("uniRefStreamerConfigProperties") StreamerConfigProperties configProperties,
            SolrClient solrClient,
            SolrRequestConverter requestConverter) {
        return TupleStreamTemplate.builder()
                .streamConfig(configProperties)
                .solrClient(solrClient)
                .solrRequestConverter(requestConverter)
                .build();
    }

    @Bean
    public StoreStreamer<UniRefEntryLight> uniRefEntryStoreStreamer(
            StoreStreamerConfig<UniRefEntryLight> storeStreamerConfig) {
        return new StoreStreamer<>(storeStreamerConfig);
    }

    @Bean(name = "uniRefStoreStreamerConfig")
    public StoreStreamerConfig<UniRefEntryLight> storeStreamerConfig(
            UniRefLightStoreClient uniRefLightStoreClient,
            @Qualifier("uniRefTupleStreamTemplate") TupleStreamTemplate tupleStreamTemplate,
            @Qualifier("uniRefStreamerConfigProperties") StreamerConfigProperties streamConfig,
            @Qualifier("uniRefDocumentIdStream") TupleStreamDocumentIdStream documentIdStream) {

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

    @Bean("uniRefStreamerConfigProperties")
    @ConfigurationProperties(prefix = "streamer.uniref")
    public StreamerConfigProperties resultsConfigProperties() {
        return new StreamerConfigProperties();
    }

    @Bean("uniRefFacetTupleStreamTemplate")
    public FacetTupleStreamTemplate facetTupleStreamTemplate(
            RepositoryConfigProperties configProperties) {
        return FacetTupleStreamTemplate.builder()
                .collection(SolrCollection.uniref.name())
                .zookeeperHost(configProperties.getZkHost())
                .build();
    }

    @Bean("uniRefDocumentIdStream")
    public TupleStreamDocumentIdStream documentIdStream(
            @Qualifier("uniRefTupleStreamTemplate") TupleStreamTemplate tupleStreamTemplate,
            @Qualifier("uniRefStreamerConfigProperties") StreamerConfigProperties streamConfig) {
        return TupleStreamDocumentIdStream.builder()
                .tupleStreamTemplate(tupleStreamTemplate)
                .streamConfig(streamConfig)
                .build();
    }
}
