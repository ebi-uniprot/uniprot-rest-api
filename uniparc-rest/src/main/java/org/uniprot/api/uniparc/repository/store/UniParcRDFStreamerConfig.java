package org.uniprot.api.uniparc.repository.store;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.BufferingClientHttpRequestFactory;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.DefaultUriBuilderFactory;
import org.uniprot.api.common.repository.stream.rdf.*;
import org.uniprot.api.rest.output.RequestResponseLoggingInterceptor;
import org.uniprot.api.rest.service.TagProvider;

import java.util.Collections;

@Configuration
public class UniParcRDFStreamerConfig {
    private final PrologProvider prologProvider;
    private final TagProvider tagProvider;

    public UniParcRDFStreamerConfig(PrologProvider prologProvider, TagProvider tagProvider) {
        this.prologProvider = prologProvider;
        this.tagProvider = tagProvider;
    }

    @Bean
    public RDFStreamer uniparcRdfStreamer(
            RDFStreamerConfigProperties uniparcRDFStreamerConfigProperties,
            RDFServiceFactory uniparcRdfServiceFactory) {
        return new RDFStreamer(
                uniparcRDFStreamerConfigProperties.getBatchSize(),
                prologProvider,
                uniparcRdfServiceFactory,
                RDFStreamConfig.rdfRetryPolicy(uniparcRDFStreamerConfigProperties));
    }

    @Bean
    public RDFServiceFactory uniparcRdfServiceFactory(RestTemplate uniparcRdfRestTemplate) {
        return new RDFServiceFactory(uniparcRdfRestTemplate, tagProvider);
    }

    @Bean
    public RestTemplate uniparcRdfRestTemplate(
            RDFStreamerConfigProperties uniparcRDFStreamerConfigProperties) {
        ClientHttpRequestFactory factory =
                new BufferingClientHttpRequestFactory(new SimpleClientHttpRequestFactory());
        RestTemplate restTemplate = new RestTemplate(factory);
        restTemplate.setInterceptors(
                Collections.singletonList(new RequestResponseLoggingInterceptor()));
        restTemplate.setUriTemplateHandler(
                new DefaultUriBuilderFactory(
                        uniparcRDFStreamerConfigProperties.getRequestUrl()));
        return restTemplate;
    }

    @Bean
    @ConfigurationProperties(prefix = "uniparc.rdf.streamer")
    public RDFStreamerConfigProperties uniparcRDFStreamerConfigProperties() {
        return new RDFStreamerConfigProperties();
    }
}
