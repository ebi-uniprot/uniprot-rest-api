package org.uniprot.api.uniprotkb.controller;

import org.apache.solr.client.solrj.SolrClient;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.client.AutoConfigureWebClient;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.uniprot.api.rest.download.AsyncDownloadMocks;
import org.uniprot.api.uniprotkb.repository.DataStoreTestConfig;
import org.uniprot.core.cv.keyword.KeywordId;
import org.uniprot.core.cv.keyword.impl.KeywordEntryBuilder;
import org.uniprot.core.cv.keyword.impl.KeywordIdBuilder;
import org.uniprot.core.json.parser.keyword.KeywordJsonConfig;
import org.uniprot.store.indexer.DataStoreManager;
import org.uniprot.store.indexer.DataStoreManager.StoreType;
import org.uniprot.store.search.SolrCollection;
import org.uniprot.store.search.document.Document;
import org.uniprot.store.search.document.keyword.KeywordDocument;
import org.uniprot.store.search.document.uniprot.UniProtDocument;

import java.nio.ByteBuffer;
import java.util.List;

import static org.hamcrest.Matchers.containsStringIgnoringCase;
import static org.hamcrest.core.Is.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.log;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.uniprot.store.indexer.DataStoreManager.StoreType.KEYWORD;
import static org.uniprot.store.indexer.DataStoreManager.StoreType.UNIPROT;

@ExtendWith(SpringExtension.class)
@WebMvcTest(controllers = UniProtKBViewByController.class)
@ContextConfiguration(classes = {DataStoreTestConfig.class, AsyncDownloadMocks.class})
@AutoConfigureWebClient
@ActiveProfiles({"viewbyTest", "viewByKeywordTest"})
class UniProtKBViewByKeywordControllerIT {
    private static final String EMPTY_PARENT = "";
    private static final String ACCESSION_0 = "A0";
    private static final String ACCESSION_1 = "A1";
    private static final String ACCESSION_2 = "A2";
    private static final String ORGANISM_ID_0 = "29";
    private static final String ORGANISM_ID_1 = "324";
    private static final String ORGANISM_ID_2 = "994";
    private static final String KEYWORD_ID_0 = "keywordId0";
    private static final String KEYWORD_NAME_0 = "keywordName0";
    private static final String KEYWORD_ID_1 = "keywordId1";
    private static final String KEYWORD_NAME_1 = "keywordName1";
    private static final String KEYWORD_ID_2 = "keywordId2";
    private static final String KEYWORD_NAME_2 = "keywordName2";
    private static final String KEYWORD_ID_3 = "keywordId3";
    private static final String KEYWORD_NAME_3 = "keywordName3";
    private static final String KEYWORD_ID_4 = "keywordId4";
    private static final String KEYWORD_NAME_4 = "keywordName4";
    public static final String PATH = "/uniprotkb/view/keyword";
    @Autowired private MockMvc mockMvc;
    @RegisterExtension static DataStoreManager dataStoreManager = new DataStoreManager();

    @TestConfiguration
    @Profile("viewByKeywordTest")
    static class TestConfig {
        @Bean("uniProtKBSolrClient")
        public SolrClient uniProtKBSolrClient() {
            return dataStoreManager.getSolrClient(UNIPROT);
        }

        @Bean
        public SolrClient solrClient() {
            return dataStoreManager.getSolrClient(KEYWORD);
        }
    }

    @BeforeAll
    static void beforeAll() {
        dataStoreManager.addSolrClient(UNIPROT, SolrCollection.uniprot);
        dataStoreManager.addSolrClient(KEYWORD, SolrCollection.keyword);
    }

    @AfterEach
    void tearDown() {
        dataStoreManager.cleanSolr(UNIPROT);
        dataStoreManager.cleanSolr(KEYWORD);
    }

