package org.uniprot.api.uniparc.controller;

import static org.mockito.Mockito.mock;

import org.apache.http.client.HttpClient;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;
import org.uniprot.api.common.repository.search.SolrRequest;
import org.uniprot.api.common.repository.search.SolrRequestConverter;
import org.uniprot.api.uniparc.repository.store.UniParcStoreClient;
import org.uniprot.store.datastore.voldemort.uniparc.VoldemortInMemoryUniParcEntryStore;

/**
 * @author jluo
 * @date: 25 Jun 2019
 */
@TestConfiguration
public class UniParcDataStoreTestConfig {

    @Bean
    @Profile("offline")
    public HttpClient httpClient() {
        return mock(HttpClient.class);
    }

    @Bean
    @Profile("offline")
    public SolrClient uniparcSolrClient() {
        return mock(SolrClient.class);
    }

    @Bean
    @Profile("offline")
    public UniParcStoreClient uniparcStoreClient() {
        return new UniParcStoreClient(VoldemortInMemoryUniParcEntryStore.getInstance("uniparc"));
    }

    @Bean
    @Profile("offline")
    public SolrRequestConverter requestConverter() {
        return new SolrRequestConverter() {
            @Override
            public SolrQuery toSolrQuery(SolrRequest request) {
                SolrQuery solrQuery = super.toSolrQuery(request);

                // required for tests, because EmbeddedSolrServer is not sharded
                solrQuery.setParam("distrib", "false");
                solrQuery.setParam("terms.mincount", "1");

                return solrQuery;
            }
        };
    }
}
