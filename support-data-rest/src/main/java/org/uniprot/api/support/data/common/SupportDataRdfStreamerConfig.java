package org.uniprot.api.support.data.common;

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
public class SupportDataRdfStreamerConfig {
    private final PrologProvider prologProvider;
    private final TagPositionProvider tagPositionProvider;
    private final RdfEntryCountProvider rdfEntryCountProvider;

    public SupportDataRdfStreamerConfig(
            PrologProvider prologProvider, TagPositionProvider tagPositionProvider, RdfEntryCountProvider rdfEntryCountProvider) {
        this.prologProvider = prologProvider;
        this.tagPositionProvider = tagPositionProvider;
        this.rdfEntryCountProvider = rdfEntryCountProvider;
    }

    @Bean
    public RdfStreamer supportDataRdfStreamer(
            RdfStreamerConfigProperties supportDataRdfStreamerConfigProperties,
            RdfServiceFactory supportDataRdfServiceFactory) {
        return new RdfStreamer(
                supportDataRdfStreamerConfigProperties.getBatchSize(),
                prologProvider,
                supportDataRdfServiceFactory,
                RdfStreamConfig.rdfRetryPolicy(supportDataRdfStreamerConfigProperties), rdfEntryCountProvider);
    }

    @Bean
    public RdfServiceFactory supportDataRdfServiceFactory(RestTemplate supportDataRdfRestTemplate) {
        return new RdfServiceFactory(supportDataRdfRestTemplate, tagPositionProvider);
    }

    @Bean
    public RestTemplate supportDataRdfRestTemplate(
            RdfStreamerConfigProperties supportDataRdfStreamerConfigProperties) {
        ClientHttpRequestFactory factory =
                new BufferingClientHttpRequestFactory(new SimpleClientHttpRequestFactory());
        RestTemplate restTemplate = new RestTemplate(factory);
        restTemplate.setInterceptors(
                Collections.singletonList(new RequestResponseLoggingInterceptor()));
        restTemplate.setUriTemplateHandler(
                new DefaultUriBuilderFactory(
                        supportDataRdfStreamerConfigProperties.getRequestUrl()));
        return restTemplate;
    }

    @Bean
    @ConfigurationProperties(prefix = "support.data.rdf.streamer")
    public RdfStreamerConfigProperties supportDataRdfStreamerConfigProperties() {
        return new RdfStreamerConfigProperties();
    }
}
