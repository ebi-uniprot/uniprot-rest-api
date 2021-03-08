package org.uniprot.api.idmapping.service.config;

import java.io.IOException;
import java.time.temporal.ChronoUnit;
import java.util.Collections;

import lombok.extern.slf4j.Slf4j;
import net.jodah.failsafe.RetryPolicy;

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
import org.uniprot.api.common.repository.stream.rdf.RDFStreamer;
import org.uniprot.api.common.repository.stream.rdf.RDFStreamerConfigProperties;
import org.uniprot.api.rest.output.RequestResponseLoggingInterceptor;
import org.uniprot.api.rest.respository.RepositoryConfig;
import org.uniprot.api.rest.service.RDFPrologs;
import org.uniprot.api.rest.service.RDFService;

/**
 * @author sahmad
 * @created 04/03/2021
 */
@Configuration
@Import(RepositoryConfig.class)
@Slf4j
public class UniProtKBRDFStreamerConfig {

    @Bean
    public RDFStreamer uniProtKBRDFStreamer(
            RestTemplate uniProtKBRestTemplate,
            RDFStreamerConfigProperties uniProtKBRDFConfigProperties) {
        int rdfRetryDelay = uniProtKBRDFConfigProperties.getRetryDelayMillis();
        int maxRdfRetryDelay = rdfRetryDelay * 8;
        RetryPolicy<Object> rdfRetryPolicy =
                new RetryPolicy<>()
                        .handle(IOException.class)
                        .withBackoff(rdfRetryDelay, maxRdfRetryDelay, ChronoUnit.MILLIS)
                        .withMaxRetries(uniProtKBRDFConfigProperties.getMaxRetries())
                        .onRetry(
                                e ->
                                        log.warn(
                                                "Call to RDF server failed. Failure #{}. Retrying...",
                                                e.getAttemptCount()));

        return RDFStreamer.builder()
                .rdfBatchSize(uniProtKBRDFConfigProperties.getBatchSize())
                .rdfFetchRetryPolicy(rdfRetryPolicy)
                .rdfService(new RDFService<>(uniProtKBRestTemplate, String.class))
                .rdfProlog(RDFPrologs.UNIPROT_RDF_PROLOG)
                .build();
    }

    @Bean
    @Profile("live")
    RestTemplate uniProtKBRestTemplate(RDFStreamerConfigProperties uniProtKBRDFConfigProperties) {
        ClientHttpRequestFactory factory =
                new BufferingClientHttpRequestFactory(new SimpleClientHttpRequestFactory());
        RestTemplate restTemplate = new RestTemplate(factory);
        restTemplate.setInterceptors(
                Collections.singletonList(new RequestResponseLoggingInterceptor()));
        restTemplate.setUriTemplateHandler(
                new DefaultUriBuilderFactory(uniProtKBRDFConfigProperties.getRequestUrl()));
        return restTemplate;
    }

    @Bean
    @ConfigurationProperties(prefix = "id.mapping.streamer.uniprot.rdf")
    public RDFStreamerConfigProperties uniProtKBRDFConfigProperties() {
        return new RDFStreamerConfigProperties();
    }
}
