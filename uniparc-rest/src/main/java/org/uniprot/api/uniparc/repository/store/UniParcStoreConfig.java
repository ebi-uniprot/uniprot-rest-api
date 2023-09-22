package org.uniprot.api.uniparc.repository.store;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.uniprot.core.uniparc.UniParcEntry;
import org.uniprot.store.datastore.voldemort.VoldemortClient;
import org.uniprot.store.datastore.voldemort.uniparc.VoldemortRemoteUniParcEntryStore;

/**
 * @author lgonzales
 * @since 2020-03-04
 */
@Configuration
@EnableConfigurationProperties({UniParcStoreConfigProperties.class})
public class UniParcStoreConfig {

    @Bean
    @Profile("live")
    public UniParcStoreClient uniParcStoreClient(
            UniParcStoreConfigProperties uniParcStoreConfigProperties) {
        VoldemortClient<UniParcEntry> client =
                new VoldemortRemoteUniParcEntryStore(
                        uniParcStoreConfigProperties.getNumberOfConnections(),
                        uniParcStoreConfigProperties.isBrotliEnabled(),
                        uniParcStoreConfigProperties.getStoreName(),
                        uniParcStoreConfigProperties.getHost());
        return new UniParcStoreClient(client);
    }
}
