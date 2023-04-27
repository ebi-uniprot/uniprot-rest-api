package org.uniprot.api.idmapping.service.config;

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
public class IdMappingRdfStreamerConfig {
    private final PrologProvider prologProvider;
    private final TagPositionProvider tagPositionProvider;

    public IdMappingRdfStreamerConfig(PrologProvider prologProvider, TagPositionProvider tagPositionProvider) {
        this.prologProvider = prologProvider;
        this.tagPositionProvider = tagPositionProvider;
    }

    @Bean
    public RdfStreamer idMappingRdfStreamer(
            RdfStreamerConfigProperties idMappingRdfStreamerConfigProperties,
            RdfServiceFactory idMappingRdfServiceFactory) {
        return new RdfStreamer(
                idMappingRdfStreamerConfigProperties.getBatchSize(),
                prologProvider,
                idMappingRdfServiceFactory,
                RdfStreamConfig.rdfRetryPolicy(idMappingRdfStreamerConfigProperties));
    }

    @Bean
    public RdfServiceFactory idMappingRdfServiceFactory(RestTemplate idMappingRdfRestTemplate) {
        return new RdfServiceFactory(idMappingRdfRestTemplate, tagPositionProvider);
    }

    @Bean
    public RestTemplate idMappingRdfRestTemplate(
            RdfStreamerConfigProperties idMappingRdfStreamerConfigProperties) {
        ClientHttpRequestFactory factory =
                new BufferingClientHttpRequestFactory(new SimpleClientHttpRequestFactory());
        RestTemplate restTemplate = new RestTemplate(factory);
        restTemplate.setInterceptors(
                Collections.singletonList(new RequestResponseLoggingInterceptor()));
        restTemplate.setUriTemplateHandler(
                new DefaultUriBuilderFactory(idMappingRdfStreamerConfigProperties.getRequestUrl()));
        return restTemplate;
    }

    @Bean
    @ConfigurationProperties(prefix = "id.mapping.rdf.streamer")
    public RdfStreamerConfigProperties idMappingRdfStreamerConfigProperties() {
        return new RdfStreamerConfigProperties();
    }
}
