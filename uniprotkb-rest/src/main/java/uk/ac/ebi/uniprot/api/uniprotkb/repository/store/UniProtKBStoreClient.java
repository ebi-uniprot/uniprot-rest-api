package uk.ac.ebi.uniprot.api.uniprotkb.repository.store;

import uk.ac.ebi.uniprot.api.common.repository.store.UniProtStoreClient;
import uk.ac.ebi.uniprot.datastore.voldemort.VoldemortClient;
import uk.ac.ebi.uniprot.domain.uniprot.UniProtEntry;

/**
 * Created 21/09/18
 *
 * @author Edd
 */
public class UniProtKBStoreClient extends UniProtStoreClient<UniProtEntry> {
    public UniProtKBStoreClient(VoldemortClient<UniProtEntry> client) {
        super(client);
    }
}
