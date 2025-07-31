package org.uniprot.api.uniprotkb.common.repository.store.protnlm;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.uniprot.core.uniprotkb.UniProtKBEntry;
import org.uniprot.store.datastore.voldemort.VoldemortClient;
import org.uniprot.store.datastore.voldemort.uniprot.VoldemortRemoteUniProtKBEntryStore;

@Configuration
public class ProtNLMStoreConfig {
    @Bean
    public ProtNLMStoreConfigProperties protNLMStoreConfigProperties() {
        return new ProtNLMStoreConfigProperties();
    }

    @Bean
    @Profile("live")
    public ProtNLMStoreClient protNLMStoreClient(
            ProtNLMStoreConfigProperties protNLMStoreConfigProperties) {
        VoldemortClient<UniProtKBEntry> client =
                new VoldemortRemoteUniProtKBEntryStore(
                        protNLMStoreConfigProperties.getNumberOfConnections(),
                        protNLMStoreConfigProperties.isBrotliEnabled(),
                        protNLMStoreConfigProperties.getStoreName(),
                        protNLMStoreConfigProperties.getHost());
        return new ProtNLMStoreClient(client);
    }
}
