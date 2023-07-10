package org.uniprot.api.uniprotkb.controller;

import static org.hamcrest.core.Is.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.log;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.uniprot.store.indexer.DataStoreManager.StoreType.KEYWORD;
import static org.uniprot.store.indexer.DataStoreManager.StoreType.UNIPROT;

import java.nio.ByteBuffer;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.client.AutoConfigureWebClient;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.uniprot.api.support.data.common.keyword.repository.KeywordRepository;
import org.uniprot.api.uniprotkb.repository.search.impl.UniprotQueryRepository;
import org.uniprot.core.cv.keyword.KeywordId;
import org.uniprot.core.cv.keyword.impl.KeywordEntryBuilder;
import org.uniprot.core.cv.keyword.impl.KeywordIdBuilder;
import org.uniprot.core.json.parser.keyword.KeywordJsonConfig;
import org.uniprot.store.indexer.DataStoreManager;
import org.uniprot.store.search.SolrCollection;
import org.uniprot.store.search.document.keyword.KeywordDocument;
import org.uniprot.store.search.document.uniprot.UniProtDocument;

@ExtendWith(SpringExtension.class)
@WebMvcTest(controllers = GroupByController.class)
@AutoConfigureWebClient
@ActiveProfiles({"offline"})
class GroupByKeywordControllerIT extends GroupByControllerIT {
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
    public static final String PATH = "/uniprotkb/groups/keyword";
    @RegisterExtension static DataStoreManager dataStoreManager = new DataStoreManager();
    @Autowired private MockMvc mockMvc;
    @Autowired private UniprotQueryRepository uniprotQueryRepository;
    @Autowired private KeywordRepository keywordRepository;

