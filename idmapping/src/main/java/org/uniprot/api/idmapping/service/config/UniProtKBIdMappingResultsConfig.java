package org.uniprot.api.idmapping.service.config;

import java.io.IOException;
import java.time.Duration;

import lombok.extern.slf4j.Slf4j;
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
import org.uniprot.api.common.repository.stream.store.StreamerConfigProperties;
import org.uniprot.api.rest.respository.RepositoryConfig;
import org.uniprot.api.rest.respository.RepositoryConfigProperties;
import org.uniprot.core.uniprotkb.UniProtKBEntry;
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
            SolrClient solrClient,
            SolrRequestConverter requestConverter) {
        return TupleStreamTemplate.builder()
                .streamConfig(configProperties)
                .httpClient(httpClient)
                .solrClient(solrClient)
                .solrRequestConverter(requestConverter)
                .build();
    }

    @Bean("uniProtKBEntryStoreStreamer")
    public StoreStreamer<UniProtKBEntry> uniProtKBEntryStoreStreamer(
            @Qualifier("uniProtStoreClient") UniProtStoreClient<UniProtKBEntry> storeClient,
            @Qualifier("uniProtKBTupleStreamTemplate") TupleStreamTemplate tupleStreamTemplate,
            @Qualifier("uniProtKBStreamerConfigProperties") StreamerConfigProperties streamConfig,
            @Qualifier("uniprotKBdocumentIdStream") TupleStreamDocumentIdStream documentIdStream) {

        RetryPolicy<Object> storeRetryPolicy =
                new RetryPolicy<>()
                        .handle(IOException.class)
                        .withDelay(Duration.ofMillis(streamConfig.getStoreFetchRetryDelayMillis()))
                        .withMaxRetries(streamConfig.getStoreFetchMaxRetries());

        return StoreStreamer.<UniProtKBEntry>builder()
                .streamConfig(streamConfig)
                .storeClient(storeClient)
                .tupleStreamTemplate(tupleStreamTemplate)
                .storeFetchRetryPolicy(storeRetryPolicy)
                .documentIdStream(documentIdStream)
                .build();
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
            RepositoryConfigProperties configProperties, HttpClient httpClient) {
        return FacetTupleStreamTemplate.builder()
                .collection(SolrCollection.uniprot.name())
                .zookeeperHost(configProperties.getZkHost())
                .httpClient(httpClient)
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
}
