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

import java.util.Collections;

@Configuration
public class RDFXMLStreamerConfig {
    @Bean
    public RetryPolicy<Object> rdfFetchRetryPolicy(RDFXMLStreamerConfigProperties rdfXmlStreamerConfigProperties) {
        return RDFStreamConfig.rdfRetryPolicy(rdfXmlStreamerConfigProperties);
    }

    @Bean
    public RestTemplate rdfXmlRestTemplate(RDFXMLStreamerConfigProperties rdfXmlStreamerConfigProperties) {
        ClientHttpRequestFactory factory =
                new BufferingClientHttpRequestFactory(new SimpleClientHttpRequestFactory());
        RestTemplate restTemplate = new RestTemplate(factory);
        restTemplate.setInterceptors(
                Collections.singletonList(new RequestResponseLoggingInterceptor()));
        restTemplate.setUriTemplateHandler(
                new DefaultUriBuilderFactory(rdfXmlStreamerConfigProperties.getRequestUrl()));
        return restTemplate;
    }

    @Bean
    @ConfigurationProperties(prefix = "rdf.streamer")
    public RDFXMLStreamerConfigProperties rdfXmlStreamerConfigProperties() {
        return new RDFXMLStreamerConfigProperties();
    }
}
