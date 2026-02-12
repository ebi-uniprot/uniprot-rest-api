package org.uniprot.api.uniprotkb.common.repository.store.precomputed;

import org.uniprot.core.uniprotkb.UniProtKBEntry;
import org.uniprot.store.datastore.UniProtStoreClient;
import org.uniprot.store.datastore.voldemort.VoldemortClient;

public class PrecomputedAnnotationStoreClient extends UniProtStoreClient<UniProtKBEntry> {
    public PrecomputedAnnotationStoreClient(VoldemortClient<UniProtKBEntry> client) {
        super(client);
    }
}
