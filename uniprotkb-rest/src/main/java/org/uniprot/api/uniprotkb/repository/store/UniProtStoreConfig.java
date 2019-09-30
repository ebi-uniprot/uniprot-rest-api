package org.uniprot.api.uniprotkb.repository.store;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.uniprot.core.uniprot.UniProtEntry;
import org.uniprot.store.datastore.voldemort.VoldemortClient;
import org.uniprot.store.datastore.voldemort.uniprot.VoldemortRemoteUniProtKBEntryStore;

/**
 * Created 21/08/18
 *
 * @author Edd
 */
@Configuration
public class UniProtStoreConfig {
    @Bean
    public UniProtStoreConfigProperties storeConfigProperties() {
        return new UniProtStoreConfigProperties();
    }

    @Bean
    @Profile("live")
    public UniProtKBStoreClient uniProtStoreClient(
            UniProtStoreConfigProperties uniProtStoreConfigProperties) {
        VoldemortClient<UniProtEntry> client =
                new VoldemortRemoteUniProtKBEntryStore(
                        uniProtStoreConfigProperties.getNumberOfConnections(),
                        uniProtStoreConfigProperties.getStoreName(),
                        uniProtStoreConfigProperties.getHost());
        return new UniProtKBStoreClient(client);
    }
}
