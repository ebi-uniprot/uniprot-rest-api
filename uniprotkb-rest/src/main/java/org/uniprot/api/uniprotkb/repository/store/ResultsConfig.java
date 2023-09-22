package org.uniprot.api.uniprotkb.repository.store;

import static java.util.Arrays.asList;

import java.io.IOException;
import java.time.Duration;
import java.util.Optional;

import lombok.extern.slf4j.Slf4j;
import net.jodah.failsafe.RetryPolicy;

import org.apache.http.client.HttpClient;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.impl.CloudSolrClient;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.springframework.beans.factory.BeanCreationException;
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
import org.uniprot.core.util.Utils;

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
                httpClient,
                config.getZkHost(),
                config.getConnectionTimeout(),
                config.getSocketTimeout(),
                config.getHttphost());
    }

    @Bean
    public TupleStreamTemplate tupleStreamTemplate(
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
            StoreStreamerConfig<UniProtKBEntry> storeStreamerConfig,
            TaxonomyLineageService taxonomyLineageService) {
        return new UniProtKBStoreStreamer(storeStreamerConfig, taxonomyLineageService);
    }

    @Bean
    public StoreStreamerConfig<UniProtKBEntry> storeStreamerConfig(
            UniProtKBStoreClient uniProtClient,
            TupleStreamTemplate tupleStreamTemplate,
            @Qualifier("streamConfig") StreamerConfigProperties streamConfig,
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

    @Bean(name = "streamConfig")
    @ConfigurationProperties(prefix = "streamer.uniprot")
    public StreamerConfigProperties resultsConfigProperties() {
        return new StreamerConfigProperties();
    }

    @Bean
    public TupleStreamDocumentIdStream documentIdStream(
            TupleStreamTemplate tupleStreamTemplate,
            @Qualifier("streamConfig") StreamerConfigProperties streamConfig) {
        return TupleStreamDocumentIdStream.builder()
                .tupleStreamTemplate(tupleStreamTemplate)
                .streamConfig(streamConfig)
                .build();
    }

    private SolrClient buildSolrClient(
            HttpClient httpClient,
            String zkHost,
            int connTimeout,
            int sockTimeout,
            String httpHost) {
        if (Utils.notNullNotEmpty(zkHost)) {
            String[] zookeeperHosts = zkHost.split(",");
            return new CloudSolrClient.Builder(asList(zookeeperHosts), Optional.empty())
                    .withConnectionTimeout(connTimeout)
                    .withHttpClient(httpClient)
                    .withSocketTimeout(sockTimeout)
                    .build();
        } else if (Utils.notNullNotEmpty(httpHost)) {
            return new HttpSolrClient.Builder().withBaseSolrUrl(httpHost).build();
        } else {
            throw new BeanCreationException(
                    "make sure your application.properties has right solr zookeeperhost or httphost properties");
        }
    }
}
