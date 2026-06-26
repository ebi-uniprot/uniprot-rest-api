package org.uniprot.api.uniprotkb.common.service.precomputed;

import java.io.IOException;
import java.time.Duration;

import org.apache.solr.client.solrj.SolrClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.uniprot.api.common.repository.search.SolrRequestConverter;
import org.uniprot.api.common.repository.stream.common.TupleStreamTemplate;
import org.uniprot.api.common.repository.stream.document.TupleStreamDocumentIdStream;
import org.uniprot.api.common.repository.stream.store.StoreStreamer;
import org.uniprot.api.common.repository.stream.store.StoreStreamerConfig;
import org.uniprot.api.common.repository.stream.store.StreamerConfigProperties;
import org.uniprot.api.rest.respository.RepositoryConfig;
import org.uniprot.api.uniprotkb.common.repository.store.precomputed.PrecomputedAnnotationStoreClient;
import org.uniprot.core.uniprotkb.UniProtKBEntry;

import lombok.extern.slf4j.Slf4j;
import net.jodah.failsafe.RetryPolicy;

@Configuration
@Import(RepositoryConfig.class)
@Slf4j
public class PrecomputedAnnotationStreamConfig {
    @Bean
    public StoreStreamer<UniProtKBEntry> precomputedAnnotationStoreStreamer(
            StoreStreamerConfig<UniProtKBEntry> precomputedAnnotationStoreStreamerConfig) {
        return new StoreStreamer<>(precomputedAnnotationStoreStreamerConfig);
    }

    @Bean("precomputedAnnotationTupleStream")
    public TupleStreamTemplate tupleStreamTemplate(
            @Qualifier("precomputedAnnotationStreamerConfigProperties")
                    StreamerConfigProperties configProperties,
            @Qualifier("uniProtKBSolrClient") SolrClient solrClient,
            SolrRequestConverter requestConverter) {
        return TupleStreamTemplate.builder()
                .streamConfig(configProperties)
                .solrClient(solrClient)
                .solrRequestConverter(requestConverter)
                .build();
    }

    @Bean(name = "precomputedAnnotationStreamerConfigProperties")
    @ConfigurationProperties(prefix = "streamer.precomputedannotation")
    public StreamerConfigProperties resultsConfigProperties() {
        return new StreamerConfigProperties();
    }

    @Bean("precomputedAnnotationTupleStreamDocumentIdStream")
    public TupleStreamDocumentIdStream documentIdStream(
            @Qualifier("precomputedAnnotationTupleStream") TupleStreamTemplate tupleStreamTemplate,
            @Qualifier("precomputedAnnotationStreamerConfigProperties")
                    StreamerConfigProperties streamConfig) {
        return TupleStreamDocumentIdStream.builder()
                .tupleStreamTemplate(tupleStreamTemplate)
                .streamConfig(streamConfig)
                .build();
    }

    @Bean
    public StoreStreamerConfig<UniProtKBEntry> precomputedAnnotationStoreStreamerConfig(
            PrecomputedAnnotationStoreClient precomputedAnnotationStoreClient,
            @Qualifier("precomputedAnnotationTupleStream") TupleStreamTemplate tupleStreamTemplate,
            @Qualifier("precomputedAnnotationStreamerConfigProperties")
                    StreamerConfigProperties streamConfig,
            @Qualifier("precomputedAnnotationTupleStreamDocumentIdStream")
                    TupleStreamDocumentIdStream documentIdStream) {

        RetryPolicy<Object> storeRetryPolicy =
                new RetryPolicy<>()
                        .handle(IOException.class)
                        .withDelay(Duration.ofMillis(streamConfig.getStoreFetchRetryDelayMillis()))
                        .withMaxRetries(streamConfig.getStoreFetchMaxRetries());

        return StoreStreamerConfig.<UniProtKBEntry>builder()
                .streamConfig(streamConfig)
                .storeClient(precomputedAnnotationStoreClient)
                .tupleStreamTemplate(tupleStreamTemplate)
                .storeFetchRetryPolicy(storeRetryPolicy)
                .documentIdStream(documentIdStream)
                .build();
    }
}
