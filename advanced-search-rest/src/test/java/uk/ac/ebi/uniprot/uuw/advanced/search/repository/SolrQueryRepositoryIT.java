package uk.ac.ebi.uniprot.uuw.advanced.search.repository;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrServerException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Sort;
import org.springframework.data.solr.core.SolrTemplate;
import org.springframework.data.solr.core.query.SimpleQuery;
import org.springframework.data.solr.core.query.result.Cursor;
import org.springframework.test.context.junit4.SpringRunner;
import uk.ac.ebi.uniprot.dataservice.document.Document;
import uk.ac.ebi.uniprot.dataservice.document.uniprot.UniProtDocument;
import uk.ac.ebi.uniprot.uuw.advanced.search.model.response.QueryResult;
import uk.ac.ebi.uniprot.uuw.advanced.search.model.response.page.impl.CursorPage;
import uk.ac.ebi.uniprot.uuw.advanced.search.repository.facet.FacetConfigConverter;
import uk.ac.ebi.uniprot.uuw.advanced.search.repository.impl.uniprot.UniprotFacetConfig;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

import static java.util.Arrays.asList;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static uk.ac.ebi.uniprot.uuw.advanced.search.repository.UniProtDocMocker.createDoc;
import static uk.ac.ebi.uniprot.uuw.advanced.search.repository.UniProtDocMocker.createDocs;

/**
 * Created 17/09/18
 *
 * @author Edd
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = SolrQueryRepositoryIT.FakeApplication.class)
public class SolrQueryRepositoryIT {
    @Autowired
    private SolrTemplate template;

    @Autowired
    private SolrClient uniProtSolrClient;

    @Autowired
    private UniprotFacetConfig facetConverter;

    private static GeneralSolrQueryRespository queryRepo;

    @Before
    public void setUp() throws IOException, SolrServerException {
        queryRepo =
                new GeneralSolrQueryRespository(template, SolrCollection.uniprot, UniProtDocument.class, facetConverter);
        uniProtSolrClient.deleteByQuery("*:*");
    }

    // getEntry -------------------
    @Test
    public void getEntrySucceeds() throws IOException, SolrServerException {
        // given
        String acc = "P12345";
        saveDocuments(createDoc(acc));

        // when
        Optional<UniProtDocument> entry = queryRepo.getEntry(query("accession:" + acc));

        // then
        assertThat(entry.isPresent(), is(true));
        assertThat(entry.get().accession, is(acc));
    }

    @Test
    public void getEntryWhenNotPresent() {
        // when
        String acc = "XXXXXX";
        Optional<UniProtDocument> entry = queryRepo.getEntry(query("accession:" + acc));

        // then
        assertThat(entry.isPresent(), is(false));
    }

    // getAll -------------------
    @Test
    public void getAllSucceeds() throws IOException, SolrServerException {
        // given
        int docCount = 100;
        List<String> savedAccs =
                saveDocuments(docCount)
                        .stream()
                        .map(doc -> doc.accession)
                        .collect(Collectors.toList());
        Set<String> uniqSavedAccs = new HashSet<>(savedAccs);

        // when
        Cursor<UniProtDocument> docCursor = queryRepo.getAll(query("accession:*"));

        List<String> foundAccs = new ArrayList<>();
        docCursor.forEachRemaining(doc -> foundAccs.add(doc.accession));
        Set<String> uniqFoundAccs = new HashSet<>(foundAccs);

        // then
        assertThat(savedAccs, hasSize(docCount));
        assertThat(foundAccs, hasSize(docCount));
        assertThat(uniqSavedAccs, is(uniqFoundAccs));
    }

    // searchPage -------------------
    @Test
    public void searchPageSucceeds() throws IOException, SolrServerException {
        // given
        int docCount = 10;
        List<String> savedAccs =
                saveDocuments(docCount)
                        .stream()
                        .map(doc -> doc.accession)
                        .collect(Collectors.toList());
        List<String> expectedAccs = asList(savedAccs.get(5), savedAccs.get(6));

        // when
        QueryResult<UniProtDocument> queryResult = queryRepo.searchPage(query("accession:*"), 5L, 2);
        List<String> pageAccs = queryResult.getContent().stream().map(doc -> doc.accession)
                .collect(Collectors.toList());

        // then
        assertThat(pageAccs, is(expectedAccs));
    }

    // searchCursorPage -------------------
    @Test
    public void searchCursorPageSucceeds() throws IOException, SolrServerException {
        // given
        int docCount = 10;
        List<String> savedAccs =
                saveDocuments(docCount)
                        .stream()
                        .map(doc -> doc.accession)
                        .collect(Collectors.toList());
        List<String> expectedPage1Accs = asList(savedAccs.get(0), savedAccs.get(1));
        List<String> expectedPage2Accs = asList(savedAccs.get(2), savedAccs.get(3));

        // when attempt to fetch page 1
        String accQuery = "accession:*";
        QueryResult<UniProtDocument> queryResult = queryRepo.searchCursorPage(query(accQuery), null, 2);
        List<String> page1Accs = queryResult.getContent().stream().map(doc -> doc.accession)
                .collect(Collectors.toList());
        String nextCursor = ((CursorPage)queryResult.getPage()).getEncryptedNextCursor();

        // ... and attempt to fetch page 2
        queryResult = queryRepo.searchCursorPage(query(accQuery), nextCursor, 2);
        List<String> page2Accs = queryResult.getContent().stream().map(doc -> doc.accession)
                .collect(Collectors.toList());

        // then
        assertThat(page1Accs, is(expectedPage1Accs));
        assertThat(page2Accs, is(expectedPage2Accs));
    }

    private SimpleQuery query(String query) {
        SimpleQuery simpleQuery = new SimpleQuery(query);
        simpleQuery.addSort(new Sort(Sort.Direction.ASC, "accession"));
        return simpleQuery;
    }

    public static class GeneralSolrQueryRespository extends SolrQueryRepository<UniProtDocument> {
        GeneralSolrQueryRespository(SolrTemplate solrTemplate, SolrCollection collection, Class<UniProtDocument> docClass, FacetConfigConverter facetConverter) {
            super(solrTemplate, collection, docClass, facetConverter);
        }
    }

    @Configuration
    @EnableAutoConfiguration
    @Import({RepositoryConfig.class, SolrClientTestConfig.class, UniprotFacetConfig.class})
    public static class FakeApplication {
    }

    private void saveDocuments(Document... docs) throws IOException, SolrServerException {
        for (Document doc : docs) {
            uniProtSolrClient.addBean(doc);
        }
        uniProtSolrClient.commit();
    }

    private List<UniProtDocument> saveDocuments(int docCount) throws IOException, SolrServerException {
        List<UniProtDocument> addedDocs = createDocs(docCount);
        uniProtSolrClient.addBeans(addedDocs);
        uniProtSolrClient.commit();
        return addedDocs;
    }
}