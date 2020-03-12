package org.uniprot.api.uniprotkb.repository.store;

import org.uniprot.core.uniprotkb.UniProtkbEntry;
import org.uniprot.store.datastore.UniProtStoreClient;
import org.uniprot.store.datastore.voldemort.VoldemortClient;

/**
 * Created 21/09/18
 *
 * @author Edd
 */
public class UniProtKBStoreClient extends UniProtStoreClient<UniProtkbEntry> {
    public UniProtKBStoreClient(VoldemortClient<UniProtkbEntry> client) {
        super(client);
    }
}
