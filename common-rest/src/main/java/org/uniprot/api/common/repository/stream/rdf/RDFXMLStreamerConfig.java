package org.uniprot.api.common.repository.stream.rdf;

import net.jodah.failsafe.RetryPolicy;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.BufferingClientHttpRequestFactory;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.DefaultUriBuilderFactory;
import org.uniprot.api.rest.output.RequestResponseLoggingInterceptor;
import org.uniprot.api.rest.service.RDFXMLClient;
import org.uniprot.api.rest.service.TagProvider;

import java.util.Collections;

@Configuration
public class RDFXMLStreamerConfig {
    private final PrologProvider prologProvider;
    private final TagProvider tagProvider;

    public RDFXMLStreamerConfig(PrologProvider prologProvider, TagProvider tagProvider) {
        this.prologProvider = prologProvider;
        this.tagProvider = tagProvider;
    }

    @Bean
    public RDFStreamer supportDataRdfXmlStreamer(RDFXMLStreamerConfigProperties supportDataRDFXMLStreamerConfigProperties, RestTemplate supportDataRdfRestTemplate) {
        return new RDFStreamer(
                supportDataRDFXMLStreamerConfigProperties.getBatchSize(),
                prologProvider,
                new RDFXMLClient(tagProvider, supportDataRdfRestTemplate),
                getRdfFetchRetryPolicy(supportDataRDFXMLStreamerConfigProperties));
    }

    @Bean
    public RestTemplate supportDataRdfRestTemplate(RDFXMLStreamerConfigProperties supportDataRDFXMLStreamerConfigProperties) {
        return getRestTemplate(supportDataRDFXMLStreamerConfigProperties);
    }

    private RestTemplate getRestTemplate(RDFXMLStreamerConfigProperties properties) {
        ClientHttpRequestFactory factory = new BufferingClientHttpRequestFactory(new SimpleClientHttpRequestFactory());
        RestTemplate restTemplate = new RestTemplate(factory);
        restTemplate.setInterceptors(Collections.singletonList(new RequestResponseLoggingInterceptor()));
        if (properties.getRequestUrl() != null) {
            restTemplate.setUriTemplateHandler(new DefaultUriBuilderFactory(properties.getRequestUrl()));
        }
        return restTemplate;
    }

    private static RetryPolicy<Object> getRdfFetchRetryPolicy(RDFXMLStreamerConfigProperties properties) {
        return properties.getRetryDelayMillis() > 0 ? RDFStreamConfig.rdfRetryPolicy(properties) : null;
    }

    @Bean
    public RDFStreamer idMappingRdfXmlStreamer(RDFXMLStreamerConfigProperties idMappingRDFXMLStreamerConfigProperties, RestTemplate idMappingRdfRestTemplate) {
        return new RDFStreamer(
                idMappingRDFXMLStreamerConfigProperties.getBatchSize(),
                prologProvider,
                new RDFXMLClient(tagProvider, idMappingRdfRestTemplate),
                getRdfFetchRetryPolicy(idMappingRDFXMLStreamerConfigProperties));
    }

    @Bean
    public RestTemplate idMappingRdfRestTemplate(RDFXMLStreamerConfigProperties idMappingRDFXMLStreamerConfigProperties) {
        return getRestTemplate(idMappingRDFXMLStreamerConfigProperties);
    }

    @Bean
    @ConfigurationProperties(prefix = "support-data.rdf.streamer")
    public RDFXMLStreamerConfigProperties supportDataRDFXMLStreamerConfigProperties() {
        return new RDFXMLStreamerConfigProperties();
    }

    @Bean
    @ConfigurationProperties(prefix = "id-mapping.rdf.streamer")
    public RDFXMLStreamerConfigProperties idMappingRDFXMLStreamerConfigProperties() {
        return new RDFXMLStreamerConfigProperties();
    }
}