    @BeforeAll
    static void beforeAll() {
        dataStoreManager.addSolrClient(UNIPROT, SolrCollection.uniprot);
        dataStoreManager.addSolrClient(KEYWORD, SolrCollection.keyword);
    }

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(
                uniprotQueryRepository, "solrClient", dataStoreManager.getSolrClient(UNIPROT));
        ReflectionTestUtils.setField(
                keywordRepository, "solrClient", dataStoreManager.getSolrClient(KEYWORD));
    }

    @AfterEach
    void tearDown() {
        dataStoreManager.cleanSolr(UNIPROT);
        dataStoreManager.cleanSolr(KEYWORD);
    }

    @Test
    void getGroupByKeyword_whenNoParentSpecifiedAndNoTraversalAndQuerySpecifiedWithField()
            throws Exception {
        prepareSingleRootNodeWithNoChildren();

        mockMvc.perform(
                        get(PATH)
                                .param("query", "organism_id:" + ORGANISM_ID_0)
                                .param("parent", EMPTY_PARENT))
                .andDo(log())
                .andExpect(jsonPath("$.groups[0].id", is(KEYWORD_ID_0)))
                .andExpect(jsonPath("$.groups[0].label", is(KEYWORD_NAME_0)))
                .andExpect(jsonPath("$.groups[0].expandable", is(false)))
                .andExpect(jsonPath("$.groups[0].count", is(1)))
                .andExpect(jsonPath("$.groups.size()", is(1)))
                .andExpect(jsonPath("$.ancestors.size()", is(0)));
    }

    @Test
    void getGroupByKeyword_whenNoParentSpecifiedAndNoTraversalAndFreeFormQuery() throws Exception {
        prepareSingleRootNodeWithNoChildren();

        mockMvc.perform(get(PATH).param("query", ORGANISM_ID_0).param("parent", EMPTY_PARENT))
                .andDo(log())
                .andExpect(jsonPath("$.groups[0].id", is(KEYWORD_ID_0)))
                .andExpect(jsonPath("$.groups[0].label", is(KEYWORD_NAME_0)))
                .andExpect(jsonPath("$.groups[0].expandable", is(false)))
                .andExpect(jsonPath("$.groups[0].count", is(1)))
                .andExpect(jsonPath("$.groups.size()", is(1)))
                .andExpect(jsonPath("$.ancestors.size()", is(0)));
    }

    @Test
    void getGroupByKeyword_whenNoParentSpecifiedAndTraversalAndQuerySpecifiedWithField()
            throws Exception {
        prepareSingleRootWithTwoLevelsOfChildren();

        mockMvc.perform(
                        get(PATH)
                                .param("query", "organism_id:" + ORGANISM_ID_2)
                                .param("parent", EMPTY_PARENT))
                .andDo(log())
                .andExpect(jsonPath("$.groups[0].id", is(KEYWORD_ID_2)))
                .andExpect(jsonPath("$.groups[0].label", is(KEYWORD_NAME_2)))
                .andExpect(jsonPath("$.groups[0].expandable", is(false)))
                .andExpect(jsonPath("$.groups[0].count", is(1)))
                .andExpect(jsonPath("$.groups.size()", is(1)))
                .andExpect(jsonPath("$.ancestors[0].id", is(KEYWORD_ID_0)))
                .andExpect(jsonPath("$.ancestors[0].label", is(KEYWORD_NAME_0)))
                .andExpect(jsonPath("$.ancestors[1].id", is(KEYWORD_ID_1)))
                .andExpect(jsonPath("$.ancestors[1].label", is(KEYWORD_NAME_1)))
                .andExpect(jsonPath("$.ancestors.size()", is(2)));
    }

    @Test
    void getGroupByKeyword_whenNoParentSpecifiedAndTraversalAndFreeFormQuery() throws Exception {
        prepareSingleRootWithTwoLevelsOfChildren();

        mockMvc.perform(get(PATH).param("query", ORGANISM_ID_2).param("parent", EMPTY_PARENT))
                .andDo(log())
                .andExpect(jsonPath("$.groups[0].id", is(KEYWORD_ID_2)))
                .andExpect(jsonPath("$.groups[0].label", is(KEYWORD_NAME_2)))
                .andExpect(jsonPath("$.groups[0].expandable", is(false)))
                .andExpect(jsonPath("$.groups[0].count", is(1)))
                .andExpect(jsonPath("$.groups.size()", is(1)))
                .andExpect(jsonPath("$.ancestors[0].id", is(KEYWORD_ID_0)))
                .andExpect(jsonPath("$.ancestors[0].label", is(KEYWORD_NAME_0)))
                .andExpect(jsonPath("$.ancestors[1].id", is(KEYWORD_ID_1)))
                .andExpect(jsonPath("$.ancestors[1].label", is(KEYWORD_NAME_1)))
                .andExpect(jsonPath("$.ancestors.size()", is(2)));
    }

    @Test
    void getGroupByKeyword_whenParentSpecifiedAndQuerySpecifiedWithField() throws Exception {
        prepareSingleRootWithTwoLevelsOfChildrenAndSidePaths();

        mockMvc.perform(
                        get(PATH)
                                .param("query", "organism_id:" + ORGANISM_ID_2)
                                .param("parent", KEYWORD_ID_0))
                .andDo(log())
                .andExpect(jsonPath("$.groups[0].id", is(KEYWORD_ID_2)))
                .andExpect(jsonPath("$.groups[0].label", is(KEYWORD_NAME_2)))
                .andExpect(jsonPath("$.groups[0].expandable", is(false)))
                .andExpect(jsonPath("$.groups[0].count", is(1)))
                .andExpect(jsonPath("$.groups.size()", is(1)))
                .andExpect(jsonPath("$.ancestors[0].id", is(KEYWORD_ID_1)))
                .andExpect(jsonPath("$.ancestors[0].label", is(KEYWORD_NAME_1)))
                .andExpect(jsonPath("$.ancestors.size()", is(1)));
    }

    @Test
    void getGroupByKeyword_whenParentNotSpecifiedAndTraversalAndFreeFormQuery() throws Exception {
        prepareSingleRootWithTwoLevelsOfChildren();

        mockMvc.perform(get(PATH).param("query", KEYWORD_ID_2).param("parent", EMPTY_PARENT))
                .andDo(log())
                .andExpect(jsonPath("$.groups[0].id", is(KEYWORD_ID_2)))
                .andExpect(jsonPath("$.groups[0].label", is(KEYWORD_NAME_2)))
                .andExpect(jsonPath("$.groups[0].expandable", is(false)))
                .andExpect(jsonPath("$.groups[0].count", is(1)))
                .andExpect(jsonPath("$.groups.size()", is(1)))
                .andExpect(jsonPath("$.ancestors[0].id", is(KEYWORD_ID_0)))
                .andExpect(jsonPath("$.ancestors[0].label", is(KEYWORD_NAME_0)))
                .andExpect(jsonPath("$.ancestors[1].id", is(KEYWORD_ID_1)))
                .andExpect(jsonPath("$.ancestors[1].label", is(KEYWORD_NAME_1)))
                .andExpect(jsonPath("$.ancestors.size()", is(2)));
    }

    @Test
    void getGroupByKeyword_whenParentSpecifiedAndTraversalAndFreeFormQuery() throws Exception {
        prepareSingleRootWithTwoLevelsOfChildrenAndSidePaths();

        mockMvc.perform(get(PATH).param("query", KEYWORD_ID_2).param("parent", KEYWORD_ID_0))
                .andDo(log())
                .andExpect(jsonPath("$.groups[0].id", is(KEYWORD_ID_2)))
                .andExpect(jsonPath("$.groups[0].label", is(KEYWORD_NAME_2)))
                .andExpect(jsonPath("$.groups[0].expandable", is(false)))
                .andExpect(jsonPath("$.groups[0].count", is(1)))
                .andExpect(jsonPath("$.groups.size()", is(1)))
                .andExpect(jsonPath("$.ancestors[0].id", is(KEYWORD_ID_1)))
                .andExpect(jsonPath("$.ancestors[0].label", is(KEYWORD_NAME_1)))
                .andExpect(jsonPath("$.ancestors.size()", is(1)));
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

    @Override
    protected DataStoreManager getDataStoreManager() {
        return dataStoreManager;
    }

    @Override
    protected MockMvc getMockMvc() {
        return mockMvc;
    }

    @Override
    protected String getPath() {
        return PATH;
    }

    @Override
    protected void prepareSingleRootNodeWithNoChildren() throws Exception {
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
}
