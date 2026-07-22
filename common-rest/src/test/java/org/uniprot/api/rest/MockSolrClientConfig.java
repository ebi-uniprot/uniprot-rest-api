package org.uniprot.api.rest;

import static org.mockito.Mockito.mock;

import java.net.URISyntaxException;
import java.net.http.HttpClient;

import org.apache.solr.client.solrj.SolrClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

/**
 * This used {@code @Primary} hence should only be used in unit tests where solr test container is not needed.
 * <p>
 * Example usage:
 * <p>
 * {@code @SpringBootTest(classes = { MockSolrClientConfig.class }, properties = "spring.main.allow-bean-definition-overriding=true")}
 * <p>
 * Note that {@code spring.main.allow-bean-definition-overriding=true} is required to override respective live beans
 * with test beans.
 */
public class MockSolrClientConfig {

    @Bean("uniProtKBSolrClient")
    @Primary
    public SolrClient uniProtKBSolrClient() throws URISyntaxException {
        return mock(SolrClient.class);
    }

    @Bean
    @Primary
    public SolrClient solrClient() throws URISyntaxException {
        return mock(SolrClient.class);
    }

    @Bean
    @Primary
    public HttpClient httpClient() {
        return mock(HttpClient.class);
    }
}
