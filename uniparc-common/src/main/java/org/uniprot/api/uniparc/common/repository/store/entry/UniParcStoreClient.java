package org.uniprot.api.uniparc.common.repository.store.entry;

import org.uniprot.core.uniparc.UniParcEntry;
import org.uniprot.store.datastore.UniProtStoreClient;
import org.uniprot.store.datastore.voldemort.VoldemortClient;

/**
 * @author lgonzales
 * @since 2020-03-04
 */
public class UniParcStoreClient extends UniProtStoreClient<UniParcEntry> {
    public UniParcStoreClient(VoldemortClient<UniParcEntry> client) {
        super(client);
    }
}
