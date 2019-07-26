package uk.ac.ebi.uniprot.api.common.repository.store;

import uk.ac.ebi.uniprot.datastore.voldemort.VoldemortClient;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Generic class for saving and retrieving entities of type {@code S} to/from a data-store containing
 * serialized forms of the entity, of type {@code A}. Currently, the underlying data-store is accessed via a
 * {@link VoldemortClient}, but could be replaced in future to any other store.
 *
 * Created 21/09/18
 *
 * @author Edd
 */
public abstract class UniProtStoreClient<S> {
    private final VoldemortClient<S> client;

    public UniProtStoreClient(VoldemortClient<S> client) {
        this.client = client;
    }

    public String getStoreName() {
        return client.getStoreName();
    }

    public Optional<S> getEntry(String s) {
        return client.getEntry(s);
    }

    public List<S> getEntries(Iterable<String> iterable) {
        return client.getEntries(iterable);
    }

    public Map<String, S> getEntryMap(Iterable<String> iterable) {
        return client.getEntryMap(iterable);
    }

    public void saveEntry(S s) {
        client.saveEntry(s);
    }
}
