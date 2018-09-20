package uk.ac.ebi.uniprot.uuw.advanced.search.repository;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrServerException;
import org.slf4j.Logger;
import uk.ac.ebi.uniprot.dataservice.document.Document;
import uk.ac.ebi.uniprot.dataservice.document.DocumentConverter;
import uk.ac.ebi.uniprot.dataservice.serializer.avro.Converter;
import uk.ac.ebi.uniprot.dataservice.voldemort.VoldemortClient;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static java.util.Arrays.asList;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Created 19/09/18
 *
 * @author Edd
 */
public class DataStoreManager {
    private static final Logger LOGGER = getLogger(DataStoreManager.class);
    private final SolrDataStoreManager solrDataStoreManager;
    private final Map<StoreType, SolrClient> solrClientMap = new HashMap<>();
    private final Map<StoreType, VoldemortClient> voldemortMap = new HashMap<>();
    private final Map<StoreType, DocumentConverter> docConverterMap = new HashMap<>();
    private final Map<StoreType, Converter> entryConverterMap = new HashMap<>();

    public DataStoreManager(SolrDataStoreManager solrDataStoreManager) {
        this.solrDataStoreManager = solrDataStoreManager;
    }

    public void close() {
        for (SolrClient client : solrClientMap.values()) {
            try {
                client.close();
            } catch (IOException e) {
                throw new IllegalStateException(e);
            }
        }
        solrDataStoreManager.cleanUp();
    }

    public void addSolrClient(StoreType storeType, ClosableEmbeddedSolrClient client) {
        solrClientMap.put(storeType, client);
    }

    public void addVoldemort(StoreType storeType, VoldemortClient voldemortClient) {
        voldemortMap.put(storeType, voldemortClient);
    }

    public void addDocConverter(StoreType storeType, DocumentConverter converter) {
        docConverterMap.put(storeType, converter);
    }

    public void addEntryConverter(StoreType storeType, Converter converter) {
        entryConverterMap.put(storeType, converter);
    }

    @SuppressWarnings("unchecked")
    public <T> void saveToVoldemort(StoreType storeType, List<T> entries) {
        VoldemortClient voldemort = getVoldemort(storeType);
        Converter<T, ?> converter = entryConverterMap.get(storeType);
        entries.stream().map(converter::toAvro).forEach(entryObject -> voldemort.saveEntry(entryObject));
        LOGGER.debug("Added {} entries to voldemort", entries.size());
    }

    public <T> void saveToVoldemort(StoreType storeType, T... entries) {
        saveToVoldemort(storeType, asList(entries));
    }

    public SolrClient getSolrClient(StoreType storeType) {
        return solrClientMap.get(storeType);
    }

    public <T> void saveDocs(StoreType storeType, List<T> docs) {
        SolrClient client = getSolrClient(storeType);
        try {
            for (T doc : docs) {
                client.addBean(doc);
            }
            client.commit();
        } catch (SolrServerException | IOException e) {
            throw new IllegalStateException(e);
        }
        LOGGER.debug("Added {} beans to Solr", docs.size());
    }

    public <T> void saveDocs(StoreType storeType, T... docs) {
        saveDocs(storeType, asList(docs));
    }

    @SuppressWarnings("unchecked")
    public <T, D extends Document> void saveEntriesInSolr(StoreType storeType, List<T> entries) {
        DocumentConverter<T, D> documentConverter = docConverterMap.get(storeType);
        SolrClient client = solrClientMap.get(storeType);
        List<D> docs = entries.stream().map(documentConverter::convert).flatMap(Collection::stream)
                .collect(Collectors.toList());
        try {
            client.addBeans(docs);
            client.commit();
        } catch (SolrServerException | IOException e) {
            throw new IllegalStateException(e);
        }
    }

    public <T> void saveEntriesInSolr(StoreType storeType, T... entries) {
        saveEntriesInSolr(storeType, asList(entries));
    }

    private VoldemortClient getVoldemort(StoreType storeType) {
        return voldemortMap.get(storeType);
    }

    public <T> List<T> getVoldemortEntries(StoreType storeType, String... entries) {
        return getVoldemortEntries(storeType, asList(entries));
    }

    public <S, T> List<T> getVoldemortEntries(StoreType storeType, List<String> entries) {
        VoldemortClient<S> voldemort = getVoldemort(storeType);
        Converter<T, S> converter = entryConverterMap.get(storeType);
        List<S> voldemortEntries = voldemort.getEntries(entries);
        return voldemortEntries.stream().map(converter::fromAvro).collect(Collectors.toList());
    }

    public void cleanSolr(StoreType storeType) {
        try {
            SolrClient solrClient = solrClientMap.get(storeType);
            solrClient.deleteByQuery("*:*");
            solrClient.commit();
        } catch (SolrServerException | IOException e) {
            throw new IllegalStateException(e);
        }
    }

    public enum StoreType {
        UNIPROT, UNIPARC, UNIREF
    }
}
