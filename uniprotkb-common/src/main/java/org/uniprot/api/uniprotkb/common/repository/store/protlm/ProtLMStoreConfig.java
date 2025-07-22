package org.uniprot.api.uniprotkb.common.repository.store.protlm;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.uniprot.core.uniprotkb.UniProtKBEntry;
import org.uniprot.store.datastore.voldemort.VoldemortClient;
import org.uniprot.store.datastore.voldemort.uniprot.VoldemortRemoteUniProtKBEntryStore;

@Configuration
public class ProtLMStoreConfig {
    @Bean
    public ProtLMStoreConfigProperties protLMStoreConfigProperties() {
        return new ProtLMStoreConfigProperties();
    }

    @Bean
    @Profile("live")
    public ProtLMStoreClient protLMStoreClient(
            ProtLMStoreConfigProperties protLMStoreConfigProperties) {
        VoldemortClient<UniProtKBEntry> client =
                new VoldemortRemoteUniProtKBEntryStore(
                        protLMStoreConfigProperties.getNumberOfConnections(),
                        protLMStoreConfigProperties.isBrotliEnabled(),
                        protLMStoreConfigProperties.getStoreName(),
                        protLMStoreConfigProperties.getHost());
        return new ProtLMStoreClient(client);
    }
}
