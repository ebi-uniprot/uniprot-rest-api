package org.uniprot.api.uniref.common.repository.store;

import org.uniprot.core.uniref.UniRefEntryLight;
import org.uniprot.store.datastore.UniProtStoreClient;
import org.uniprot.store.datastore.voldemort.VoldemortClient;

/**
 * @author lgonzales
 * @since 09/07/2020
 */
public class UniRefLightStoreClient extends UniProtStoreClient<UniRefEntryLight> {
    public UniRefLightStoreClient(VoldemortClient<UniRefEntryLight> client) {
        super(client);
    }
}