    @Test
    void viewByKeyword_whenNoParentSpecifiedAndNoTraversalAndQuerySpecifiedWithField()
            throws Exception {
        prepareSingleRootNodeWithNoChildren();

        mockMvc.perform(
                        get(PATH)
                                .param("query", "organism_id:" + ORGANISM_ID_0)
                                .param("parent", EMPTY_PARENT))
                .andDo(log())
                .andExpect(jsonPath("$.results[0].id", is(KEYWORD_ID_0)))
                .andExpect(jsonPath("$.results[0].label", is(KEYWORD_NAME_0)))
                .andExpect(jsonPath("$.results[0].expand", is(false)))
                .andExpect(jsonPath("$.results[0].count", is(1)))
                .andExpect(jsonPath("$.results.size()", is(1)))
                .andExpect(jsonPath("$.ancestors.size()", is(0)));
    }

    @Test
    void viewByKeyword_whenNoParentSpecifiedAndNoTraversalAndFreeFormQuery() throws Exception {
        prepareSingleRootNodeWithNoChildren();

        mockMvc.perform(get(PATH).param("query", ORGANISM_ID_0).param("parent", EMPTY_PARENT))
                .andDo(log())
                .andExpect(jsonPath("$.results[0].id", is(KEYWORD_ID_0)))
                .andExpect(jsonPath("$.results[0].label", is(KEYWORD_NAME_0)))
                .andExpect(jsonPath("$.results[0].expand", is(false)))
                .andExpect(jsonPath("$.results[0].count", is(1)))
                .andExpect(jsonPath("$.results.size()", is(1)))
                .andExpect(jsonPath("$.ancestors.size()", is(0)));
    }

    @Test
    void viewByKeyword_whenNoParentSpecifiedAndTraversalAndQuerySpecifiedWithField()
            throws Exception {
        prepareSingleRootWithTwoLevelsOfChildren();

        mockMvc.perform(
                        get(PATH)
                                .param("query", "organism_id:" + ORGANISM_ID_2)
                                .param("parent", EMPTY_PARENT))
                .andDo(log())
                .andExpect(jsonPath("$.results[0].id", is(KEYWORD_ID_2)))
                .andExpect(jsonPath("$.results[0].label", is(KEYWORD_NAME_2)))
                .andExpect(jsonPath("$.results[0].expand", is(false)))
                .andExpect(jsonPath("$.results[0].count", is(1)))
                .andExpect(jsonPath("$.results.size()", is(1)))
                .andExpect(jsonPath("$.ancestors[0].id", is(KEYWORD_ID_0)))
                .andExpect(jsonPath("$.ancestors[0].label", is(KEYWORD_NAME_0)))
                .andExpect(jsonPath("$.ancestors[1].id", is(KEYWORD_ID_1)))
                .andExpect(jsonPath("$.ancestors[1].label", is(KEYWORD_NAME_1)))
                .andExpect(jsonPath("$.ancestors.size()", is(2)));
    }

    @Test
    void viewByKeyword_whenNoParentSpecifiedAndTraversalAndFreeFormQuery() throws Exception {
        prepareSingleRootWithTwoLevelsOfChildren();

        mockMvc.perform(get(PATH).param("query", ORGANISM_ID_2).param("parent", EMPTY_PARENT))
                .andDo(log())
                .andExpect(jsonPath("$.results[0].id", is(KEYWORD_ID_2)))
                .andExpect(jsonPath("$.results[0].label", is(KEYWORD_NAME_2)))
                .andExpect(jsonPath("$.results[0].expand", is(false)))
                .andExpect(jsonPath("$.results[0].count", is(1)))
                .andExpect(jsonPath("$.results.size()", is(1)))
                .andExpect(jsonPath("$.ancestors[0].id", is(KEYWORD_ID_0)))
                .andExpect(jsonPath("$.ancestors[0].label", is(KEYWORD_NAME_0)))
                .andExpect(jsonPath("$.ancestors[1].id", is(KEYWORD_ID_1)))
                .andExpect(jsonPath("$.ancestors[1].label", is(KEYWORD_NAME_1)))
                .andExpect(jsonPath("$.ancestors.size()", is(2)));
    }

