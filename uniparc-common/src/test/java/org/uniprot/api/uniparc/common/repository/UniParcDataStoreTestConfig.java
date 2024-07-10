package org.uniprot.api.uniparc.common.repository;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;
import org.uniprot.api.uniparc.common.repository.store.entry.UniParcStoreClient;
import org.uniprot.store.datastore.voldemort.uniparc.VoldemortInMemoryUniParcEntryStore;

/**
 * @author jluo
 * @date: 25 Jun 2019
 */
@TestConfiguration
public class UniParcDataStoreTestConfig {

    @Bean
    @Profile("offline")
    public UniParcStoreClient uniparcStoreClient() {
        return new UniParcStoreClient(VoldemortInMemoryUniParcEntryStore.getInstance("uniparc"));
    }
}
