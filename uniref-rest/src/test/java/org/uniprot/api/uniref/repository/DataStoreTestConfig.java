package org.uniprot.api.uniref.repository;

import static org.mockito.Mockito.mock;

import java.net.URISyntaxException;

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
import org.uniprot.api.uniref.repository.store.UniRefLightStoreClient;
import org.uniprot.api.uniref.repository.store.UniRefMemberStoreClient;
import org.uniprot.store.datastore.voldemort.light.uniref.VoldemortInMemoryUniRefEntryLightStore;
import org.uniprot.store.datastore.voldemort.member.uniref.VoldemortInMemoryUniRefMemberStore;

/**
 * @author jluo
 * @date: 23 Aug 2019
 */
@TestConfiguration
public class DataStoreTestConfig {

    @Bean("nonKBHttpClient")
    @Profile("offline")
    public HttpClient httpClient() {
        return mock(HttpClient.class);
    }

    @Bean("nonKBSolrClient")
    @Profile("offline")
    public SolrClient unirefSolrClient() throws URISyntaxException {
        return mock(SolrClient.class);
    }

    @Bean
    @Profile("offline")
    public UniRefMemberStoreClient unirefMemberStoreClient() {
        return new UniRefMemberStoreClient(
                VoldemortInMemoryUniRefMemberStore.getInstance("uniref-member"), 5);
    }

    @Bean
    @Profile("offline")
    public UniRefLightStoreClient unirefLightStoreClient() {
        return new UniRefLightStoreClient(
                VoldemortInMemoryUniRefEntryLightStore.getInstance("uniref-light"));
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

    @Bean(name = "rdfRestTemplate")
    @Profile("offline")
    public RestTemplate restTemplate() {
        return mock(RestTemplate.class);
    }
}
