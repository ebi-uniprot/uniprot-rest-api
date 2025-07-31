package org.uniprot.api.idmapping.common;

import static org.mockito.Mockito.mock;

import java.net.URISyntaxException;

import org.apache.http.client.HttpClient;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.request.json.JsonQueryRequest;
import org.apache.solr.common.params.ModifiableSolrParams;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;
import org.uniprot.api.common.repository.search.SolrRequest;
import org.uniprot.api.common.repository.search.SolrRequestConverter;
import org.uniprot.api.idmapping.common.model.IdMappingResult;
import org.uniprot.api.idmapping.common.repository.IdMappingRepository;
import org.uniprot.api.idmapping.common.request.IdMappingJobRequest;
import org.uniprot.api.idmapping.common.service.IdMappingPIRService;
import org.uniprot.core.uniparc.UniParcEntryLight;
import org.uniprot.core.uniparc.impl.UniParcCrossReferencePair;
import org.uniprot.core.uniprotkb.UniProtKBEntry;
import org.uniprot.core.uniref.UniRefEntryLight;
import org.uniprot.store.datastore.UniProtStoreClient;
import org.uniprot.store.datastore.voldemort.light.uniparc.VoldemortInMemoryUniParcEntryLightStore;
import org.uniprot.store.datastore.voldemort.light.uniparc.crossref.VoldemortInMemoryUniParcCrossReferenceStore;
import org.uniprot.store.datastore.voldemort.light.uniref.VoldemortInMemoryUniRefEntryLightStore;
import org.uniprot.store.datastore.voldemort.uniprot.VoldemortInMemoryUniprotEntryStore;

/**
 * @author sahmad
 * @created 18/02/2021
 */
@TestConfiguration
public class IdMappingDataStoreTestConfig {
    @Bean
    @Profile("offline")
    public HttpClient idMappingHttpClient() {
        return mock(HttpClient.class);
    }

    @Bean
    @Profile("offline")
    public SolrClient idMappingSolrClient() throws URISyntaxException {
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

    @Bean("uniParcLightStoreClient")
    @Profile("offline")
    public UniProtStoreClient<UniParcEntryLight> uniParcLightStoreClient() {
        return new UniProtStoreClient<>(
                VoldemortInMemoryUniParcEntryLightStore.getInstance("uniparc-light"));
    }

    @Bean
    @Profile("offline")
    public UniProtStoreClient<UniParcCrossReferencePair> xrefStoreClient() {
        return new UniProtStoreClient<>(
                VoldemortInMemoryUniParcCrossReferenceStore.getInstance("uniparc-cross-reference"));
    }

    @Bean
    @Profile("offline")
    public SolrRequestConverter idMappingSolrRequestConverter() {
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
    @Profile("offline")
    public IdMappingPIRService pirService(
            @Value("${search.request.converter.defaultRestPageSize:#{null}}")
                    Integer defaultPageSize) {
        return new IdMappingPIRService(defaultPageSize) {
            @Override
            public IdMappingResult mapIds(IdMappingJobRequest request, String jobId) {
                return IdMappingResult.builder().build();
            }
        };
    }

    @Bean
    @Profile("offline")
    public IdMappingRepository idMappingRepo(
            @Qualifier("uniProtKBSolrClient") SolrClient uniProtKBSolrClient)
            throws URISyntaxException {
        return new IdMappingRepository(uniProtKBSolrClient, idMappingSolrClient());
    }
}
