package uk.ac.ebi.uniprot.uniprotkb.repository.search;

import org.hamcrest.CoreMatchers;
import org.hamcrest.Matchers;
import org.hamcrest.collection.IsCollectionWithSize;
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
import org.springframework.test.context.junit4.SpringRunner;
import uk.ac.ebi.uniprot.common.repository.DataStoreManager;
import uk.ac.ebi.uniprot.common.repository.search.QueryResult;
import uk.ac.ebi.uniprot.common.repository.search.SolrCollection;
import uk.ac.ebi.uniprot.common.repository.search.SolrQueryBuilder;
import uk.ac.ebi.uniprot.common.repository.search.SolrQueryRepository;
import uk.ac.ebi.uniprot.common.repository.search.facet.FacetConfigConverter;
import uk.ac.ebi.uniprot.common.repository.search.page.impl.CursorPage;
import uk.ac.ebi.uniprot.dataservice.document.uniprot.UniProtDocument;
import uk.ac.ebi.uniprot.uniprotkb.configuration.UniProtField;
import uk.ac.ebi.uniprot.uniprotkb.repository.DataStoreTestConfig;
import uk.ac.ebi.uniprot.uniprotkb.repository.search.impl.UniprotFacetConfig;
import uk.ac.ebi.uniprot.uniprotkb.repository.search.mockers.UniProtDocMocker;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static java.util.Arrays.asList;
import static org.hamcrest.MatcherAssert.assertThat;

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
    private DataStoreManager storeManager;

    @Autowired
    private UniprotFacetConfig facetConverter;

    private static GeneralSolrQueryRepository queryRepo;

    @Before
    public void setUp() {
        queryRepo =
                new GeneralSolrQueryRepository(template, SolrCollection.uniprot, UniProtDocument.class, facetConverter);
        storeManager.cleanSolr(DataStoreManager.StoreType.UNIPROT);
    }

    // getEntry -------------------
    @Test
    public void getEntrySucceeds() {
        // given
        String acc = "P12345";
        storeManager.saveDocs(DataStoreManager.StoreType.UNIPROT, UniProtDocMocker.createDoc(acc));

        // when
        Optional<UniProtDocument> entry = queryRepo.getEntry(queryWithFacets("accession:" + acc));

        // then
        assertThat(entry.isPresent(), CoreMatchers.is(true));
        assertThat(entry.get().accession, CoreMatchers.is(acc));
    }

    @Test
    public void getEntryWhenNotPresent() {
        // when
        String acc = "XXXXXX";
        Optional<UniProtDocument> entry = queryRepo.getEntry(queryWithFacets("accession:" + acc));

        // then
        assertThat(entry.isPresent(), CoreMatchers.is(false));
    }

    // searchCursorPage -------------------
    @Test
    public void searchCursorPageSucceeds() {
        // given
        int docCount = 10;
        List<UniProtDocument> docs = UniProtDocMocker.createDocs(docCount);
        storeManager.saveDocs(DataStoreManager.StoreType.UNIPROT, docs);
        List<String> savedAccs =
                docs
                        .stream()
                        .map(doc -> doc.accession)
                        .collect(Collectors.toList());
        List<String> expectedPage1Accs = asList(savedAccs.get(0), savedAccs.get(1));
        List<String> expectedPage2Accs = asList(savedAccs.get(2), savedAccs.get(3));

        // when attempt to fetch page 1
        String accQuery = "accession:*";
        QueryResult<UniProtDocument> queryResult = queryRepo.searchPage(queryWithFacets(accQuery), null, 2);
        List<String> page1Accs = queryResult.getContent().stream().map(doc -> doc.accession)
                .collect(Collectors.toList());
        String nextCursor = ((CursorPage) queryResult.getPage()).getEncryptedNextCursor();

        // ... and attempt to fetch page 2
        queryResult = queryRepo.searchPage(queryWithFacets(accQuery), nextCursor, 2);
        List<String> page2Accs = queryResult.getContent().stream().map(doc -> doc.accession)
                .collect(Collectors.toList());

        // then
        assertThat(page1Accs, CoreMatchers.is(expectedPage1Accs));
        assertThat(page2Accs, CoreMatchers.is(expectedPage2Accs));
    }

    @Test
    public void facetsNotRequested() {
        // given
        int docCount = 10;
        List<UniProtDocument> docs = UniProtDocMocker.createDocs(docCount);
        storeManager.saveDocs(DataStoreManager.StoreType.UNIPROT, docs);

        // when attempt to fetch results with no facets
        String accQuery = "accession:*";
        QueryResult<UniProtDocument> queryResult = queryRepo.searchPage(queryWithoutFacets(accQuery), null, 2);

        // then
        assertThat(queryResult.getFacets(), IsCollectionWithSize.hasSize(0));
    }

    @Test
    public void facetsRequested() {
        // given
        int docCount = 10;
        List<UniProtDocument> docs = UniProtDocMocker.createDocs(docCount);
        storeManager.saveDocs(DataStoreManager.StoreType.UNIPROT, docs);

        // when attempt to fetch results with facets
        String accQuery = "accession:*";
        QueryResult<UniProtDocument> queryResult = queryRepo.searchPage(queryWithFacets(accQuery), null, 2);

        // then
        assertThat(queryResult.getFacets(), IsCollectionWithSize.hasSize(Matchers.greaterThan(0)));
    }

    private SimpleQuery queryWithFacets(String query) {
        SimpleQuery simpleQuery = SolrQueryBuilder.of(query, facetConverter).build();
        simpleQuery.addSort(new Sort(Sort.Direction.ASC, UniProtField.Sort.accession.getSolrFieldName()));
        return simpleQuery;
    }

    private SimpleQuery queryWithoutFacets(String query) {
        SimpleQuery simpleQuery = SolrQueryBuilder.of(query).build();
        simpleQuery.addSort(new Sort(Sort.Direction.ASC, UniProtField.Sort.accession.getSolrFieldName()));
        return simpleQuery;
    }

    public static class GeneralSolrQueryRepository extends SolrQueryRepository<UniProtDocument> {
        GeneralSolrQueryRepository(SolrTemplate solrTemplate, SolrCollection collection, Class<UniProtDocument> docClass, FacetConfigConverter facetConverter) {
            super(solrTemplate, collection, docClass, facetConverter);
        }
    }

    @Configuration
    @EnableAutoConfiguration
    @Import({RepositoryConfig.class, DataStoreTestConfig.class, UniprotFacetConfig.class})
    public static class FakeApplication {
    }
}