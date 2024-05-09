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
    public RdfStreamer uniParcRdfStreamer(
            RdfStreamerConfigProperties uniParcRdfStreamerConfigProperties,
            RdfServiceFactory uniParcRdfServiceFactory) {
        return new RdfStreamer(
                uniParcRdfStreamerConfigProperties.getBatchSize(),
                prologProvider,
                uniParcRdfServiceFactory,
                RdfStreamConfig.rdfRetryPolicy(uniParcRdfStreamerConfigProperties),
                rdfEntryCountProvider);
    }

    @Bean
    public RdfServiceFactory uniParcRdfServiceFactory(RestTemplate uniParcRdfRestTemplate) {
        return new RdfServiceFactory(uniParcRdfRestTemplate, tagPositionProvider);
    }

    @Bean
    public RestTemplate uniParcRdfRestTemplate(
            RdfStreamerConfigProperties uniParcRdfStreamerConfigProperties) {
        ClientHttpRequestFactory factory =
                new BufferingClientHttpRequestFactory(new SimpleClientHttpRequestFactory());
        RestTemplate restTemplate = new RestTemplate(factory);
        restTemplate.setInterceptors(
                Collections.singletonList(new RequestResponseLoggingInterceptor()));
        restTemplate.setUriTemplateHandler(
                new DefaultUriBuilderFactory(uniParcRdfStreamerConfigProperties.getRequestUrl()));
        return restTemplate;
    }

    @Bean
    @ConfigurationProperties(prefix = "uniparc.rdf.streamer")
    public RdfStreamerConfigProperties uniParcRdfStreamerConfigProperties() {
        return new RdfStreamerConfigProperties();
    }
}
