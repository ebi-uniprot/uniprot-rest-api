package org.uniprot.api.uniprotkb.common.repository.store.protlm;

import org.uniprot.core.uniprotkb.UniProtKBEntry;
import org.uniprot.store.datastore.UniProtStoreClient;
import org.uniprot.store.datastore.voldemort.VoldemortClient;

public class ProtLMStoreClient extends UniProtStoreClient<UniProtKBEntry> {
    public ProtLMStoreClient(VoldemortClient<UniProtKBEntry> client) {
        super(client);
    }
}
