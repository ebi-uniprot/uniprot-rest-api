package uk.ac.ebi.uniprot.uuw.advanced.search.repository;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import uk.ac.ebi.uniprot.dataservice.voldemort.client.UniProtClient;
import uk.ac.ebi.uniprot.dataservice.voldemort.client.impl.DefaultClientFactory;

/**
 * Created 21/08/18
 *
 * @author Edd
 */
@Configuration
public class StoreConfig {
    @Bean
    public StoreConfigProperties storeConfigProperties() {
        return new StoreConfigProperties();
    }

    @Bean
    public UniProtClient uniProtClient(StoreConfigProperties storeConfigProperties) {
        return new DefaultClientFactory(storeConfigProperties.getUrl()).createUniProtClient();
    }
}
