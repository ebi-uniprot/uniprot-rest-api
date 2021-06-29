package org.uniprot.api.proteome.controller;

import org.apache.http.client.HttpClient;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.request.json.JsonQueryRequest;
import org.mockito.Mockito;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;
import org.uniprot.api.common.repository.search.SolrRequest;
import org.uniprot.api.common.repository.search.SolrRequestConverter;

/**
 * @author jluo
 * @date: 14 Jun 2019
 */
@TestConfiguration
public class DataStoreTestConfig {

    @Bean
    @Profile("offline")
    public HttpClient httpClient() {
        return Mockito.mock(HttpClient.class);
    }

    @Bean()
    @Profile("offline")
    public SolrClient solrClient() {
        return Mockito.mock(SolrClient.class);
    }

    @Bean
    @Profile("offline")
    public SolrRequestConverter requestConverter() {
        return new SolrRequestConverter() {
            @Override
            public JsonQueryRequest toJsonQueryRequest(SolrRequest request) {
                JsonQueryRequest solrQuery = super.toJsonQueryRequest(request);

                // required for tests, because EmbeddedSolrServer is not sharded
                solrQuery.withParam("distrib", "false");
                solrQuery.withParam("terms.mincount", "1");

                return solrQuery;
            }
        };
    }
}
