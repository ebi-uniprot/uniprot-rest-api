package org.uniprot.api.idmapping.controller;

import static org.mockito.Mockito.mock;

import java.net.URISyntaxException;

import org.apache.http.client.HttpClient;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;
import org.uniprot.api.common.repository.search.SolrRequest;
import org.uniprot.api.common.repository.search.SolrRequestConverter;
import org.uniprot.api.idmapping.controller.request.IdMappingJobRequest;
import org.uniprot.api.idmapping.model.IdMappingResult;
import org.uniprot.api.idmapping.service.IdMappingJobCacheService;
import org.uniprot.api.idmapping.service.IdMappingPIRService;
import org.uniprot.core.uniparc.UniParcEntry;
import org.uniprot.core.uniprotkb.UniProtKBEntry;
import org.uniprot.core.uniref.UniRefEntryLight;
import org.uniprot.store.datastore.UniProtStoreClient;
import org.uniprot.store.datastore.voldemort.light.uniref.VoldemortInMemoryUniRefEntryLightStore;
import org.uniprot.store.datastore.voldemort.uniparc.VoldemortInMemoryUniParcEntryStore;
import org.uniprot.store.datastore.voldemort.uniprot.VoldemortInMemoryUniprotEntryStore;

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

    @Bean("uniProtStoreClient")
    @Profile("offline")
    public UniProtStoreClient<UniProtKBEntry> uniProtStoreClient() {
        return new UniProtStoreClient<>(
                VoldemortInMemoryUniprotEntryStore.getInstance("avro-uniprot"));
    }

    @Bean("uniRefLightStoreClient")
    @Profile("offline")
    public UniProtStoreClient<UniRefEntryLight> uniRefLightStoreClient() {
        return new UniProtStoreClient<>(
                VoldemortInMemoryUniRefEntryLightStore.getInstance("avro-uniprot"));
    }

    @Bean("uniParcStoreClient")
    @Profile("offline")
    public UniProtStoreClient<UniParcEntry> uniParcStoreClient() {
        return new UniProtStoreClient<>(VoldemortInMemoryUniParcEntryStore.getInstance("uniparc"));
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
    public IdMappingPIRService pirService() {
        return new IdMappingPIRService(25) {
            @Override
            public IdMappingResult mapIds(IdMappingJobRequest request) {
                return null;
            }
        };
    }

    @Bean
    @Profile("offline")
    public JobOperation idMappingResultJobOp(IdMappingJobCacheService cacheService) {
        return new IdMappingResultsJobOperation(cacheService);
    }
}
