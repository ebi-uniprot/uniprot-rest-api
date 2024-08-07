package org.uniprot.api.uniparc.common.repository.store.crossref;

import org.uniprot.core.uniparc.impl.UniParcCrossReferencePair;
import org.uniprot.store.datastore.UniProtStoreClient;
import org.uniprot.store.datastore.voldemort.VoldemortClient;

public class UniParcCrossReferenceStoreClient
        extends UniProtStoreClient<UniParcCrossReferencePair> {

    public UniParcCrossReferenceStoreClient(
            VoldemortClient<UniParcCrossReferencePair> client) {
        super(client);
    }
}
