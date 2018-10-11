package uk.ac.ebi.uniprot.uuw.advanced.search.store;

import uk.ac.ebi.kraken.interfaces.uniprot.UniProtEntry;
import uk.ac.ebi.uniprot.dataservice.serializer.avro.Converter;
import uk.ac.ebi.uniprot.dataservice.voldemort.VoldemortClient;
import uk.ac.ebi.uniprot.services.data.serializer.model.entry.EntryObject;

/**
 * Created 21/09/18
 *
 * @author Edd
 */
public class UniProtStoreClient extends UUWStoreClient<UniProtEntry, EntryObject> {
    public UniProtStoreClient(VoldemortClient<EntryObject> client, Converter<UniProtEntry, EntryObject> converter) {
        super(client, converter);
    }
}
