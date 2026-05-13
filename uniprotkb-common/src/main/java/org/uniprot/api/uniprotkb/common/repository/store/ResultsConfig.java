package org.uniprot.api.uniprotkb.common.repository.store;

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

import lombok.extern.slf4j.Slf4j;
import net.jodah.failsafe.RetryPolicy;

/**
 * Created 21/08/18
 *
 * @author Edd
 */
@Configuration
@Import(RepositoryConfig.class)
@Slf4j
public class ResultsConfig {

    @Bean("uniProtKBSolrClient")
    @Profile("live")
    public SolrClient uniProtKBSolrClient(
            HttpClient httpClient, UniProtKBRepositoryConfigProperties config) {
        return buildSolrClient(
                config.getZkHost(),
                config.getConnectionTimeout(),
                config.getSocketTimeout(),
                config.getHttphost(),
                config.getUsername(),
                config.getPassword());
    }

    @Bean("uniProtKBTupleStream")
    public TupleStreamTemplate tupleStreamTemplate(
            @Qualifier("uniProtKBStreamerConfigProperties")
                    StreamerConfigProperties configProperties,
            @Qualifier("uniProtKBSolrClient") SolrClient solrClient,
            SolrRequestConverter requestConverter) {
        return TupleStreamTemplate.builder()
                .streamConfig(configProperties)
                .solrClient(solrClient)
                .solrRequestConverter(requestConverter)
                .build();
    }

    @Bean
    public StoreStreamer<UniProtKBEntry> uniProtEntryStoreStreamer(
            StoreStreamerConfig<UniProtKBEntry> uniProtKBStoreStreamerConfig,
            TaxonomyLineageService taxonomyLineageService) {
        return new UniProtKBStoreStreamer(uniProtKBStoreStreamerConfig, taxonomyLineageService);
    }

    @Bean
    public StoreStreamerConfig<UniProtKBEntry> uniProtKBStoreStreamerConfig(
            UniProtKBStoreClient uniProtClient,
            @Qualifier("uniProtKBTupleStream") TupleStreamTemplate tupleStreamTemplate,
            @Qualifier("uniProtKBStreamerConfigProperties") StreamerConfigProperties streamConfig,
            @Qualifier("uniProtKBTupleStreamDocumentIdStream")
                    TupleStreamDocumentIdStream documentIdStream) {

        RetryPolicy<Object> storeRetryPolicy =
                new RetryPolicy<>()
                        .handle(IOException.class)
                        .withDelay(Duration.ofMillis(streamConfig.getStoreFetchRetryDelayMillis()))
                        .withMaxRetries(streamConfig.getStoreFetchMaxRetries());

        return StoreStreamerConfig.<UniProtKBEntry>builder()
                .streamConfig(streamConfig)
                .storeClient(uniProtClient)
                .tupleStreamTemplate(tupleStreamTemplate)
                .storeFetchRetryPolicy(storeRetryPolicy)
                .documentIdStream(documentIdStream)
                .build();
    }

    @Bean(name = "uniProtKBStreamerConfigProperties")
    @ConfigurationProperties(prefix = "streamer.uniprot")
    public StreamerConfigProperties resultsConfigProperties() {
        StreamerConfigProperties b = new StreamerConfigProperties();
        return b;
    }

    @Bean("uniProtKBTupleStreamDocumentIdStream")
    public TupleStreamDocumentIdStream documentIdStream(
            @Qualifier("uniProtKBTupleStream") TupleStreamTemplate tupleStreamTemplate,
            @Qualifier("uniProtKBStreamerConfigProperties") StreamerConfigProperties streamConfig) {
        return TupleStreamDocumentIdStream.builder()
                .tupleStreamTemplate(tupleStreamTemplate)
                .streamConfig(streamConfig)
                .build();
    }

    private SolrClient buildSolrClient(
            String zkHost,
            int connTimeout,
            int sockTimeout,
            String httpHost,
            String username,
            String password) {
        return RepositoryConfig.buildSolrClient(
                zkHost, connTimeout, sockTimeout, httpHost, username, password);
    }
}
