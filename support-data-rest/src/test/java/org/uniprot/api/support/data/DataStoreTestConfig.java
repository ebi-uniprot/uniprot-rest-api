package org.uniprot.api.support.data;

import org.apache.http.client.HttpClient;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.request.json.JsonQueryRequest;
import org.apache.solr.common.params.ModifiableSolrParams;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;
import org.springframework.web.client.RestTemplate;
import org.uniprot.api.common.repository.search.SolrRequest;
import org.uniprot.api.common.repository.search.SolrRequestConverter;

import static org.mockito.Mockito.mock;

@TestConfiguration
public class DataStoreTestConfig {

    @Bean
    @Profile("offline")
    public HttpClient httpClient() {
        return mock(HttpClient.class);
    }

    @Bean
    @Profile("offline")
    public SolrClient solrClient() {
        return mock(SolrClient.class);
    }

    @Bean
    @Profile("offline")
    public SolrRequestConverter requestConverter() {
        return new SolrRequestConverter() {
            @Override
            public JsonQueryRequest toJsonQueryRequest(SolrRequest request) {
                JsonQueryRequest solrQuery = super.toJsonQueryRequest(request);

                // required for tests, because EmbeddedSolrServer is not sharded
                ((ModifiableSolrParams) solrQuery.getParams()).set("distrib", "false");
                ((ModifiableSolrParams) solrQuery.getParams()).set("terms.mincount", "1");

                return solrQuery;
            }
        };
    }

    @Bean(name = "xrefRDFRestTemplate")
    @Profile("offline")
    public RestTemplate xrefRDFRestTemplate() {
        return mock(RestTemplate.class);
    }

    @Bean(name = "diseaseRDFRestTemplate")
    @Profile("offline")
    public RestTemplate diseaseRDFRestTemplate() {
        return mock(RestTemplate.class);
    }

    @Bean(name = "keywordRDFRestTemplate")
    @Profile("offline")
    public RestTemplate keywordRDFRestTemplate() {
        return mock(RestTemplate.class);
    }

    @Bean(name = "taxonomyRDFRestTemplate")
    @Profile("offline")
    public RestTemplate taxonomyRDFRestTemplate() {
        return mock(RestTemplate.class);
    }

    @Bean(name = "locationRDFRestTemplate")
    @Profile("offline")
    public RestTemplate locationRDFRestTemplate() {
        return mock(RestTemplate.class);
    }

    @Bean(name = "literatureRDFRestTemplate")
    @Profile("offline")
    public RestTemplate literaturesRDFRestTemplate() {
        return mock(RestTemplate.class);
    }
}
