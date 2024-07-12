package org.uniprot.api.uniparc.common.repository.store.crossref;

import org.uniprot.core.uniparc.UniParcCrossReference;
import org.uniprot.store.datastore.UniProtStoreClient;
import org.uniprot.store.datastore.voldemort.VoldemortClient;

public class UniParcCrossReferenceStoreClient extends UniProtStoreClient<UniParcCrossReference> {

    private final int batchSize;

    public UniParcCrossReferenceStoreClient(VoldemortClient<UniParcCrossReference> client, int batchSize) {
        super(client);
        this.batchSize = batchSize;
    }

    public int getBatchSize(){
        return this.batchSize;
    }
}
