package org.uniprot.api.uniref.repository.store;

import org.uniprot.core.uniref.UniRefEntry;
import org.uniprot.store.datastore.UniProtStoreClient;
import org.uniprot.store.datastore.voldemort.VoldemortClient;

/**
 * @author jluo
 * @date: 20 Aug 2019
 */
public class UniRefStoreClient extends UniProtStoreClient<UniRefEntry> {
    public UniRefStoreClient(VoldemortClient<UniRefEntry> client) {
        super(client);
    }
}
