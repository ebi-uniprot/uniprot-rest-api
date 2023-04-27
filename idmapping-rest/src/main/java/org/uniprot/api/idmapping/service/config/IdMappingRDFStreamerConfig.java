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
public class IdMappingRDFStreamerConfig {
    private final PrologProvider prologProvider;
    private final TagPositionProvider tagPositionProvider;

    public IdMappingRDFStreamerConfig(PrologProvider prologProvider, TagPositionProvider tagPositionProvider) {
        this.prologProvider = prologProvider;
        this.tagPositionProvider = tagPositionProvider;
    }

    @Bean
    public RDFStreamer idMappingRdfStreamer(
            RDFStreamerConfigProperties idMappingRDFStreamerConfigProperties,
            RDFServiceFactory idMappingRdfServiceFactory) {
        return new RDFStreamer(
                idMappingRDFStreamerConfigProperties.getBatchSize(),
                prologProvider,
                idMappingRdfServiceFactory,
                RDFStreamConfig.rdfRetryPolicy(idMappingRDFStreamerConfigProperties));
    }

    @Bean
    public RDFServiceFactory idMappingRdfServiceFactory(RestTemplate idMappingRdfRestTemplate) {
        return new RDFServiceFactory(idMappingRdfRestTemplate, tagPositionProvider);
    }

    @Bean
    public RestTemplate idMappingRdfRestTemplate(
            RDFStreamerConfigProperties idMappingRDFStreamerConfigProperties) {
        ClientHttpRequestFactory factory =
                new BufferingClientHttpRequestFactory(new SimpleClientHttpRequestFactory());
        RestTemplate restTemplate = new RestTemplate(factory);
        restTemplate.setInterceptors(
                Collections.singletonList(new RequestResponseLoggingInterceptor()));
        restTemplate.setUriTemplateHandler(
                new DefaultUriBuilderFactory(idMappingRDFStreamerConfigProperties.getRequestUrl()));
        return restTemplate;
    }

    @Bean
    @ConfigurationProperties(prefix = "id.mapping.rdf.streamer")
    public RDFStreamerConfigProperties idMappingRDFStreamerConfigProperties() {
        return new RDFStreamerConfigProperties();
    }
}
