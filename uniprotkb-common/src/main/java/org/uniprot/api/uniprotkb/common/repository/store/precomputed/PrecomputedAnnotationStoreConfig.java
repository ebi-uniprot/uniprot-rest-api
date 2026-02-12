package org.uniprot.api.uniprotkb.common.repository.store.precomputed;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.uniprot.core.uniprotkb.UniProtKBEntry;
import org.uniprot.store.datastore.voldemort.VoldemortClient;
import org.uniprot.store.datastore.voldemort.uniprot.VoldemortRemoteUniProtKBEntryStore;

@Configuration
public class PrecomputedAnnotationStoreConfig {
    @Bean
    public PrecomputedAnnotationStoreConfigProperties precomputedAnnotationStoreConfigProperties() {
        return new PrecomputedAnnotationStoreConfigProperties();
    }

    @Bean
    @Profile("live")
    public PrecomputedAnnotationStoreClient precomputedAnnotationStoreClient(
            PrecomputedAnnotationStoreConfigProperties precomputedAnnotationStoreConfigProperties) {
        VoldemortClient<UniProtKBEntry> client =
                new VoldemortRemoteUniProtKBEntryStore(
                        precomputedAnnotationStoreConfigProperties.getNumberOfConnections(),
                        precomputedAnnotationStoreConfigProperties.isBrotliEnabled(),
                        precomputedAnnotationStoreConfigProperties.getStoreName(),
                        precomputedAnnotationStoreConfigProperties.getHost());
        return new PrecomputedAnnotationStoreClient(client);
    }
}
