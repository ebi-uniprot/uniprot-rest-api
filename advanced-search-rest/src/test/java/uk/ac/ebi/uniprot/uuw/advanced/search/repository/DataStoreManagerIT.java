package uk.ac.ebi.uniprot.uuw.advanced.search.repository;

import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit4.SpringRunner;
import uk.ac.ebi.kraken.interfaces.uniprot.UniProtEntry;
import uk.ac.ebi.uniprot.dataservice.document.uniprot.UniProtDocument;
import uk.ac.ebi.uniprot.uuw.advanced.search.mockers.UniProtEntryMocker;
import uk.ac.ebi.uniprot.uuw.advanced.search.store.UniProtStoreConfig;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static uk.ac.ebi.uniprot.uuw.advanced.search.mockers.UniProtDocMocker.createDoc;
import static uk.ac.ebi.uniprot.uuw.advanced.search.mockers.UniProtEntryMocker.Type.SP;
import static uk.ac.ebi.uniprot.uuw.advanced.search.repository.DataStoreManager.StoreType.UNIPROT;

/**
 * Created 19/09/18
 *
 * @author Edd
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = SolrQueryRepositoryIT.FakeApplication.class)
public class DataStoreManagerIT {
    private static final String P12345 = "P12345";

    @Autowired
    private DataStoreManager storeManager;

    @Before
    public void setUp() {
        storeManager.cleanSolr(UNIPROT);
    }

    @Test
    public void canAddAndSearchDocumentsInSolr() throws IOException, SolrServerException {
        storeManager.saveDocs(UNIPROT, createDoc(P12345));
        QueryResponse response = storeManager.querySolr(UNIPROT, "accession:P12345");
        List<String> results = response.getBeans(UniProtDocument.class).stream().map(doc -> doc.accession)
                .collect(Collectors.toList());
        assertThat(results, contains(P12345));
    }

    @Test
    public void canAddEntriesAndSearchDocumentsInSolr() throws IOException, SolrServerException {
        storeManager.saveDocs(UNIPROT, createDoc(P12345));
        QueryResponse response = storeManager.querySolr(UNIPROT, "accession:P12345");
        List<String> results = response.getBeans(UniProtDocument.class).stream().map(doc -> doc.accession)
                .collect(Collectors.toList());
        assertThat(results, contains(P12345));
    }

    @Test
    public void canAddAndFetchEntriesInSolr() throws IOException, SolrServerException {
        UniProtEntry entry = UniProtEntryMocker.create(SP);
        String accession = entry.getPrimaryUniProtAccession().getValue();
        storeManager.saveEntriesInSolr(UNIPROT, entry);
        QueryResponse response = storeManager.querySolr(UNIPROT, "*:*");
        List<String> results = response.getBeans(UniProtDocument.class).stream().map(doc -> doc.accession)
                .collect(Collectors.toList());
        assertThat(results, contains(accession));
    }

    @Test
    public void canAddAndFetchEntriesInVoldemort() {
        UniProtEntry entry = UniProtEntryMocker.create(SP);
        String accession = entry.getPrimaryUniProtAccession().getValue();
        storeManager.saveToVoldemort(UNIPROT, entry);
        List<UniProtEntry> voldemortEntries = storeManager.getVoldemortEntries(UNIPROT, accession);
        assertThat(voldemortEntries, hasSize(1));
        assertThat(voldemortEntries.get(0), is(entry));
    }

    @Test
    public void canAddAndFetchEntriesInSolrAndVoldemort() throws IOException, SolrServerException {
        UniProtEntry entry = UniProtEntryMocker.create(SP);
        String accession = entry.getPrimaryUniProtAccession().getValue();
        storeManager.save(UNIPROT, entry);

        QueryResponse response = storeManager.querySolr(UNIPROT, "*:*");
        List<String> results = response.getBeans(UniProtDocument.class).stream().map(doc -> doc.accession)
                .collect(Collectors.toList());
        assertThat(results, contains(accession));

        List<UniProtEntry> voldemortEntries = storeManager.getVoldemortEntries(UNIPROT, accession);
        assertThat(voldemortEntries, hasSize(1));
        assertThat(voldemortEntries.get(0), is(entry));
    }

    @Configuration
    @EnableAutoConfiguration
    @Import({UniProtStoreConfig.class, RepositoryConfig.class, DataStoreTestConfig.class})
    public static class FakeApplication {
    }
}