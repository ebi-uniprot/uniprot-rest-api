package org.uniprot.api.uniparc.common.repository.store.crossref;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.uniprot.api.common.repository.stream.store.uniparc.UniParcCrossReferenceLazyLoader;
import org.uniprot.api.common.repository.stream.store.uniparc.UniParcCrossReferenceStoreConfigProperties;
import org.uniprot.core.uniparc.impl.UniParcCrossReferencePair;
import org.uniprot.store.datastore.voldemort.VoldemortClient;
import org.uniprot.store.datastore.voldemort.light.uniparc.crossref.VoldemortRemoteUniParcCrossReferenceStore;

@Configuration
@EnableConfigurationProperties({UniParcCrossReferenceStoreConfigProperties.class})
public class UniParcCrossReferenceStoreConfig {

    @Bean
    @Profile("live")
    public UniParcCrossReferenceStoreClient uniParcCrossReferenceStoreClient(
            UniParcCrossReferenceStoreConfigProperties configProperties) {
        VoldemortClient<UniParcCrossReferencePair> client =
                new VoldemortRemoteUniParcCrossReferenceStore(
                        configProperties.getNumberOfConnections(),
                        configProperties.isBrotliEnabled(),
                        configProperties.getStoreName(),
                        configProperties.getHost());
        return new UniParcCrossReferenceStoreClient(client);
    }

    @Bean
    public UniParcCrossReferenceLazyLoader uniParcCrossReferenceLazyLoader(
            UniParcCrossReferenceStoreConfigProperties configProperties,
            UniParcCrossReferenceStoreClient uniParcCrossReferenceStoreClient) {
        return new UniParcCrossReferenceLazyLoader(
                uniParcCrossReferenceStoreClient, configProperties);
    }
}
