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
import org.uniprot.api.rest.service.TagProvider;

import java.util.Collections;

@Configuration
public class SupportDataRDFStreamerConfig {
    private final PrologProvider prologProvider;
    private final TagProvider tagProvider;

    public SupportDataRDFStreamerConfig(PrologProvider prologProvider, TagProvider tagProvider) {
        this.prologProvider = prologProvider;
        this.tagProvider = tagProvider;
    }

    @Bean
    public RDFStreamer supportDataRdfStreamer(
            RDFStreamerConfigProperties supportDataRDFStreamerConfigProperties,
            RDFServiceFactory supportDataRdfServiceFactory) {
        return new RDFStreamer(
                supportDataRDFStreamerConfigProperties.getBatchSize(),
                prologProvider,
                supportDataRdfServiceFactory,
                RDFStreamConfig.rdfRetryPolicy(supportDataRDFStreamerConfigProperties));
    }

    @Bean
    public RDFServiceFactory supportDataRdfServiceFactory(RestTemplate supportDataRdfRestTemplate) {
        return new RDFServiceFactory(supportDataRdfRestTemplate, tagProvider);
    }

    @Bean
    public RestTemplate supportDataRdfRestTemplate(
            RDFStreamerConfigProperties supportDataRDFStreamerConfigProperties) {
        ClientHttpRequestFactory factory =
                new BufferingClientHttpRequestFactory(new SimpleClientHttpRequestFactory());
        RestTemplate restTemplate = new RestTemplate(factory);
        restTemplate.setInterceptors(
                Collections.singletonList(new RequestResponseLoggingInterceptor()));
        restTemplate.setUriTemplateHandler(
                new DefaultUriBuilderFactory(
                        supportDataRDFStreamerConfigProperties.getRequestUrl()));
        return restTemplate;
    }

    @Bean
    @ConfigurationProperties(prefix = "support.data.rdf.streamer")
    public RDFStreamerConfigProperties supportDataRDFStreamerConfigProperties() {
        return new RDFStreamerConfigProperties();
    }
}
