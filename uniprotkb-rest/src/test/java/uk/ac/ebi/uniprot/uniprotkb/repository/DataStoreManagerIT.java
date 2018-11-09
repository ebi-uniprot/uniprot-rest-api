package uk.ac.ebi.uniprot.uniprotkb.repository;

import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.hamcrest.Matchers;
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
import uk.ac.ebi.uniprot.common.repository.DataStoreManager;
import uk.ac.ebi.uniprot.dataservice.document.uniprot.UniProtDocument;
import uk.ac.ebi.uniprot.uniprotkb.repository.search.RepositoryConfig;
import uk.ac.ebi.uniprot.uniprotkb.repository.search.SolrQueryRepositoryIT;
import uk.ac.ebi.uniprot.uniprotkb.repository.search.mockers.UniProtDocMocker;
import uk.ac.ebi.uniprot.uniprotkb.repository.search.mockers.UniProtEntryMocker;
import uk.ac.ebi.uniprot.uniprotkb.repository.store.UniProtStoreConfig;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static uk.ac.ebi.uniprot.common.repository.DataStoreManager.StoreType;
import static uk.ac.ebi.uniprot.uniprotkb.repository.search.mockers.UniProtEntryMocker.Type;

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
        storeManager.cleanSolr(StoreType.UNIPROT);
    }

    @Test
    public void canAddAndSearchDocumentsInSolr() throws IOException, SolrServerException {
        storeManager.saveDocs(StoreType.UNIPROT, UniProtDocMocker.createDoc(P12345));
        QueryResponse response = storeManager.querySolr(StoreType.UNIPROT, "accession:P12345");
        List<String> results = response.getBeans(UniProtDocument.class).stream().map(doc -> doc.accession)
                .collect(Collectors.toList());
        assertThat(results, Matchers.contains(P12345));
    }

    @Test
    public void canAddEntriesAndSearchDocumentsInSolr() throws IOException, SolrServerException {
        storeManager.saveDocs(StoreType.UNIPROT, UniProtDocMocker.createDoc(P12345));
        QueryResponse response = storeManager.querySolr(StoreType.UNIPROT, "accession:P12345");
        List<String> results = response.getBeans(UniProtDocument.class).stream().map(doc -> doc.accession)
                .collect(Collectors.toList());
        assertThat(results, Matchers.contains(P12345));
    }

    @Test
    public void canAddAndFetchEntriesInSolr() throws IOException, SolrServerException {
        UniProtEntry entry = UniProtEntryMocker.create(UniProtEntryMocker.Type.SP);
        String accession = entry.getPrimaryUniProtAccession().getValue();
        storeManager.saveEntriesInSolr(StoreType.UNIPROT, entry);
        QueryResponse response = storeManager.querySolr(StoreType.UNIPROT, "*:*");
        List<String> results = response.getBeans(UniProtDocument.class).stream().map(doc -> doc.accession)
                .collect(Collectors.toList());
        assertThat(results, Matchers.contains(accession));
    }

    @Test
    public void canAddAndFetchEntriesInVoldemort() {
        UniProtEntry entry = UniProtEntryMocker.create(Type.SP);
        String accession = entry.getPrimaryUniProtAccession().getValue();
        storeManager.saveToVoldemort(StoreType.UNIPROT, entry);
        List<UniProtEntry> voldemortEntries = storeManager.getVoldemortEntries(StoreType.UNIPROT, accession);
        assertThat(voldemortEntries, hasSize(1));
        assertThat(voldemortEntries.get(0), Matchers.is(entry));
    }

    @Test
    public void canAddAndFetchEntriesInSolrAndVoldemort() throws IOException, SolrServerException {
        UniProtEntry entry = UniProtEntryMocker.create(Type.SP);
        String accession = entry.getPrimaryUniProtAccession().getValue();
        storeManager.save(StoreType.UNIPROT, entry);

        QueryResponse response = storeManager.querySolr(StoreType.UNIPROT, "*:*");
        List<String> results = response.getBeans(UniProtDocument.class).stream().map(doc -> doc.accession)
                .collect(Collectors.toList());
        assertThat(results, Matchers.contains(accession));

        List<UniProtEntry> voldemortEntries = storeManager.getVoldemortEntries(StoreType.UNIPROT, accession);
        assertThat(voldemortEntries, hasSize(1));
        assertThat(voldemortEntries.get(0), Matchers.is(entry));
    }

    @Configuration
    @EnableAutoConfiguration
    @Import({UniProtStoreConfig.class, RepositoryConfig.class, DataStoreTestConfig.class})
    public static class FakeApplication {
    }
}