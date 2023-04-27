package org.uniprot.api.uniprotkb.repository.store;

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
public class UniProtRDFStreamerConfig {
    private final PrologProvider prologProvider;
    private final TagPositionProvider tagPositionProvider;

    public UniProtRDFStreamerConfig(PrologProvider prologProvider, TagPositionProvider tagPositionProvider) {
        this.prologProvider = prologProvider;
        this.tagPositionProvider = tagPositionProvider;
    }

    @Bean
    public RDFStreamer uniprotRdfStreamer(
            RDFStreamerConfigProperties uniprotRDFStreamerConfigProperties,
            RDFServiceFactory uniprotRdfServiceFactory) {
        return new RDFStreamer(
                uniprotRDFStreamerConfigProperties.getBatchSize(),
                prologProvider,
                uniprotRdfServiceFactory,
                RDFStreamConfig.rdfRetryPolicy(uniprotRDFStreamerConfigProperties));
    }

    @Bean
    public RDFServiceFactory uniprotRdfServiceFactory(RestTemplate uniprotRdfRestTemplate) {
        return new RDFServiceFactory(uniprotRdfRestTemplate, tagPositionProvider);
    }

    @Bean
    public RestTemplate uniprotRdfRestTemplate(
            RDFStreamerConfigProperties uniprotRDFStreamerConfigProperties) {
        ClientHttpRequestFactory factory =
                new BufferingClientHttpRequestFactory(new SimpleClientHttpRequestFactory());
        RestTemplate restTemplate = new RestTemplate(factory);
        restTemplate.setInterceptors(
                Collections.singletonList(new RequestResponseLoggingInterceptor()));
        restTemplate.setUriTemplateHandler(
                new DefaultUriBuilderFactory(
                        uniprotRDFStreamerConfigProperties.getRequestUrl()));
        return restTemplate;
    }

    @Bean
    @ConfigurationProperties(prefix = "uniprot.rdf.streamer")
    public RDFStreamerConfigProperties uniprotRDFStreamerConfigProperties() {
        return new RDFStreamerConfigProperties();
    }
}
