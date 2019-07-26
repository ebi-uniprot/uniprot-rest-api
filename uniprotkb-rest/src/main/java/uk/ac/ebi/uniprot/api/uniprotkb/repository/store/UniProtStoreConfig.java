package uk.ac.ebi.uniprot.api.uniprotkb.repository.store;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import uk.ac.ebi.uniprot.datastore.voldemort.VoldemortClient;
import uk.ac.ebi.uniprot.datastore.voldemort.uniprot.VoldemortRemoteUniprotEntryStore;
import uk.ac.ebi.uniprot.domain.uniprot.UniProtEntry;

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
    public UniProtKBStoreClient uniProtStoreClient(UniProtStoreConfigProperties uniProtStoreConfigProperties) {
        VoldemortClient<UniProtEntry> client =
                new VoldemortRemoteUniprotEntryStore(uniProtStoreConfigProperties
                                                             .getNumberOfConnections(),
                                                     uniProtStoreConfigProperties.getStoreName(),
                                                     uniProtStoreConfigProperties.getHost());
        return new UniProtKBStoreClient(client);
    }
}
