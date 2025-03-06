package org.uniprot.api.uniparc.common.repository;

import java.io.IOException;
import java.time.Duration;

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
import org.uniprot.api.common.repository.stream.store.uniparc.UniParcCrossReferenceLazyLoader;
import org.uniprot.api.common.repository.stream.store.uniparc.UniParcLightStoreStreamer;
import org.uniprot.api.rest.respository.RepositoryConfig;
import org.uniprot.api.rest.respository.RepositoryConfigProperties;
import org.uniprot.api.uniparc.common.repository.store.light.UniParcLightStoreClient;
import org.uniprot.api.uniparc.common.repository.store.stream.UniParcFastaStoreStreamer;
import org.uniprot.api.uniparc.common.service.light.UniParcCrossReferenceService;
import org.uniprot.core.uniparc.UniParcEntry;
import org.uniprot.core.uniparc.UniParcEntryLight;
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
    public StoreStreamer<UniParcEntry> uniParcFastaStoreStreamer(
            StoreStreamerConfig<UniParcEntry> uniParcFastaStoreStreamerConfig,
            StoreStreamerConfig<UniParcEntryLight> lightConfig,
            UniParcCrossReferenceService uniParcCrossReferenceService) {
        return new UniParcFastaStoreStreamer(
                uniParcFastaStoreStreamerConfig, lightConfig, uniParcCrossReferenceService);
    }

    @Bean
    public StoreStreamerConfig<UniParcEntry> uniParcFastaStoreStreamerConfig(
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
                .streamConfig(uniParcStreamerConfigProperties)
                .tupleStreamTemplate(uniParcTupleStreamTemplate)
                .storeFetchRetryPolicy(storeRetryPolicy)
                .documentIdStream(uniParcTupleStreamDocumentIdStream)
                .build();
    }

    @Bean
    public StoreStreamer<UniParcEntryLight> uniParcEntryLightStoreStreamer(
            StoreStreamerConfig<UniParcEntryLight> storeLightStreamerConfig,
            UniParcCrossReferenceLazyLoader uniParcCrossReferenceLazyLoader) {
        return new UniParcLightStoreStreamer(
                storeLightStreamerConfig, uniParcCrossReferenceLazyLoader);
    }

    @Bean
    public StoreStreamerConfig<UniParcEntryLight> storeLightStreamerConfig(
            UniParcLightStoreClient uniParcLightClient,
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
        return StoreStreamerConfig.<UniParcEntryLight>builder()
                .storeClient(uniParcLightClient)
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
            RepositoryConfigProperties configProperties) {
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
