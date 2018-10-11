package uk.ac.ebi.uniprot.uuw.advanced.search.store;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import uk.ac.ebi.kraken.interfaces.uniprot.UniProtEntry;
import uk.ac.ebi.uniprot.dataservice.restful.entry.dataaccess.EntryJsonDataAdapterImpl;
import uk.ac.ebi.uniprot.dataservice.restful.entry.domain.model.UPEntry;
import uk.ac.ebi.uniprot.dataservice.restful.response.adapter.JsonDataAdapter;
import uk.ac.ebi.uniprot.dataservice.serializer.avro.EntryConverter;
import uk.ac.ebi.uniprot.dataservice.voldemort.VoldemortClient;
import uk.ac.ebi.uniprot.dataservice.voldemort.uniprot.VoldemortRemoteUniprotEntryStore;
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
    @Profile("live")
    public UniProtStoreClient uniProtStoreClient(UniProtStoreConfigProperties uniProtStoreConfigProperties) {
        VoldemortClient<EntryObject> client =
                new VoldemortRemoteUniprotEntryStore(uniProtStoreConfigProperties
                                                             .getNumberOfConnections(),
                                                     uniProtStoreConfigProperties.getStoreName(),
                                                     uniProtStoreConfigProperties.getHost());
        return new UniProtStoreClient(client, new EntryConverter());
    }

    @Bean
    public JsonDataAdapter<UniProtEntry, UPEntry> uniProtJsonAdaptor() {
        return new EntryJsonDataAdapterImpl();
    }
}
