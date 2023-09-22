package org.uniprot.api.aa.controller;

import static org.mockito.Mockito.mock;

import org.apache.http.client.HttpClient;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.request.json.JsonQueryRequest;
import org.apache.solr.common.params.ModifiableSolrParams;
import org.mockito.Mockito;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;
import org.uniprot.api.common.repository.search.SolrRequest;
import org.uniprot.api.common.repository.search.SolrRequestConverter;
import org.uniprot.api.common.repository.stream.rdf.RdfStreamer;

/**
 * @author sahmad
 * @created 11/11/2020
 */
@TestConfiguration
public class DataStoreTestConfig {

    @Bean
    @Profile("offline")
    public HttpClient httpClient() {
        return Mockito.mock(HttpClient.class);
    }

    @Bean
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
                ((ModifiableSolrParams) solrQuery.getParams()).set("distrib", "false");
                ((ModifiableSolrParams) solrQuery.getParams()).set("terms.mincount", "1");

                return solrQuery;
            }
        };
    }

    @Bean
    public RdfStreamer rdfStreamer() {
        return mock(RdfStreamer.class);
    }
}
