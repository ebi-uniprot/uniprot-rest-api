package org.uniprot.api.uniparc.controller;

import java.util.HashMap;
import java.util.List;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.uniprot.api.common.repository.search.SolrQueryRepository;
import org.uniprot.api.rest.controller.AbstractGetByIdControllerIT;
import org.uniprot.api.uniparc.common.repository.search.UniParcQueryRepository;
import org.uniprot.api.uniparc.common.repository.store.crossref.UniParcCrossReferenceStoreClient;
import org.uniprot.api.uniparc.common.repository.store.light.UniParcLightStoreClient;
import org.uniprot.core.uniparc.UniParcEntry;
import org.uniprot.core.uniparc.UniParcEntryLight;
import org.uniprot.core.uniparc.impl.UniParcCrossReferencePair;
import org.uniprot.core.xml.jaxb.uniparc.Entry;
import org.uniprot.core.xml.uniparc.UniParcEntryConverter;
import org.uniprot.store.datastore.voldemort.light.uniparc.VoldemortInMemoryUniParcEntryLightStore;
import org.uniprot.store.datastore.voldemort.light.uniparc.crossref.VoldemortInMemoryUniParcCrossReferenceStore;
import org.uniprot.store.indexer.DataStoreManager;
import org.uniprot.store.indexer.converters.UniParcDocumentConverter;
import org.uniprot.store.indexer.uniparc.mockers.UniParcCrossReferenceMocker;
import org.uniprot.store.indexer.uniparc.mockers.UniParcEntryMocker;
import org.uniprot.store.indexer.uniprot.mockers.TaxonomyRepoMocker;
import org.uniprot.store.search.SolrCollection;

public abstract class BaseUniParcGetByIdControllerTest extends AbstractGetByIdControllerIT {
    @Autowired private UniParcQueryRepository repository;

    protected static final String UPI_PREF = "UPI0000083D";
    protected static String ACCESSION = "P12301";
    protected static String UNIPARC_ID = "UPI0000083D01";

    protected abstract String getIdPathValue();

    @Value("${voldemort.uniparc.cross.reference.groupSize:#{null}}")
    private Integer xrefGroupSize;

    @Override
    protected DataStoreManager.StoreType getStoreType() {
        return DataStoreManager.StoreType.UNIPARC;
    }

    @Override
    protected SolrCollection getSolrCollection() {
        return SolrCollection.uniparc;
    }

    @Override
    protected SolrQueryRepository getRepository() {
        return this.repository;
    }

    @Override
    protected void saveEntry() {
        int xrefCount = 25;
        // full uniparc entry object for solr
        UniParcEntry uniParcEntry = UniParcEntryMocker.createUniParcEntry(1, UPI_PREF, xrefCount);
        UniParcEntryConverter converter = new UniParcEntryConverter();
        Entry xmlEntry = converter.toXml(uniParcEntry);
        DataStoreManager manager = getStoreManager();
        manager.saveEntriesInSolr(DataStoreManager.StoreType.UNIPARC, xmlEntry);
        //  uniparc light and cross reference in voldemort
        int qualifier = 1;
        UniParcEntryLight uniParcEntryLight =
                UniParcEntryMocker.createUniParcEntryLight(qualifier, UPI_PREF, xrefCount);
        manager.saveToStore(DataStoreManager.StoreType.UNIPARC_LIGHT, uniParcEntryLight);
        List<UniParcCrossReferencePair> crossReferences =
                UniParcCrossReferenceMocker.createUniParcCrossReferencePairs(
                        uniParcEntryLight.getUniParcId(), 1, xrefCount, xrefGroupSize);
        manager.saveToStore(DataStoreManager.StoreType.UNIPARC_CROSS_REFERENCE, crossReferences);
    }

    @BeforeAll
    void initDataStore() {
        getStoreManager()
                .addDocConverter(
                        DataStoreManager.StoreType.UNIPARC,
                        new UniParcDocumentConverter(
                                TaxonomyRepoMocker.getTaxonomyRepo(), new HashMap<>()));

        UniParcLightStoreClient uniParcLightStoreClient =
                new UniParcLightStoreClient(
                        VoldemortInMemoryUniParcEntryLightStore.getInstance("uniparc-light"));
        VoldemortInMemoryUniParcCrossReferenceStore xrefVDClient =
                VoldemortInMemoryUniParcCrossReferenceStore.getInstance("uniparc-cross-reference");
        UniParcCrossReferenceStoreClient crossRefStoreClient =
                new UniParcCrossReferenceStoreClient(xrefVDClient);
        getStoreManager()
                .addStore(DataStoreManager.StoreType.UNIPARC_LIGHT, uniParcLightStoreClient);
        getStoreManager()
                .addStore(DataStoreManager.StoreType.UNIPARC_CROSS_REFERENCE, crossRefStoreClient);
    }

    @AfterAll
    void cleanUp() {
        getStoreManager().cleanSolr(DataStoreManager.StoreType.UNIPARC);
        getStoreManager().close();
    }
}
