package org.uniprot.api.proteome.rdf;

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
public class ProteomeRdfStreamerConfig {
    private final PrologProvider prologProvider;
    private final TagPositionProvider tagPositionProvider;
    private final RdfEntryCountProvider rdfEntryCountProvider;

    public ProteomeRdfStreamerConfig(
            PrologProvider prologProvider,
            TagPositionProvider tagPositionProvider,
            RdfEntryCountProvider rdfEntryCountProvider) {
        this.prologProvider = prologProvider;
        this.tagPositionProvider = tagPositionProvider;
        this.rdfEntryCountProvider = rdfEntryCountProvider;
    }

    @Bean
    public RdfStreamer proteomeRdfStreamer(
            RdfStreamerConfigProperties proteomeRdfStreamerConfigProperties,
            RdfServiceFactory proteomeRdfServiceFactory) {
        return new RdfStreamer(
                proteomeRdfStreamerConfigProperties.getBatchSize(),
                prologProvider,
                proteomeRdfServiceFactory,
                RdfStreamConfig.rdfRetryPolicy(proteomeRdfStreamerConfigProperties),
                rdfEntryCountProvider);
    }

    @Bean
    public RdfServiceFactory proteomeRdfServiceFactory(RestTemplate proteomeRdfRestTemplate) {
        return new RdfServiceFactory(proteomeRdfRestTemplate, tagPositionProvider);
    }

    @Bean
    public RestTemplate proteomeRdfRestTemplate(
            RdfStreamerConfigProperties proteomeStreamerConfigProperties) {
        ClientHttpRequestFactory factory =
                new BufferingClientHttpRequestFactory(new SimpleClientHttpRequestFactory());
        RestTemplate restTemplate = new RestTemplate(factory);
        restTemplate.setInterceptors(
                Collections.singletonList(new RequestResponseLoggingInterceptor()));
        restTemplate.setUriTemplateHandler(
                new DefaultUriBuilderFactory(proteomeStreamerConfigProperties.getRequestUrl()));
        return restTemplate;
    }

    @Bean
    @ConfigurationProperties(prefix = "proteomes.rdf.streamer")
    public RdfStreamerConfigProperties proteomeStreamerConfigProperties() {
        return new RdfStreamerConfigProperties();
    }
}
