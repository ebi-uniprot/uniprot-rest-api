package org.uniprot.api.uniparc.common.repository.rdf;

import java.util.Collections;

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

@Configuration
public class UniParcRdfStreamerConfig {
    private final PrologProvider prologProvider;
    private final TagPositionProvider tagPositionProvider;
    private final RdfEntryCountProvider rdfEntryCountProvider;

    public UniParcRdfStreamerConfig(
            PrologProvider prologProvider,
            TagPositionProvider tagPositionProvider,
            RdfEntryCountProvider rdfEntryCountProvider) {
        this.prologProvider = prologProvider;
        this.tagPositionProvider = tagPositionProvider;
        this.rdfEntryCountProvider = rdfEntryCountProvider;
    }

    @Bean
    public RdfStreamer uniparcRdfStreamer(
            RdfStreamerConfigProperties uniparcRdfStreamerConfigProperties,
            RdfServiceFactory uniparcRdfServiceFactory) {
        return new RdfStreamer(
                uniparcRdfStreamerConfigProperties.getBatchSize(),
                prologProvider,
                uniparcRdfServiceFactory,
                RdfStreamConfig.rdfRetryPolicy(uniparcRdfStreamerConfigProperties),
                rdfEntryCountProvider);
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
                new DefaultUriBuilderFactory(uniparcRdfStreamerConfigProperties.getRequestUrl()));
        return restTemplate;
    }

    @Bean
    @ConfigurationProperties(prefix = "uniparc.rdf.streamer")
    public RdfStreamerConfigProperties uniparcRdfStreamerConfigProperties() {
        return new RdfStreamerConfigProperties();
    }
}
