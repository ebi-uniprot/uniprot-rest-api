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
import org.uniprot.api.common.repository.search.SolrRequestConverter;
import org.uniprot.api.common.repository.solrstream.FacetTupleStreamTemplate;
import org.uniprot.api.common.repository.stream.common.TupleStreamTemplate;
import org.uniprot.api.common.repository.stream.document.TupleStreamDocumentIdStream;
import org.uniprot.api.common.repository.stream.store.StoreStreamer;
import org.uniprot.api.common.repository.stream.store.StoreStreamerConfig;
import org.uniprot.api.common.repository.stream.store.StreamerConfigProperties;
import org.uniprot.api.common.repository.stream.store.uniprotkb.TaxonomyLineageService;
import org.uniprot.api.common.repository.stream.store.uniprotkb.UniProtKBStoreStreamer;
import org.uniprot.api.rest.respository.RepositoryConfig;
import org.uniprot.api.rest.respository.UniProtKBRepositoryConfigProperties;
import org.uniprot.core.uniprotkb.UniProtKBEntry;
import org.uniprot.store.datastore.UniProtStoreClient;
import org.uniprot.store.search.SolrCollection;

import lombok.extern.slf4j.Slf4j;
import net.jodah.failsafe.RetryPolicy;

/**
 * @author sahmad
 * @created 18/02/2021
 */
@Configuration
@Import({RepositoryConfig.class})
@Slf4j
public class UniProtKBIdMappingResultsConfig {
    @Bean("uniProtKBIdMappingStreamerConfigProperties")
    @ConfigurationProperties(prefix = "id.mapping.streamer.uniprot")
    public StreamerConfigProperties uniprotKbStreamerConfigProperties() {
        return new StreamerConfigProperties();
    }

    @Bean("uniProtKBTupleStreamTemplate")
    public TupleStreamTemplate uniProtKBTupleStreamTemplate(
            @Qualifier("uniProtKBStreamerConfigProperties")
                    StreamerConfigProperties configProperties,
            SolrClient solrClient,
            SolrRequestConverter requestConverter) {
        return TupleStreamTemplate.builder()
                .streamConfig(configProperties)
                .solrClient(solrClient)
                .solrRequestConverter(requestConverter)
                .build();
    }

    @Bean("uniProtKBEntryStoreStreamer")
    public StoreStreamer<UniProtKBEntry> uniProtKBEntryStoreStreamer(
            @Qualifier("uniProtKBIdMappingStoreStreamerConfig")
                    StoreStreamerConfig<UniProtKBEntry> uniProtKBStoreStreamerConfig,
            TaxonomyLineageService taxonomyLineageService) {
        return new UniProtKBStoreStreamer(uniProtKBStoreStreamerConfig, taxonomyLineageService);
    }

    @Bean("uniProtKBIdMappingStoreStreamerConfig")
    public StoreStreamerConfig<UniProtKBEntry> uniProtKBStoreStreamerConfig(
            @Qualifier("uniProtStoreClient") UniProtStoreClient<UniProtKBEntry> storeClient,
            @Qualifier("uniProtKBTupleStreamTemplate") TupleStreamTemplate tupleStreamTemplate,
            @Qualifier("uniProtKBIdMappingStreamerConfigProperties")
                    StreamerConfigProperties streamConfig,
            @Qualifier("uniprotKBdocumentIdStream") TupleStreamDocumentIdStream documentIdStream,
            @Qualifier("uniProtKBStoreRetryPolicy") RetryPolicy<Object> uniProtKBStoreRetryPolicy) {
        return StoreStreamerConfig.<UniProtKBEntry>builder()
                .streamConfig(streamConfig)
                .storeClient(storeClient)
                .tupleStreamTemplate(tupleStreamTemplate)
                .storeFetchRetryPolicy(uniProtKBStoreRetryPolicy)
                .documentIdStream(documentIdStream)
                .build();
    }

    @Bean("uniProtKBStoreRetryPolicy")
    public RetryPolicy<Object> uniProtKBStoreRetryPolicy(
            @Qualifier("uniProtKBIdMappingStreamerConfigProperties")
                    StreamerConfigProperties streamConfig) {
        return new RetryPolicy<>()
                .handle(IOException.class)
                .withDelay(Duration.ofMillis(streamConfig.getStoreFetchRetryDelayMillis()))
                .withMaxRetries(streamConfig.getStoreFetchMaxRetries());
    }

    @Bean("uniprotKBdocumentIdStream")
    public TupleStreamDocumentIdStream uniprotKBdocumentIdStream(
            @Qualifier("uniProtKBTupleStreamTemplate")
                    TupleStreamTemplate uniProtKBTupleStreamTemplate,
            @Qualifier("uniProtKBIdMappingStreamerConfigProperties")
                    StreamerConfigProperties uniProtKBStreamerConfigProperties) {
        return TupleStreamDocumentIdStream.builder()
                .tupleStreamTemplate(uniProtKBTupleStreamTemplate)
                .streamConfig(uniProtKBStreamerConfigProperties)
                .build();
    }

    @Bean("uniproKBfacetTupleStreamTemplate")
    public FacetTupleStreamTemplate uniproKBfacetTupleStreamTemplate(
            UniProtKBRepositoryConfigProperties configProperties, HttpClient httpClient) {
        return FacetTupleStreamTemplate.builder()
                .collection(SolrCollection.uniprot.name())
                .zookeeperHost(configProperties.getZkHost())
                .build();
    }
}
