package org.uniprot.api.uniparc.common.repository.store.light;

import org.uniprot.core.uniparc.UniParcEntryLight;
import org.uniprot.store.datastore.UniProtStoreClient;
import org.uniprot.store.datastore.voldemort.VoldemortClient;

public class UniParcLightStoreClient extends UniProtStoreClient<UniParcEntryLight> {
    public UniParcLightStoreClient(VoldemortClient<UniParcEntryLight> client) {
        super(client);
    }
}