    @Test
    void viewByKeyword_whenParentSpecifiedAndQuerySpecifiedWithField() throws Exception {
        prepareSingleRootWithTwoLevelsOfChildrenAndSidePaths();

        mockMvc.perform(
                        get(PATH)
                                .param("query", "organism_id:" + ORGANISM_ID_2)
                                .param("parent", KEYWORD_ID_0))
                .andDo(log())
                .andExpect(jsonPath("$.results[0].id", is(KEYWORD_ID_2)))
                .andExpect(jsonPath("$.results[0].label", is(KEYWORD_NAME_2)))
                .andExpect(jsonPath("$.results[0].expand", is(false)))
                .andExpect(jsonPath("$.results[0].count", is(1)))
                .andExpect(jsonPath("$.results.size()", is(1)))
                .andExpect(jsonPath("$.ancestors[0].id", is(KEYWORD_ID_1)))
                .andExpect(jsonPath("$.ancestors[0].label", is(KEYWORD_NAME_1)))
                .andExpect(jsonPath("$.ancestors.size()", is(1)));
    }

    @Test
    void viewByKeyword_whenParentNotSpecifiedAndTraversalAndFreeFormQuery() throws Exception {
        prepareSingleRootWithTwoLevelsOfChildren();

        mockMvc.perform(get(PATH).param("query", KEYWORD_ID_2).param("parent", EMPTY_PARENT))
                .andDo(log())
                .andExpect(jsonPath("$.results[0].id", is(KEYWORD_ID_2)))
                .andExpect(jsonPath("$.results[0].label", is(KEYWORD_NAME_2)))
                .andExpect(jsonPath("$.results[0].expand", is(false)))
                .andExpect(jsonPath("$.results[0].count", is(1)))
                .andExpect(jsonPath("$.results.size()", is(1)))
                .andExpect(jsonPath("$.ancestors[0].id", is(KEYWORD_ID_0)))
                .andExpect(jsonPath("$.ancestors[0].label", is(KEYWORD_NAME_0)))
                .andExpect(jsonPath("$.ancestors[1].id", is(KEYWORD_ID_1)))
                .andExpect(jsonPath("$.ancestors[1].label", is(KEYWORD_NAME_1)))
                .andExpect(jsonPath("$.ancestors.size()", is(2)));
    }

    @Test
    void viewByKeyword_whenParentSpecifiedAndTraversalAndFreeFormQuery() throws Exception {
        prepareSingleRootWithTwoLevelsOfChildrenAndSidePaths();

        mockMvc.perform(get(PATH).param("query", KEYWORD_ID_2).param("parent", KEYWORD_ID_0))
                .andDo(log())
                .andExpect(jsonPath("$.results[0].id", is(KEYWORD_ID_2)))
                .andExpect(jsonPath("$.results[0].label", is(KEYWORD_NAME_2)))
                .andExpect(jsonPath("$.results[0].expand", is(false)))
                .andExpect(jsonPath("$.results[0].count", is(1)))
                .andExpect(jsonPath("$.results.size()", is(1)))
                .andExpect(jsonPath("$.ancestors[0].id", is(KEYWORD_ID_1)))
                .andExpect(jsonPath("$.ancestors[0].label", is(KEYWORD_NAME_1)))
                .andExpect(jsonPath("$.ancestors.size()", is(1)));
    }

    @Test
    void viewByKeyword_emptyResults() throws Exception {
        prepareSingleRootNodeWithNoChildren();

        mockMvc.perform(
                        get(PATH)
                                .param("query", "organism_id:" + ORGANISM_ID_1)
                                .param("parent", EMPTY_PARENT))
                .andDo(log())
                .andExpect(jsonPath("$.results.size()", is(0)))
                .andExpect(jsonPath("$.ancestors.size()", is(0)));
    }

    @Test
    void viewByKeyword_whenFreeFormQueryAndEmptyResults() throws Exception {
        prepareSingleRootNodeWithNoChildren();

        mockMvc.perform(get(PATH).param("query", ORGANISM_ID_1).param("parent", EMPTY_PARENT))
                .andDo(log())
                .andExpect(jsonPath("$.results.size()", is(0)))
                .andExpect(jsonPath("$.ancestors.size()", is(0)));
    }

