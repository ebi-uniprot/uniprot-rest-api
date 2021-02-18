package org.uniprot.api.idmapping.controller;

import org.apache.http.client.HttpClient;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;
import org.uniprot.api.common.repository.search.SolrRequest;
import org.uniprot.api.common.repository.search.SolrRequestConverter;
import org.uniprot.api.idmapping.service.IDMappingPIRService;
import org.uniprot.core.uniprotkb.UniProtKBEntry;
import org.uniprot.store.datastore.UniProtStoreClient;
import org.uniprot.store.datastore.voldemort.uniprot.VoldemortInMemoryUniprotEntryStore;

import java.net.URISyntaxException;

import static org.mockito.Mockito.mock;

/**
 * @author sahmad
 * @created 18/02/2021
 */
@TestConfiguration
public class DataStoreTestConfig {
    @Bean
    @Profile("offline")
    public HttpClient httpClient() {
        return mock(HttpClient.class);
    }

    @Bean
    @Profile("offline")
    public SolrClient solrClient() throws URISyntaxException {
        return mock(SolrClient.class);
    }

    @SuppressWarnings("rawtypes")
    @Bean
    @Profile("offline")
    public UniProtStoreClient<UniProtKBEntry> primaryUniProtStoreClient() {
        return new UniProtStoreClient<>(
                VoldemortInMemoryUniprotEntryStore.getInstance("avro-uniprot"));
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

    @Bean
    @Profile("offline")
    public IDMappingPIRService pirService() {
        return mock(IDMappingPIRService.class);
    }
}