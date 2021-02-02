package org.uniprot.api.support.data.keyword.response;

import java.util.Collections;

import lombok.extern.slf4j.Slf4j;
import net.jodah.failsafe.RetryPolicy;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Profile;
import org.springframework.http.client.BufferingClientHttpRequestFactory;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.DefaultUriBuilderFactory;
import org.uniprot.api.common.repository.stream.document.DefaultDocumentIdStream;
import org.uniprot.api.common.repository.stream.rdf.RDFStreamer;
import org.uniprot.api.common.repository.stream.rdf.RDFStreamerConfigProperties;
import org.uniprot.api.rest.output.RequestResponseLoggingInterceptor;
import org.uniprot.api.rest.respository.RepositoryConfig;
import org.uniprot.api.rest.service.RDFPrologs;
import org.uniprot.api.rest.service.RDFService;
import org.uniprot.api.support.data.common.RDFStreamConfig;
import org.uniprot.api.support.data.common.SolrDocumentRDFIdConverter;
import org.uniprot.api.support.data.keyword.repository.KeywordRepository;
import org.uniprot.store.search.document.keyword.KeywordDocument;

/**
 * @author sahmad
 * @created 02/02/2021
 */
@Configuration
@Import(RepositoryConfig.class)
@Slf4j
public class KeywordStreamConfig {

    @Bean(name = "keywordRDFStreamer")
    public RDFStreamer keywordRDFStreamer(
            @Qualifier("keywordRDFRestTemplate") RestTemplate restTemplate,
            DefaultDocumentIdStream<KeywordDocument> keywordDocumentIdStream,
            RDFStreamerConfigProperties keywordRDFConfigProperties) {

        RetryPolicy<Object> rdfRetryPolicy =
                RDFStreamConfig.rdfRetryPolicy(keywordRDFConfigProperties);

        return RDFStreamer.builder()
                .rdfBatchSize(keywordRDFConfigProperties.getBatchSize())
                .rdfFetchRetryPolicy(rdfRetryPolicy)
                .rdfService(new RDFService<>(restTemplate, String.class))
                .rdfProlog(RDFPrologs.KEYWORD_PROLOG)
                .idStream(keywordDocumentIdStream)
                .build();
    }

    @Bean
    @ConfigurationProperties(prefix = "keyword.streamer.rdf")
    public RDFStreamerConfigProperties keywordRDFConfigProperties() {
        return new RDFStreamerConfigProperties();
    }

    @Bean
    public DefaultDocumentIdStream<KeywordDocument> keywordDocumentIdStream(
            KeywordRepository repository) {
        return DefaultDocumentIdStream.<KeywordDocument>builder()
                .repository(repository)
                .documentToId(document -> new SolrDocumentRDFIdConverter().apply(document))
                .build();
    }

    @Bean(name = "keywordRDFRestTemplate")
    @Profile("live")
    RestTemplate keywordRDFRestTemplate(RDFStreamerConfigProperties keywordRDFConfigProperties) {
        ClientHttpRequestFactory factory =
                new BufferingClientHttpRequestFactory(new SimpleClientHttpRequestFactory());
        RestTemplate restTemplate = new RestTemplate(factory);
        restTemplate.setInterceptors(
                Collections.singletonList(new RequestResponseLoggingInterceptor()));
        restTemplate.setUriTemplateHandler(
                new DefaultUriBuilderFactory(keywordRDFConfigProperties.getRequestUrl()));
        return restTemplate;
    }
}
