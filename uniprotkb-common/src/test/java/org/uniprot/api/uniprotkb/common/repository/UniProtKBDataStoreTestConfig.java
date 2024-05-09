package org.uniprot.api.uniprotkb.common.repository;

import static org.mockito.Mockito.mock;

import org.apache.solr.client.solrj.SolrClient;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;
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

    @SuppressWarnings("rawtypes")
    @Bean
    @Profile("offline")
    public UniProtKBStoreClient uniProtKBStoreClient() {
        return new UniProtKBStoreClient(
                VoldemortInMemoryUniprotEntryStore.getInstance("avro-uniprot"));
    }

    @Bean()
    @Profile("offline")
    public UniSaveClient unisaveClient() {
        UniSaveClient client = mock(UniSaveClient.class);
        return client;
    }
}
