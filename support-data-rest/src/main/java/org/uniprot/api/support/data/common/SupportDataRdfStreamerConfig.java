package org.uniprot.api.support.data.common;

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
public class SupportDataRdfStreamerConfig {
    private final PrologProvider prologProvider;
    private final TagPositionProvider tagPositionProvider;

    public SupportDataRdfStreamerConfig(
            PrologProvider prologProvider, TagPositionProvider tagPositionProvider) {
        this.prologProvider = prologProvider;
        this.tagPositionProvider = tagPositionProvider;
    }

    @Bean
    public RdfStreamer supportDataRdfStreamer(
            RdfStreamerConfigProperties supportDataRdfStreamerConfigProperties,
            RdfServiceFactory supportDataRdfServiceFactory) {
        return new RdfStreamer(
                supportDataRdfStreamerConfigProperties.getBatchSize(),
                prologProvider,
                supportDataRdfServiceFactory,
                RdfStreamConfig.rdfRetryPolicy(supportDataRdfStreamerConfigProperties));
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
