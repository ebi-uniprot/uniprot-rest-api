package org.uniprot.api.idmapping.output;

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
public class IdMappingStreamConfig {
    private final PrologProvider prologProvider;
    private final TagProvider tagProvider;

    public IdMappingStreamConfig(PrologProvider prologProvider, TagProvider tagProvider) {
        this.prologProvider = prologProvider;
        this.tagProvider = tagProvider;
    }

    @Bean
    public RDFStreamer idMappingRdfXmlStreamer(RDFStreamerConfigProperties idMappingRDFStreamerConfigProperties, RestTemplate idMappingRdfRestTemplate) {
        return new RDFStreamer(
                idMappingRDFStreamerConfigProperties.getBatchSize(),
                prologProvider,
                new RDFXMLClient(tagProvider, idMappingRdfRestTemplate),
                RDFStreamConfig.rdfRetryPolicy(idMappingRDFStreamerConfigProperties));
    }

    @Bean
    public RestTemplate idMappingRdfRestTemplate(RDFStreamerConfigProperties idMappingRDFStreamerConfigProperties) {
        ClientHttpRequestFactory factory = new BufferingClientHttpRequestFactory(new SimpleClientHttpRequestFactory());
        RestTemplate restTemplate = new RestTemplate(factory);
        restTemplate.setInterceptors(Collections.singletonList(new RequestResponseLoggingInterceptor()));
        restTemplate.setUriTemplateHandler(new DefaultUriBuilderFactory(idMappingRDFStreamerConfigProperties.getRequestUrl()));
        return restTemplate;
    }

    @Bean
    @ConfigurationProperties(prefix = "id.mapping.rdf.streamer")
    public RDFStreamerConfigProperties idMappingRDFStreamerConfigProperties() {
        return new RDFStreamerConfigProperties();
    }
}
