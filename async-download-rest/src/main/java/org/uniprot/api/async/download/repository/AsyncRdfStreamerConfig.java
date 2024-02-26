package org.uniprot.api.async.download.repository;

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
public class AsyncRdfStreamerConfig {
    private final PrologProvider prologProvider;
    private final TagPositionProvider tagPositionProvider;
    private final RdfEntryCountProvider rdfEntryCountProvider;

    public AsyncRdfStreamerConfig(
            PrologProvider prologProvider,
            TagPositionProvider tagPositionProvider,
            RdfEntryCountProvider rdfEntryCountProvider) {
        this.prologProvider = prologProvider;
        this.tagPositionProvider = tagPositionProvider;
        this.rdfEntryCountProvider = rdfEntryCountProvider;
    }

    @Bean
    public RdfStreamer asyncRdfStreamer(
            RdfStreamerConfigProperties asyncRdfStreamerConfigProperties,
            RdfServiceFactory asyncRdfServiceFactory) {
        return new RdfStreamer(
                asyncRdfStreamerConfigProperties.getBatchSize(),
                prologProvider,
                asyncRdfServiceFactory,
                RdfStreamConfig.rdfRetryPolicy(asyncRdfStreamerConfigProperties),
                rdfEntryCountProvider);
    }

    @Bean
    public RdfServiceFactory asyncRdfServiceFactory(RestTemplate asyncRdfRestTemplate) {
        return new RdfServiceFactory(asyncRdfRestTemplate, tagPositionProvider);
    }

    @Bean
    public RestTemplate asyncRdfRestTemplate(
            RdfStreamerConfigProperties asyncRdfStreamerConfigProperties) {
        ClientHttpRequestFactory factory =
                new BufferingClientHttpRequestFactory(new SimpleClientHttpRequestFactory());
        RestTemplate restTemplate = new RestTemplate(factory);
        restTemplate.setInterceptors(
                Collections.singletonList(new RequestResponseLoggingInterceptor()));
        restTemplate.setUriTemplateHandler(
                new DefaultUriBuilderFactory(asyncRdfStreamerConfigProperties.getRequestUrl()));
        return restTemplate;
    }

    @Bean
    @ConfigurationProperties(prefix = "async.rdf.streamer")
    public RdfStreamerConfigProperties asyncRdfStreamerConfigProperties() {
        return new RdfStreamerConfigProperties();
    }
}
