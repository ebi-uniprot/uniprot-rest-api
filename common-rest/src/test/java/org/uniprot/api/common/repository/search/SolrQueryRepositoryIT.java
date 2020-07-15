package org.uniprot.api.common.repository.search;

import static java.util.Arrays.asList;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.*;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.hamcrest.CoreMatchers;
import org.hamcrest.Matchers;
import org.hamcrest.collection.IsCollectionWithSize;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.web.util.UriComponentsBuilder;
import org.uniprot.api.common.exception.InvalidRequestException;
import org.uniprot.api.common.repository.search.facet.FakeFacetConfig;
import org.uniprot.api.common.repository.search.page.impl.CursorPage;
import org.uniprot.api.rest.request.SearchRequest;
import org.uniprot.store.indexer.DataStoreManager;
import org.uniprot.store.indexer.uniprot.mockers.UniProtDocMocker;
import org.uniprot.store.search.SolrCollection;
import org.uniprot.store.search.document.uniprot.UniProtDocument;

/** @author lgonzales */
class SolrQueryRepositoryIT {

    private static GeneralSolrQueryRepository queryRepo;

    @RegisterExtension static DataStoreManager storeManager = new DataStoreManager();;

    @BeforeAll
    static void setUp() {
        try {
            storeManager.addSolrClient(DataStoreManager.StoreType.UNIPROT, SolrCollection.uniprot);
            SolrClient solrClient = storeManager.getSolrClient(DataStoreManager.StoreType.UNIPROT);
            queryRepo = new GeneralSolrQueryRepository(solrClient);
        } catch (Exception e) {
            fail("Error to setup SolrQueryRepositoryTest", e);
        }
    }

    @AfterEach
    void cleanUp() {
        storeManager.cleanSolr(DataStoreManager.StoreType.UNIPROT);
    }

    @AfterAll
    static void close() {
        storeManager.close();
    }

    // getEntry -------------------
    @Test
    void getEntrySucceeds() {
        // given
        String acc = "P12345";
        storeManager.saveDocs(DataStoreManager.StoreType.UNIPROT, UniProtDocMocker.createDoc(acc));

        // when
        Optional<UniProtDocument> entry =
                queryRepo.getEntry(queryWithoutFacets("accession:" + acc));

        // then
        assertThat(entry.isPresent(), CoreMatchers.is(true));
        assertThat(entry.orElse(new UniProtDocument()).accession, CoreMatchers.is(acc));
    }

    @Test
    void getEntryWhenNotPresent() {
        // when
        String acc = "XXXXXX";
        Optional<UniProtDocument> entry =
                queryRepo.getEntry(queryWithoutFacets("accession:" + acc));

        // then
        assertThat(entry.isPresent(), CoreMatchers.is(false));
    }

    @Test
    void invalidQueryExceptionReturned() {
        SolrRequest request = queryWithoutFacets("invalid:invalid");
        QueryRetrievalException thrown =
                assertThrows(
                        QueryRetrievalException.class,
                        () -> queryRepo.getEntry(request));

        assertEquals("Error executing solr query", thrown.getMessage());
    }

    @Test
    void singlePageResult() {
        // given
        int docCount = 2;
        List<UniProtDocument> docs = UniProtDocMocker.createDocs(docCount);
        storeManager.saveDocs(DataStoreManager.StoreType.UNIPROT, docs);
        List<String> savedAccs =
                docs.stream().map(doc -> doc.accession).collect(Collectors.toList());

        List<String> expectedPage1Accs = asList(savedAccs.get(0), savedAccs.get(1));

        // when attempt to fetch page 1
        String accQuery = "accession:*";
        CursorPage page = CursorPage.of(null, 2);
        QueryResult<UniProtDocument> queryResult =
                queryRepo.searchPage(queryWithoutFacets(accQuery, 2), page.getCursor());
        List<String> page1Accs =
                queryResult.getContent().map(doc -> doc.accession).collect(Collectors.toList());

        // then
        assertNotNull(queryResult.getPage());
        page = (CursorPage) queryResult.getPage();
        assertFalse(
                page.getNextPageLink(UriComponentsBuilder.fromHttpUrl("http://localhost/test"))
                        .isPresent());

        assertThat(page1Accs, CoreMatchers.is(expectedPage1Accs));

        assertNotNull(queryResult.getFacets());
        assertTrue(queryResult.getFacets().isEmpty());
    }

    @Test
    void defaultSearch() {
        // given
        UniProtDocument doc = UniProtDocMocker.createDoc("P21802");
        doc.content.add("default value");
        storeManager.saveDocs(DataStoreManager.StoreType.UNIPROT, doc);

        // when attempt to fetch page 1
        String accQuery = "default value";
        QueryResult<UniProtDocument> queryResult =
                queryRepo.searchPage(queryWithoutFacets(accQuery), null);

        // then
        assertNotNull(queryResult.getPage());
        CursorPage page = (CursorPage) queryResult.getPage();
        assertFalse(
                page.getNextPageLink(UriComponentsBuilder.fromHttpUrl("http://localhost/test"))
                        .isPresent());

        assertNotNull(queryResult.getContent());
        assertEquals(1L, queryResult.getContent().count());

        assertNotNull(queryResult.getFacets());
        assertTrue(queryResult.getFacets().isEmpty());
    }

