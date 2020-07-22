package org.uniprot.api.uniref.repository.store;

import org.uniprot.core.uniref.RepresentativeMember;
import org.uniprot.store.datastore.UniProtStoreClient;
import org.uniprot.store.datastore.voldemort.VoldemortClient;

/**
 * @author jluo
 * @date: 20 Aug 2019
 */
public class UniRefMemberStoreClient extends UniProtStoreClient<RepresentativeMember> {
    public UniRefMemberStoreClient(VoldemortClient<RepresentativeMember> client) {
        super(client);
    }
}
