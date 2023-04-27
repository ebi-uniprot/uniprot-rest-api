package org.uniprot.api.uniref.repository.store;

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
public class UniRefRDFStreamerConfig {
    private final PrologProvider prologProvider;
    private final TagPositionProvider tagPositionProvider;

    public UniRefRDFStreamerConfig(PrologProvider prologProvider, TagPositionProvider tagPositionProvider) {
        this.prologProvider = prologProvider;
        this.tagPositionProvider = tagPositionProvider;
    }

    @Bean
    public RDFStreamer unirefRdfStreamer(
            RDFStreamerConfigProperties unirefRDFStreamerConfigProperties,
            RDFServiceFactory unirefRdfServiceFactory) {
        return new RDFStreamer(
                unirefRDFStreamerConfigProperties.getBatchSize(),
                prologProvider,
                unirefRdfServiceFactory,
                RDFStreamConfig.rdfRetryPolicy(unirefRDFStreamerConfigProperties));
    }

    @Bean
    public RDFServiceFactory unirefRdfServiceFactory(RestTemplate unirefRdfRestTemplate) {
        return new RDFServiceFactory(unirefRdfRestTemplate, tagPositionProvider);
    }

    @Bean
    public RestTemplate unirefRdfRestTemplate(
            RDFStreamerConfigProperties unirefRDFStreamerConfigProperties) {
        ClientHttpRequestFactory factory =
                new BufferingClientHttpRequestFactory(new SimpleClientHttpRequestFactory());
        RestTemplate restTemplate = new RestTemplate(factory);
        restTemplate.setInterceptors(
                Collections.singletonList(new RequestResponseLoggingInterceptor()));
        restTemplate.setUriTemplateHandler(
                new DefaultUriBuilderFactory(
                        unirefRDFStreamerConfigProperties.getRequestUrl()));
        return restTemplate;
    }

    @Bean
    @ConfigurationProperties(prefix = "uniref.rdf.streamer")
    public RDFStreamerConfigProperties unirefRDFStreamerConfigProperties() {
        return new RDFStreamerConfigProperties();
    }
}
