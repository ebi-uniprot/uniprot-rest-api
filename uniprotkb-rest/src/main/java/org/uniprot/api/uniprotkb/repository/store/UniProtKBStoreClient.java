package org.uniprot.api.uniprotkb.repository.store;



import org.uniprot.store.datastore.UniProtStoreClient;
import org.uniprot.store.datastore.voldemort.VoldemortClient;
import org.uniprot.core.uniprot.UniProtEntry;

/**
 * Created 21/09/18
 *
 * @author Edd
 */
public class UniProtKBStoreClient extends UniProtStoreClient<UniProtEntry> {
    public UniProtKBStoreClient(VoldemortClient<UniProtEntry> client) {
        super(client);
    }
}
