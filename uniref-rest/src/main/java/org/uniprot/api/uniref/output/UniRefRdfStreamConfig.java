package org.uniprot.api.uniref.output;

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
public class UniRefRdfStreamConfig {
    private final PrologProvider prologProvider;
    private final TagProvider tagProvider;

    public UniRefRdfStreamConfig(PrologProvider prologProvider, TagProvider tagProvider) {
        this.prologProvider = prologProvider;
        this.tagProvider = tagProvider;
    }

    @Bean
    public RDFStreamer unirefRdfXmlStreamer(RDFStreamerConfigProperties unirefRDFStreamerConfigProperties, RestTemplate unirefRdfRestTemplate) {
        return new RDFStreamer(
                unirefRDFStreamerConfigProperties.getBatchSize(),
                prologProvider,
                new RDFXMLClient(tagProvider, unirefRdfRestTemplate),
                RDFStreamConfig.rdfRetryPolicy(unirefRDFStreamerConfigProperties));
    }

    @Bean
    public RestTemplate unirefRdfRestTemplate(RDFStreamerConfigProperties unirefRDFStreamerConfigProperties) {
        ClientHttpRequestFactory factory = new BufferingClientHttpRequestFactory(new SimpleClientHttpRequestFactory());
        RestTemplate restTemplate = new RestTemplate(factory);
        restTemplate.setInterceptors(Collections.singletonList(new RequestResponseLoggingInterceptor()));
        restTemplate.setUriTemplateHandler(new DefaultUriBuilderFactory(unirefRDFStreamerConfigProperties.getRequestUrl()));
        return restTemplate;
    }

    @Bean
    @ConfigurationProperties(prefix = "uniref.rdf.streamer")
    public RDFStreamerConfigProperties unirefRDFStreamerConfigProperties() {
        return new RDFStreamerConfigProperties();
    }
}
