package org.uniprot.api.idmapping.service.config;

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
import org.uniprot.api.common.repository.solrstream.FacetTupleStreamTemplate;
import org.uniprot.api.common.repository.stream.common.TupleStreamTemplate;
import org.uniprot.api.common.repository.stream.document.TupleStreamDocumentIdStream;
import org.uniprot.api.common.repository.stream.store.StoreConfigProperties;
import org.uniprot.api.common.repository.stream.store.StoreStreamer;
import org.uniprot.api.common.repository.stream.store.StoreStreamerConfig;
import org.uniprot.api.common.repository.stream.store.StreamerConfigProperties;
import org.uniprot.api.common.repository.stream.store.uniprotkb.TaxonomyLineageService;
import org.uniprot.api.common.repository.stream.store.uniprotkb.UniProtKBStoreStreamer;
import org.uniprot.api.rest.respository.RepositoryConfig;
import org.uniprot.api.rest.respository.UniProtKBRepositoryConfigProperties;
import org.uniprot.core.uniprotkb.UniProtKBEntry;
import org.uniprot.core.util.Utils;
import org.uniprot.store.datastore.UniProtStoreClient;
import org.uniprot.store.datastore.voldemort.VoldemortClient;
import org.uniprot.store.datastore.voldemort.uniprot.VoldemortRemoteUniProtKBEntryStore;
import org.uniprot.store.search.SolrCollection;

/**
 * @author sahmad
 * @created 18/02/2021
 */
@Configuration
@Import(RepositoryConfig.class)
@Slf4j
public class UniProtKBIdMappingResultsConfig {

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

    @Bean("uniProtKBStoreConfigProperties")
    @ConfigurationProperties(prefix = "voldemort.uniprot")
    public StoreConfigProperties uniProtKBStoreConfigProperties() {
        return new StoreConfigProperties();
    }

    @Bean("uniProtKBStreamerConfigProperties")
    @ConfigurationProperties(prefix = "id.mapping.streamer.uniprot")
    public StreamerConfigProperties uniprotKbStreamerConfigProperties() {
        return new StreamerConfigProperties();
    }

    @Bean("uniProtKBTupleStreamTemplate")
    public TupleStreamTemplate uniProtKBTupleStreamTemplate(
            @Qualifier("uniProtKBStreamerConfigProperties")
                    StreamerConfigProperties configProperties,
            HttpClient httpClient,
            @Qualifier("uniProtKBSolrClient") SolrClient solrClient,
            SolrRequestConverter requestConverter) {
        return TupleStreamTemplate.builder()
                .streamConfig(configProperties)
                .solrClient(solrClient)
                .solrRequestConverter(requestConverter)
                .build();
    }

    @Bean("uniProtKBEntryStoreStreamer")
    public StoreStreamer<UniProtKBEntry> uniProtKBEntryStoreStreamer(
            @Qualifier("uniProtKBStoreStreamerConfig")
                    StoreStreamerConfig<UniProtKBEntry> uniProtKBStoreStreamerConfig,
            TaxonomyLineageService taxonomyLineageService) {
        return new UniProtKBStoreStreamer(uniProtKBStoreStreamerConfig, taxonomyLineageService);
    }

    @Bean("uniProtKBStoreStreamerConfig")
    public StoreStreamerConfig<UniProtKBEntry> uniProtKBStoreStreamerConfig(
            @Qualifier("uniProtStoreClient") UniProtStoreClient<UniProtKBEntry> storeClient,
            @Qualifier("uniProtKBTupleStreamTemplate") TupleStreamTemplate tupleStreamTemplate,
            @Qualifier("uniProtKBStreamerConfigProperties") StreamerConfigProperties streamConfig,
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
            @Qualifier("uniProtKBStreamerConfigProperties") StreamerConfigProperties streamConfig) {
        return new RetryPolicy<>()
                .handle(IOException.class)
                .withDelay(Duration.ofMillis(streamConfig.getStoreFetchRetryDelayMillis()))
                .withMaxRetries(streamConfig.getStoreFetchMaxRetries());
    }

    @Bean("uniprotKBdocumentIdStream")
    public TupleStreamDocumentIdStream uniprotKBdocumentIdStream(
            @Qualifier("uniProtKBTupleStreamTemplate")
                    TupleStreamTemplate uniProtKBTupleStreamTemplate,
            @Qualifier("uniProtKBStreamerConfigProperties")
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

    @Bean("uniProtStoreClient")
    @Profile("live")
    public UniProtStoreClient<UniProtKBEntry> uniProtStoreClient(
            @Qualifier("uniProtKBStoreConfigProperties")
                    StoreConfigProperties storeConfigProperties) {
        VoldemortClient<UniProtKBEntry> client =
                new VoldemortRemoteUniProtKBEntryStore(
                        storeConfigProperties.getNumberOfConnections(),
                        storeConfigProperties.getStoreName(),
                        storeConfigProperties.getHost());
        return new UniProtStoreClient<>(client);
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
