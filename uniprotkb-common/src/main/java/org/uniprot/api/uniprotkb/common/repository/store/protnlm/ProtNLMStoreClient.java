package org.uniprot.api.uniprotkb.common.repository.store.protnlm;

import org.uniprot.core.uniprotkb.UniProtKBEntry;
import org.uniprot.store.datastore.UniProtStoreClient;
import org.uniprot.store.datastore.voldemort.VoldemortClient;

public class ProtNLMStoreClient extends UniProtStoreClient<UniProtKBEntry> {
    public ProtNLMStoreClient(VoldemortClient<UniProtKBEntry> client) {
        super(client);
    }
}
