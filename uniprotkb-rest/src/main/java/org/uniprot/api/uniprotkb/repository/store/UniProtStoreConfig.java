package org.uniprot.api.uniprotkb.repository.store;


import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.uniprot.api.common.repository.solrstream.FacetTupleStreamTemplate;
import org.uniprot.api.rest.respository.RepositoryConfigProperties;
import org.uniprot.core.uniprotkb.UniProtKBEntry;
import org.uniprot.store.datastore.voldemort.VoldemortClient;
import org.uniprot.store.datastore.voldemort.uniprot.VoldemortRemoteUniProtKBEntryStore;
import org.uniprot.store.search.SolrCollection;

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
        VoldemortClient<UniProtKBEntry> client =
                new VoldemortRemoteUniProtKBEntryStore(
                        uniProtStoreConfigProperties.getNumberOfConnections(),
                        uniProtStoreConfigProperties.getStoreName(),
                        uniProtStoreConfigProperties.getHost());
        return new UniProtKBStoreClient(client);
    }

    @Bean
    public FacetTupleStreamTemplate facetTupleStreamTemplate(
            RepositoryConfigProperties configProperties) {
        return FacetTupleStreamTemplate.builder()
                .collection(SolrCollection.uniprot.name())
                .zookeeperHost(configProperties.getZkHost())
                .build();
    }
}
