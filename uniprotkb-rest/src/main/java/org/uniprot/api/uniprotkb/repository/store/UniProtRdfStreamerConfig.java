package org.uniprot.api.uniprotkb.repository.store;

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
public class UniProtRdfStreamerConfig {
    private final PrologProvider prologProvider;
    private final TagPositionProvider tagPositionProvider;

    public UniProtRdfStreamerConfig(
            PrologProvider prologProvider, TagPositionProvider tagPositionProvider) {
        this.prologProvider = prologProvider;
        this.tagPositionProvider = tagPositionProvider;
    }

    @Bean
    public RdfStreamer uniProtRdfStreamer(
            RdfStreamerConfigProperties uniProtRdfStreamerConfigProperties,
            RdfServiceFactory uniProtRdfServiceFactory) {
        return new RdfStreamer(
                uniProtRdfStreamerConfigProperties.getBatchSize(),
                prologProvider,
                uniProtRdfServiceFactory,
                RdfStreamConfig.rdfRetryPolicy(uniProtRdfStreamerConfigProperties));
    }

    @Bean
    public RdfServiceFactory uniProtRdfServiceFactory(RestTemplate uniProtRdfRestTemplate) {
        return new RdfServiceFactory(uniProtRdfRestTemplate, tagPositionProvider);
    }

    @Bean
    public RestTemplate uniProtRdfRestTemplate(
            RdfStreamerConfigProperties uniProtRdfStreamerConfigProperties) {
        ClientHttpRequestFactory factory =
                new BufferingClientHttpRequestFactory(new SimpleClientHttpRequestFactory());
        RestTemplate restTemplate = new RestTemplate(factory);
        restTemplate.setInterceptors(
                Collections.singletonList(new RequestResponseLoggingInterceptor()));
        restTemplate.setUriTemplateHandler(
                new DefaultUriBuilderFactory(uniProtRdfStreamerConfigProperties.getRequestUrl()));
        return restTemplate;
    }

    @Bean
    @ConfigurationProperties(prefix = "uniprot.rdf.streamer")
    public RdfStreamerConfigProperties uniProtRdfStreamerConfigProperties() {
        return new RdfStreamerConfigProperties();
    }
}
