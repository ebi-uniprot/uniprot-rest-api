package uk.ac.ebi.uniprot.uuw.advanced.search.store;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import uk.ac.ebi.kraken.interfaces.uniprot.UniProtEntry;
import uk.ac.ebi.uniprot.dataservice.restful.entry.dataaccess.EntryJsonDataAdapterImpl;
import uk.ac.ebi.uniprot.dataservice.restful.entry.domain.model.UPEntry;
import uk.ac.ebi.uniprot.dataservice.restful.response.adapter.JsonDataAdapter;
import uk.ac.ebi.uniprot.dataservice.voldemort.VoldemortClient;
import uk.ac.ebi.uniprot.dataservice.voldemort.client.impl.DefaultClientFactory;
import uk.ac.ebi.uniprot.services.data.serializer.model.entry.EntryObject;

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
    public VoldemortClient uniProtClient(UniProtStoreConfigProperties uniProtStoreConfigProperties) {
        return new DefaultClientFactory(
                uniProtStoreConfigProperties.getHost(),
                uniProtStoreConfigProperties.getNumberOfConnections(),
                uniProtStoreConfigProperties.getStoreName())
                .createUniProtClient();
    }

    @Bean
    public JsonDataAdapter<UniProtEntry,UPEntry> uniProtJsonAdaptor() {
        return new EntryJsonDataAdapterImpl();
    }
}
