package org.uniprot.api.uniparc.output;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.BufferingClientHttpRequestFactory;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.DefaultUriBuilderFactory;
import org.uniprot.api.common.repository.stream.rdf.PrologProvider;
import org.uniprot.api.common.repository.stream.rdf.RDFStreamConfig;
import org.uniprot.api.common.repository.stream.rdf.RDFStreamer;
import org.uniprot.api.common.repository.stream.rdf.RDFStreamerConfigProperties;
import org.uniprot.api.rest.output.RequestResponseLoggingInterceptor;
import org.uniprot.api.rest.service.RDFXMLClient;
import org.uniprot.api.rest.service.TagProvider;

import java.util.Collections;

@Configuration
public class UniparcStreamConfig {
    private final PrologProvider prologProvider;
    private final TagProvider tagProvider;

    public UniparcStreamConfig(PrologProvider prologProvider, TagProvider tagProvider) {
        this.prologProvider = prologProvider;
        this.tagProvider = tagProvider;
    }

    @Bean
    public RDFStreamer uniparcRdfXmlStreamer(RDFStreamerConfigProperties uniparcRDFStreamerConfigProperties, RestTemplate uniparcRdfRestTemplate) {
        return new RDFStreamer(
                uniparcRDFStreamerConfigProperties.getBatchSize(),
                prologProvider,
                new RDFXMLClient(tagProvider, uniparcRdfRestTemplate),
                RDFStreamConfig.rdfRetryPolicy(uniparcRDFStreamerConfigProperties));
    }

    @Bean
    public RestTemplate uniparcRdfRestTemplate(RDFStreamerConfigProperties uniparcRDFStreamerConfigProperties) {
        ClientHttpRequestFactory factory = new BufferingClientHttpRequestFactory(new SimpleClientHttpRequestFactory());
        RestTemplate restTemplate = new RestTemplate(factory);
        restTemplate.setInterceptors(Collections.singletonList(new RequestResponseLoggingInterceptor()));
        restTemplate.setUriTemplateHandler(new DefaultUriBuilderFactory(uniparcRDFStreamerConfigProperties.getRequestUrl()));
        return restTemplate;
    }

    @Bean
    @ConfigurationProperties(prefix = "uniparc.rdf.streamer")
    public RDFStreamerConfigProperties uniparcRDFStreamerConfigProperties() {
        return new RDFStreamerConfigProperties();
    }
}
