package org.uniprot.api.uniprotkb.common.repository;

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
import org.uniprot.api.uniprotkb.common.repository.store.UniProtKBStoreClient;
import org.uniprot.api.uniprotkb.common.service.uniprotkb.UniSaveClient;
import org.uniprot.store.datastore.voldemort.VoldemortClient;
import org.uniprot.store.datastore.voldemort.uniprot.VoldemortInMemoryUniprotEntryStore;

/**
 * A test configuration providing {@link SolrClient} and {@link VoldemortClient} beans that override
 * production ones. For example, this allows us to use embedded Solr data stores or in memory
 * Voldemort instances, rather than ones running on VMs.
 *
 * <p>Created 14/09/18
 *
 * @author Edd
 */
@TestConfiguration
public class UniProtKBDataStoreTestConfig {
    @Bean
    @Profile("offline")
    public HttpClient uniProtKBHttpClient() {
        return mock(HttpClient.class);
    }

    @Bean("uniProtKBSolrClient")
    @Profile("offline")
    public SolrClient uniProtKBSolrClient() throws URISyntaxException {
        return mock(SolrClient.class);
    }

    @Bean
    @Profile("offline")
    public SolrClient solrClient() throws URISyntaxException {
        return mock(SolrClient.class);
    }

    @SuppressWarnings("rawtypes")
    @Bean
    @Profile("offline")
    public UniProtKBStoreClient uniProtKBStoreClient() {
        return new UniProtKBStoreClient(
                VoldemortInMemoryUniprotEntryStore.getInstance("avro-uniprot"));
    }

    @Bean
    @Profile("offline")
    public SolrRequestConverter solrRequestConverter() {
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

    @Bean()
    @Profile("offline")
    public UniSaveClient unisaveClient() {
        UniSaveClient client = mock(UniSaveClient.class);
        return client;
    }
}
