package org.uniprot.api.uniprotkb.common.repository.store;

import org.uniprot.core.uniprotkb.UniProtKBEntry;
import org.uniprot.store.datastore.UniProtStoreClient;
import org.uniprot.store.datastore.voldemort.VoldemortClient;

/**
 * Created 21/09/18
 *
 * @author Edd
 */
public class UniProtKBStoreClient extends UniProtStoreClient<UniProtKBEntry> {
    public UniProtKBStoreClient(VoldemortClient<UniProtKBEntry> client) {
        super(client);
    }
}
