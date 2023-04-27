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
import org.uniprot.api.rest.service.TagPositionProvider;

import java.util.Collections;

@Configuration
public class UniParcRdfStreamerConfig {
    private final PrologProvider prologProvider;
    private final TagPositionProvider tagPositionProvider;

    public UniParcRdfStreamerConfig(PrologProvider prologProvider, TagPositionProvider tagPositionProvider) {
        this.prologProvider = prologProvider;
        this.tagPositionProvider = tagPositionProvider;
    }

    @Bean
    public RdfStreamer uniparcRdfStreamer(
            RdfStreamerConfigProperties uniparcRdfStreamerConfigProperties,
            RdfServiceFactory uniparcRdfServiceFactory) {
        return new RdfStreamer(
                uniparcRdfStreamerConfigProperties.getBatchSize(),
                prologProvider,
                uniparcRdfServiceFactory,
                RdfStreamConfig.rdfRetryPolicy(uniparcRdfStreamerConfigProperties));
    }

    @Bean
    public RdfServiceFactory uniparcRdfServiceFactory(RestTemplate uniparcRdfRestTemplate) {
        return new RdfServiceFactory(uniparcRdfRestTemplate, tagPositionProvider);
    }

    @Bean
    public RestTemplate uniparcRdfRestTemplate(
            RdfStreamerConfigProperties uniparcRdfStreamerConfigProperties) {
        ClientHttpRequestFactory factory =
                new BufferingClientHttpRequestFactory(new SimpleClientHttpRequestFactory());
        RestTemplate restTemplate = new RestTemplate(factory);
        restTemplate.setInterceptors(
                Collections.singletonList(new RequestResponseLoggingInterceptor()));
        restTemplate.setUriTemplateHandler(
                new DefaultUriBuilderFactory(
                        uniparcRdfStreamerConfigProperties.getRequestUrl()));
        return restTemplate;
    }

    @Bean
    @ConfigurationProperties(prefix = "uniparc.rdf.streamer")
    public RdfStreamerConfigProperties uniparcRdfStreamerConfigProperties() {
        return new RdfStreamerConfigProperties();
    }
}
