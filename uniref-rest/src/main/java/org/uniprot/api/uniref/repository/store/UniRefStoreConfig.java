package org.uniprot.api.uniref.repository.store;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.uniprot.core.uniref.UniRefEntry;
import org.uniprot.store.datastore.voldemort.VoldemortClient;
import org.uniprot.store.datastore.voldemort.uniref.VoldemortRemoteUniRefEntryStore;

/**
 * @author jluo
 * @date: 20 Aug 2019
 */
@Configuration
public class UniRefStoreConfig {
    @Bean
    public UniRefStoreConfigProperties storeConfigProperties() {
        return new UniRefStoreConfigProperties();
    }

    @Bean
    @Profile("live")
    public UniRefStoreClient uniRefStoreClient(
            UniRefStoreConfigProperties unirefStoreConfigProperties) {
        VoldemortClient<UniRefEntry> client =
                new VoldemortRemoteUniRefEntryStore(
                        unirefStoreConfigProperties.getNumberOfConnections(),
                        unirefStoreConfigProperties.getStoreName(),
                        unirefStoreConfigProperties.getHost());
        return new UniRefStoreClient(client);
    }
}