    @Test
    void defaultSearchWithMatchedFieldsRequested() {
        // given
        UniProtDocument doc1 = UniProtDocMocker.createDoc("P21802");
        String findMe = "FIND_ME";
        doc1.proteinNames.add("this is a protein name " + findMe + ".");
        UniProtDocument doc2 = UniProtDocMocker.createDoc("P21803");
        doc2.keywords.add("this is a keyword " + findMe + ", yes it is.");

        storeManager.saveDocs(DataStoreManager.StoreType.UNIPROT, doc1, doc2);

        // when attempt to fetch page 1
        QueryResult<UniProtDocument> queryResult =
                queryRepo.searchPage(queryWithMatchedFields(findMe), null);

        // then
        assertNotNull(queryResult.getMatchedFields());
    }

    @Test
    void getAllReturnsAllResultsInCorrectOrder() {
        // given
        int docCount = 52;
        List<UniProtDocument> docs = UniProtDocMocker.createDocs(docCount);
        storeManager.saveDocs(DataStoreManager.StoreType.UNIPROT, docs);
        List<String> savedAccs =
                docs.stream()
                        .map(doc -> doc.accession)
                        .sorted(Comparator.reverseOrder())
                        .collect(Collectors.toList());

        // when
        Stream<UniProtDocument> docStream = queryRepo.getAll(queryAllReverseOrderedByAccession());

        // then
        List<String> retrievedAccs =
                docStream.map(doc -> doc.accession).collect(Collectors.toList());
        assertThat(savedAccs, is(retrievedAccs));
    }

    @Test
    void invalidDefaultSearchWithMatchedFieldsRequested() {
        // given
        UniProtDocument doc1 = UniProtDocMocker.createDoc("P21802");
        String findMe = "FIND_ME";
        doc1.proteinNames.add("this is a protein name " + findMe + ".");
        UniProtDocument doc2 = UniProtDocMocker.createDoc("P21803");
        doc2.keywords.add("this is a keyword " + findMe + ", yes it is.");

        storeManager.saveDocs(DataStoreManager.StoreType.UNIPROT, doc1, doc2);

        // when attempt to fetch then error occurs
        SolrRequest request = queryWithMatchedFields("accession:" + findMe);
        assertThrows(
                InvalidRequestException.class,
                () -> queryRepo.searchPage(request, null));
    }

    @Test
    void iterateOverAllThreePages() {
        // given
        int docCount = 5;
        List<UniProtDocument> docs = UniProtDocMocker.createDocs(docCount);
        storeManager.saveDocs(DataStoreManager.StoreType.UNIPROT, docs);
        List<String> savedAccs =
                docs.stream().map(doc -> doc.accession).collect(Collectors.toList());

        List<String> expectedPage1Accs = asList(savedAccs.get(0), savedAccs.get(1));
        List<String> expectedPage2Accs = asList(savedAccs.get(2), savedAccs.get(3));
        List<String> expectedPage3Accs = Collections.singletonList(savedAccs.get(4));

        // when attempt to fetch page 1
        String accQuery = "accession:*";
        int size = 2;
        CursorPage page = CursorPage.of(null, size);
        QueryResult<UniProtDocument> queryResult =
                queryRepo.searchPage(queryWithoutFacets(accQuery, size), page.getCursor());
        List<String> page1Accs =
                queryResult.getContent().map(doc -> doc.accession).collect(Collectors.toList());

        assertNotNull(queryResult.getPage());
        page = (CursorPage) queryResult.getPage();
        assertTrue(
                page.getNextPageLink(UriComponentsBuilder.fromHttpUrl("http://localhost/test"))
                        .isPresent());
        String nextCursor = page.getEncryptedNextCursor();

        // ... and attempt to fetch page 2
        queryResult = queryRepo.searchPage(queryWithoutFacets(accQuery, size), nextCursor);
        List<String> page2Accs =
                queryResult.getContent().map(doc -> doc.accession).collect(Collectors.toList());

        assertNotNull(queryResult.getPage());
        page = (CursorPage) queryResult.getPage();
        assertTrue(
                page.getNextPageLink(UriComponentsBuilder.fromHttpUrl("http://localhost/test"))
                        .isPresent());
        nextCursor = page.getEncryptedNextCursor();

        // ... and attempt to fetch last page 3
        queryResult = queryRepo.searchPage(queryWithoutFacets(accQuery, size), nextCursor);
        List<String> page3Accs =
                queryResult.getContent().map(doc -> doc.accession).collect(Collectors.toList());

        page = (CursorPage) queryResult.getPage();
        assertFalse(
                page.getNextPageLink(UriComponentsBuilder.fromHttpUrl("http://localhost/test"))
                        .isPresent());

        // then
        assertThat(page1Accs, CoreMatchers.is(expectedPage1Accs));
        assertThat(page2Accs, CoreMatchers.is(expectedPage2Accs));
        assertThat(page3Accs, CoreMatchers.is(expectedPage3Accs));
    }

