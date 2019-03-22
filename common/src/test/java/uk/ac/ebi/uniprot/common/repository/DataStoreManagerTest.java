package uk.ac.ebi.uniprot.common.repository;

import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import uk.ac.ebi.uniprot.common.repository.search.ClosableEmbeddedSolrClient;
import uk.ac.ebi.uniprot.common.repository.search.SolrCollection;
import uk.ac.ebi.uniprot.common.repository.search.SolrDataStoreManager;
import uk.ac.ebi.uniprot.common.repository.search.mockers.GoRelationsRepoMocker;
import uk.ac.ebi.uniprot.common.repository.search.mockers.TaxonomyRepoMocker;
import uk.ac.ebi.uniprot.common.repository.search.mockers.UniProtDocMocker;
import uk.ac.ebi.uniprot.common.repository.search.mockers.UniProtEntryMocker;
import uk.ac.ebi.uniprot.common.repository.store.UUWStoreClient;
import uk.ac.ebi.uniprot.dataservice.document.impl.UniprotEntryConverter;
import uk.ac.ebi.uniprot.dataservice.document.uniprot.UniProtDocument;
import uk.ac.ebi.uniprot.dataservice.voldemort.VoldemortClient;
import uk.ac.ebi.uniprot.dataservice.voldemort.uniprot.VoldemortInMemoryUniprotEntryStore;
import uk.ac.ebi.uniprot.domain.uniprot.UniProtEntry;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.fail;

class DataStoreManagerTest {
    private static final String P12345 = "P12345";
    private static DataStoreManager storeManager;

    @BeforeAll
    static void setUp() {
        try{
            SolrDataStoreManager solrStoreManager = new SolrDataStoreManager();
            ClosableEmbeddedSolrClient solrClient = new ClosableEmbeddedSolrClient(SolrCollection.uniprot);
            storeManager = new DataStoreManager(solrStoreManager);
            storeManager.addSolrClient(DataStoreManager.StoreType.UNIPROT,solrClient);

            UUWStoreClient storeClient = new FakeStoreClient(VoldemortInMemoryUniprotEntryStore
                    .getInstance("avro-uniprot"));
            storeManager.addVoldemort(DataStoreManager.StoreType.UNIPROT, storeClient);

            storeManager.addDocConverter(DataStoreManager.StoreType.UNIPROT, new UniprotEntryConverter(TaxonomyRepoMocker.getTaxonomyRepo(), GoRelationsRepoMocker.getGoRelationRepo()));
        } catch (Exception e) {
            fail("Error to setup DataStoreManagerTest",e);
        }
    }

    @AfterEach
    void cleanUp() {
        storeManager.cleanSolr(DataStoreManager.StoreType.UNIPROT);
    }

    // getEntry -------------------

    @Test
    void canAddAndSearchDocumentsInSolr() throws IOException, SolrServerException {
        storeManager.saveDocs(DataStoreManager.StoreType.UNIPROT, UniProtDocMocker.createDoc(P12345));
        QueryResponse response = storeManager.querySolr(DataStoreManager.StoreType.UNIPROT, "accession:P12345");
        List<String> results = response.getBeans(UniProtDocument.class).stream().map(doc -> doc.accession)
                .collect(Collectors.toList());
        assertThat(results, Matchers.contains(P12345));
    }

    @Test
    void canAddEntriesAndSearchDocumentsInSolr() throws IOException, SolrServerException {
        storeManager.saveDocs(DataStoreManager.StoreType.UNIPROT, UniProtDocMocker.createDoc(P12345));
        QueryResponse response = storeManager.querySolr(DataStoreManager.StoreType.UNIPROT, "accession:P12345");
        List<String> results = response.getBeans(UniProtDocument.class).stream().map(doc -> doc.accession)
                .collect(Collectors.toList());
        assertThat(results, Matchers.contains(P12345));
    }

    @Test
    void canAddAndFetchEntriesInSolr() throws IOException, SolrServerException {
        UniProtEntry entry = UniProtEntryMocker.create(UniProtEntryMocker.Type.SP);
        String accession = entry.getPrimaryAccession().getValue();
        storeManager.saveEntriesInSolr(DataStoreManager.StoreType.UNIPROT, entry);
        QueryResponse response = storeManager.querySolr(DataStoreManager.StoreType.UNIPROT, "*:*");
        List<String> results = response.getBeans(UniProtDocument.class).stream().map(doc -> doc.accession)
                .collect(Collectors.toList());
        assertThat(results, Matchers.contains(accession));
    }

    @Test
    void canAddAndFetchEntriesInVoldemort() {
        UniProtEntry entry = UniProtEntryMocker.create(UniProtEntryMocker.Type.SP);
        String accession = entry.getPrimaryAccession().getValue();
        storeManager.saveToVoldemort(DataStoreManager.StoreType.UNIPROT, entry);
        List<UniProtEntry> voldemortEntries = storeManager.getVoldemortEntries(DataStoreManager.StoreType.UNIPROT, accession);
        assertThat(voldemortEntries, hasSize(1));
        assertThat(voldemortEntries.get(0), Matchers.is(entry));
    }

    @Test
    void canAddAndFetchEntriesInSolrAndVoldemort() throws IOException, SolrServerException {
        UniProtEntry entry = UniProtEntryMocker.create(UniProtEntryMocker.Type.SP);
        String accession = entry.getPrimaryAccession().getValue();
        storeManager.save(DataStoreManager.StoreType.UNIPROT, entry);

        QueryResponse response = storeManager.querySolr(DataStoreManager.StoreType.UNIPROT, "*:*");
        List<String> results = response.getBeans(UniProtDocument.class).stream().map(doc -> doc.accession)
                .collect(Collectors.toList());
        assertThat(results, Matchers.contains(accession));

        List<UniProtEntry> voldemortEntries = storeManager.getVoldemortEntries(DataStoreManager.StoreType.UNIPROT, accession);
        assertThat(voldemortEntries, hasSize(1));
        assertThat(voldemortEntries.get(0), Matchers.is(entry));
    }

    private static class FakeStoreClient extends UUWStoreClient<UniProtEntry> {

        FakeStoreClient(VoldemortClient<UniProtEntry> client) {
            super(client);
        }
    }
}
