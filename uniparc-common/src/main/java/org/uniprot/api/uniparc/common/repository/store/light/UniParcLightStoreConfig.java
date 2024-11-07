package org.uniprot.api.uniparc.common.repository.store.light;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.uniprot.core.uniparc.UniParcEntryLight;
import org.uniprot.store.datastore.voldemort.VoldemortClient;
import org.uniprot.store.datastore.voldemort.light.uniparc.VoldemortRemoteUniParcEntryLightStore;

@Configuration
@EnableConfigurationProperties({UniParcLightStoreConfigProperties.class})
public class UniParcLightStoreConfig {

    @Bean
    @Profile("live")
    public UniParcLightStoreClient uniParcLightStoreClient(
            UniParcLightStoreConfigProperties uniParcLightStoreConfigProperties) {
        VoldemortClient<UniParcEntryLight> client =
                new VoldemortRemoteUniParcEntryLightStore(
                        uniParcLightStoreConfigProperties.getNumberOfConnections(),
                        uniParcLightStoreConfigProperties.isBrotliEnabled(),
                        uniParcLightStoreConfigProperties.getStoreName(),
                        uniParcLightStoreConfigProperties.getHost());
        return new UniParcLightStoreClient(client);
    }
}
