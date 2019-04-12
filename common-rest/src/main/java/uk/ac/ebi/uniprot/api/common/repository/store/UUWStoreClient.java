package uk.ac.ebi.uniprot.api.common.repository.store;

import uk.ac.ebi.uniprot.datastore.voldemort.VoldemortClient;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Generic class for saving and retrieving entities of type {@code S} to/from a {@link VoldemortClient} containing
 * serialized forms of the entity, of type {@code A}.
 *
 * Created 21/09/18
 *
 * @author Edd
 */
public abstract class UUWStoreClient<S> implements VoldemortClient<S> {
    private final VoldemortClient<S> client;

    public UUWStoreClient(VoldemortClient<S> client) {
        this.client = client;
    }

    @Override
    public String getStoreName() {
        return client.getStoreName();
    }

    @Override
    public Optional<S> getEntry(String s) {
        return client.getEntry(s);
    }

    @Override
    public List<S> getEntries(Iterable<String> iterable) {
        return client.getEntries(iterable);
    }

    @Override
    public Map<String, S> getEntryMap(Iterable<String> iterable) {
        return client.getEntryMap(iterable);
    }

    @Override
    public void saveEntry(S s) {
        client.saveEntry(s);
    }
}