    @Test
    void viewByKeyword_whenQueryNotSpecified() throws Exception {
        mockMvc.perform(get(PATH).param("parent", EMPTY_PARENT))
                .andDo(log())
                .andExpect(status().isBadRequest())
                .andExpect(
                        content()
                                .string(
                                        containsStringIgnoringCase(
                                                "query is a required parameter")));
    }

    private void prepareSingleRootWithTwoLevelsOfChildren() throws Exception {
        saveKeywordDocument(KEYWORD_ID_0, KEYWORD_NAME_0, null);
        saveUniProtDocument(ACCESSION_0, ORGANISM_ID_0, List.of(KEYWORD_ID_0));
        saveKeywordDocument(KEYWORD_ID_1, KEYWORD_NAME_1, List.of(KEYWORD_ID_0));
        saveUniProtDocument(ACCESSION_1, ORGANISM_ID_1, List.of(KEYWORD_ID_0, KEYWORD_ID_1));
        saveKeywordDocument(KEYWORD_ID_2, KEYWORD_NAME_2, List.of(KEYWORD_ID_1));
        saveUniProtDocument(
                ACCESSION_2, ORGANISM_ID_2, List.of(KEYWORD_ID_0, KEYWORD_ID_1, KEYWORD_ID_2));
    }

    private void prepareSingleRootWithTwoLevelsOfChildrenAndSidePaths() throws Exception {
        saveKeywordDocument(KEYWORD_ID_0, KEYWORD_NAME_0, null);
        saveUniProtDocument(ACCESSION_0, ORGANISM_ID_0, List.of(KEYWORD_ID_0));
        saveKeywordDocument(KEYWORD_ID_1, KEYWORD_NAME_1, List.of(KEYWORD_ID_0, KEYWORD_ID_3));
        saveUniProtDocument(ACCESSION_1, ORGANISM_ID_1, List.of(KEYWORD_ID_0, KEYWORD_ID_1));
        saveKeywordDocument(KEYWORD_ID_2, KEYWORD_NAME_2, List.of(KEYWORD_ID_1, KEYWORD_ID_4));
        saveUniProtDocument(
                ACCESSION_2, ORGANISM_ID_2, List.of(KEYWORD_ID_0, KEYWORD_ID_1, KEYWORD_ID_2));
        saveKeywordDocument(KEYWORD_ID_3, KEYWORD_NAME_3, null);
        saveKeywordDocument(KEYWORD_ID_4, KEYWORD_NAME_4, null);
    }

    private void prepareSingleRootNodeWithNoChildren() throws Exception {
        saveKeywordDocument(KEYWORD_ID_0, KEYWORD_NAME_0, List.of());
        saveUniProtDocument(ACCESSION_0, ORGANISM_ID_0, List.of(KEYWORD_ID_0));
    }

    private void saveUniProtDocument(String accession, String organismId, List<String> keywords) {
        UniProtDocument uniProtDocument = new UniProtDocument();
        uniProtDocument.active = true;
        uniProtDocument.accession = accession;
        uniProtDocument.keywords = keywords;
        uniProtDocument.organismTaxId = Integer.parseInt(organismId);
        uniProtDocument.taxLineageIds = List.of(Integer.parseInt(organismId));
        save(UNIPROT, uniProtDocument);
    }

    private void saveKeywordDocument(String id, String name, List<String> parents)
            throws Exception {
        KeywordId keyword = new KeywordIdBuilder().name(name).id(id).build();
        ByteBuffer keywordObject =
                ByteBuffer.wrap(
                        KeywordJsonConfig.getInstance()
                                .getFullObjectMapper()
                                .writeValueAsBytes(
                                        new KeywordEntryBuilder().keyword(keyword).build()));
        KeywordDocument keywordDocument =
                KeywordDocument.builder()
                        .id(id)
                        .name(name)
                        .keywordObj(keywordObject)
                        .parent(parents)
                        .build();
        save(KEYWORD, keywordDocument);
    }

    void save(StoreType type, Document doc) {
        dataStoreManager.saveDocs(type, doc);
    }
}
