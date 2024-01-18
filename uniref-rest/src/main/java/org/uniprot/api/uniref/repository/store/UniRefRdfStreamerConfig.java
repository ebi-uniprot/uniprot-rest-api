package org.uniprot.api.uniref.repository.store;

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
public class UniRefRdfStreamerConfig {
    private final PrologProvider prologProvider;
    private final TagPositionProvider tagPositionProvider;
    private final RdfEntryCountProvider rdfEntryCountProvider;

    public UniRefRdfStreamerConfig(
            PrologProvider prologProvider,
            TagPositionProvider tagPositionProvider,
            RdfEntryCountProvider rdfEntryCountProvider) {
        this.prologProvider = prologProvider;
        this.tagPositionProvider = tagPositionProvider;
        this.rdfEntryCountProvider = rdfEntryCountProvider;
    }

    @Bean
    public RdfStreamer unirefRdfStreamer(
            RdfStreamerConfigProperties unirefRdfStreamerConfigProperties,
            RdfServiceFactory unirefRdfServiceFactory) {
        return new RdfStreamer(
                unirefRdfStreamerConfigProperties.getBatchSize(),
                prologProvider,
                unirefRdfServiceFactory,
                RdfStreamConfig.rdfRetryPolicy(unirefRdfStreamerConfigProperties),
                rdfEntryCountProvider);
    }

    @Bean
    public RdfServiceFactory unirefRdfServiceFactory(RestTemplate unirefRdfRestTemplate) {
        return new RdfServiceFactory(unirefRdfRestTemplate, tagPositionProvider);
    }

    @Bean
    public RestTemplate unirefRdfRestTemplate(
            RdfStreamerConfigProperties unirefRdfStreamerConfigProperties) {
        ClientHttpRequestFactory factory =
                new BufferingClientHttpRequestFactory(new SimpleClientHttpRequestFactory());
        RestTemplate restTemplate = new RestTemplate(factory);
        restTemplate.setInterceptors(
                Collections.singletonList(new RequestResponseLoggingInterceptor()));
        restTemplate.setUriTemplateHandler(
                new DefaultUriBuilderFactory(unirefRdfStreamerConfigProperties.getRequestUrl()));
        return restTemplate;
    }

    @Bean
    @ConfigurationProperties(prefix = "uniref.rdf.streamer")
    public RdfStreamerConfigProperties unirefRdfStreamerConfigProperties() {
        return new RdfStreamerConfigProperties();
    }
}
