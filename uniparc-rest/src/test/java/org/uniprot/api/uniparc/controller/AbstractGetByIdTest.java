package org.uniprot.api.uniparc.controller;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.provider.Arguments;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.uniprot.api.uniparc.repository.UniParcQueryRepository;
import org.uniprot.api.uniparc.repository.store.UniParcStoreClient;
import org.uniprot.core.Property;
import org.uniprot.core.uniparc.UniParcCrossReference;
import org.uniprot.core.uniparc.UniParcDatabase;
import org.uniprot.core.uniparc.UniParcEntry;
import org.uniprot.core.uniparc.impl.UniParcCrossReferenceBuilder;
import org.uniprot.core.uniparc.impl.UniParcEntryBuilder;
import org.uniprot.core.xml.jaxb.uniparc.Entry;
import org.uniprot.core.xml.uniparc.UniParcEntryConverter;
import org.uniprot.store.config.UniProtDataType;
import org.uniprot.store.config.returnfield.factory.ReturnFieldConfigFactory;
import org.uniprot.store.datastore.UniProtStoreClient;
import org.uniprot.store.datastore.voldemort.uniparc.VoldemortInMemoryUniParcEntryStore;
import org.uniprot.store.indexer.DataStoreManager;
import org.uniprot.store.indexer.uniparc.UniParcDocumentConverter;
import org.uniprot.store.indexer.uniprot.mockers.TaxonomyRepoMocker;
import org.uniprot.store.search.SolrCollection;

import java.time.LocalDate;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * @author sahmad
 * @created 13/08/2020
 */
public abstract class AbstractGetByIdTest {
    @RegisterExtension
    static DataStoreManager storeManager = new DataStoreManager();

    @Autowired
    private UniProtStoreClient<UniParcEntry> storeClient;

    @Autowired protected MockMvc mockMvc;

    private static final String UPI_PREF = "UPI0000083A";

    @Autowired private UniParcQueryRepository repository;

    @BeforeAll
    void initDataStore() {
        storeManager.addSolrClient(DataStoreManager.StoreType.UNIPARC, SolrCollection.uniparc);
        ReflectionTestUtils.setField(
                repository,
                "solrClient",
                storeManager.getSolrClient(DataStoreManager.StoreType.UNIPARC));
        storeClient =
                new UniParcStoreClient(
                        VoldemortInMemoryUniParcEntryStore.getInstance("avro-uniparc"));
        storeManager.addStore(DataStoreManager.StoreType.UNIPARC, storeClient);

        storeManager.addDocConverter(
                DataStoreManager.StoreType.UNIPARC,
                new UniParcDocumentConverter(TaxonomyRepoMocker.getTaxonomyRepo()));

        // create 5 entries
        IntStream.rangeClosed(1, 5).forEach(this::saveEntry);
    }

    @AfterAll
    static void cleanUp() {
        storeManager.cleanSolr(DataStoreManager.StoreType.UNIPARC);
        storeManager.close();
    }

    protected abstract String getGetByIdEndpoint();

    private void saveEntry(int i) {
        UniParcEntry entry = UniParcControllerITUtils.createEntry(i, UPI_PREF);
        // append two more cross ref
        UniParcEntry updatedEntry = appendTwoMoreXRefs(entry, i);
        UniParcEntryConverter converter = new UniParcEntryConverter();
        Entry xmlEntry = converter.toXml(updatedEntry);
        storeManager.saveEntriesInSolr(DataStoreManager.StoreType.UNIPARC, xmlEntry);
        storeManager.saveToStore(DataStoreManager.StoreType.UNIPARC, updatedEntry);
    }

    private UniParcEntry appendTwoMoreXRefs(UniParcEntry entry, int i) {
        UniParcCrossReference xref1 =
                new UniParcCrossReferenceBuilder()
                        .versionI(1)
                        .database(UniParcDatabase.EMBL)
                        .id("embl" + i)
                        .version(7)
                        .active(true)
                        .created(LocalDate.of(2017, 2, 12))
                        .lastUpdated(LocalDate.of(2017, 4, 23))
                        .propertiesAdd(
                                new Property(
                                        UniParcCrossReference.PROPERTY_PROTEIN_NAME,
                                        "proteinName" + i))
                        .build();

        UniParcCrossReference xref2 =
                new UniParcCrossReferenceBuilder()
                        .versionI(1)
                        .database(UniParcDatabase.UNIMES)
                        .id("unimes" + i)
                        .version(7)
                        .active(false)
                        .created(LocalDate.of(2017, 2, 12))
                        .lastUpdated(LocalDate.of(2017, 4, 23))
                        .propertiesAdd(
                                new Property(
                                        UniParcCrossReference.PROPERTY_PROTEIN_NAME,
                                        "proteinName" + i))
                        .build();
        UniParcEntryBuilder builder = UniParcEntryBuilder.from(entry);
        builder.uniParcCrossReferencesAdd(xref1);
        builder.uniParcCrossReferencesAdd(xref2);
        return builder.build();
    }

    protected Stream<Arguments> getAllReturnedFields() {
        return ReturnFieldConfigFactory.getReturnFieldConfig(UniProtDataType.UNIPARC)
                .getReturnFields().stream()
                .map(returnField -> Arguments.of(returnField.getName(), returnField.getPaths()));
    }
}
