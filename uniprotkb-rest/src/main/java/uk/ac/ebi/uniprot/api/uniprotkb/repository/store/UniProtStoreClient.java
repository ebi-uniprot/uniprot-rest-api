package uk.ac.ebi.uniprot.api.uniprotkb.repository.store;

import uk.ac.ebi.uniprot.api.common.repository.store.UUWStoreClient;
import uk.ac.ebi.uniprot.dataservice.voldemort.VoldemortClient;
import uk.ac.ebi.uniprot.domain.uniprot.UniProtEntry;

/**
 * Created 21/09/18
 *
 * @author Edd
 */
public class UniProtStoreClient extends UUWStoreClient<UniProtEntry> {
    public UniProtStoreClient(VoldemortClient<UniProtEntry> client) {
        super(client);
    }
}