    @Test
    void facetsNotRequested() {
        // given
        int docCount = 10;
        List<UniProtDocument> docs = UniProtDocMocker.createDocs(docCount);
        storeManager.saveDocs(DataStoreManager.StoreType.UNIPROT, docs);

        // when attempt to fetch results with no facets
        String accQuery = "accession:*";
        QueryResult<UniProtDocument> queryResult =
                queryRepo.searchPage(queryWithoutFacets(accQuery), null);

        // then
        assertThat(queryResult.getFacets(), IsCollectionWithSize.hasSize(0));
    }

    @Test
    void singleFacetRequested() {
        // given
        int docCount = 10;
        List<UniProtDocument> docs = UniProtDocMocker.createDocs(docCount);
        storeManager.saveDocs(DataStoreManager.StoreType.UNIPROT, docs);

        // when attempt to fetch results with facets
        String accQuery = "accession:*";
        SolrRequest query = queryWithFacets(accQuery, Collections.singletonList("reviewed"), 2);
        QueryResult<UniProtDocument> queryResult = queryRepo.searchPage(query, null);

        // then
        assertThat(queryResult.getFacets(), IsCollectionWithSize.hasSize(Matchers.is(1)));
    }

    @Test
    void multiplesFacetRequested() {
        // given
        int docCount = 10;
        List<UniProtDocument> docs = UniProtDocMocker.createDocs(docCount);
        storeManager.saveDocs(DataStoreManager.StoreType.UNIPROT, docs);

        // when attempt to fetch results with facets
        String accQuery = "accession:*";
        SolrRequest query = queryWithFacets(accQuery, asList("reviewed", "fragment"), 2);
        QueryResult<UniProtDocument> queryResult = queryRepo.searchPage(query, null);

        // then
        assertThat(queryResult.getFacets(), IsCollectionWithSize.hasSize(Matchers.is(2)));
    }

    private SolrRequest queryAllReverseOrderedByAccession() {
        return SolrRequest.builder()
                .query("*:*")
                .sort(SolrQuery.SortClause.desc("accession_id"))
                .rows(SearchRequest.DEFAULT_RESULTS_SIZE)
                .build();
    }

    private SolrRequest queryWithFacets(String query, List<String> facets) {
        return queryWithFacets(query, facets, SearchRequest.DEFAULT_RESULTS_SIZE);
    }

    private SolrRequest queryWithFacets(String query, List<String> facets, int size) {
        return SolrRequest.builder()
                .query(query)
                .defaultQueryOperator(QueryOperator.AND)
                .filterQuery("active:true")
                .facetConfig(new FakeFacetConfig())
                .facets(facets)
                .sort(SolrQuery.SortClause.asc("accession_id"))
                .rows(size)
                .build();
    }

    private SolrRequest queryWithoutFacets(String query) {
        return queryWithoutFacets(query, SearchRequest.DEFAULT_RESULTS_SIZE);
    }

    private SolrRequest queryWithoutFacets(String query, int size) {
        return SolrRequest.builder()
                .query(query)
                .sort(SolrQuery.SortClause.asc("accession_id"))
                .rows(size)
                .build();
    }

    private SolrRequest queryWithMatchedFields(String query) {
        return SolrRequest.builder()
                .query(query)
                .termQuery(query)
                .termField("keyword")
                .termField("protein_name")
                .sort(SolrQuery.SortClause.asc("accession_id"))
                .rows(SearchRequest.DEFAULT_RESULTS_SIZE)
                .build();
    }

    private static class GeneralSolrQueryRepository extends SolrQueryRepository<UniProtDocument> {
        GeneralSolrQueryRepository(SolrClient solrClient) {
            super(
                    solrClient,
                    SolrCollection.uniprot,
                    UniProtDocument.class,
                    new FakeFacetConfig(),
                    new GeneralSolrRequestConverter());
        }
    }

    private static class GeneralSolrRequestConverter extends SolrRequestConverter {
        @Override
        public SolrQuery toSolrQuery(SolrRequest request) {
            SolrQuery solrQuery = super.toSolrQuery(request);

            // required for tests, because EmbeddedSolrServer is not sharded
            solrQuery.setParam("distrib", "false");
            solrQuery.setParam("terms.mincount", "1");

            return solrQuery;
        }
    }
}
