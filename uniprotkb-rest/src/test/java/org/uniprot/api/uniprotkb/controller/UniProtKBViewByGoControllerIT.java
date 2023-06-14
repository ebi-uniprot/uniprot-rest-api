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
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.uniprot.api.rest.download.AsyncDownloadMocks;
import org.uniprot.api.uniprotkb.repository.DataStoreTestConfig;
import org.uniprot.api.uniprotkb.view.GoRelation;
import org.uniprot.api.uniprotkb.view.service.GoClient;
import org.uniprot.store.indexer.DataStoreManager;
import org.uniprot.store.indexer.DataStoreManager.StoreType;
import org.uniprot.store.search.SolrCollection;
import org.uniprot.store.search.document.Document;
import org.uniprot.store.search.document.uniprot.UniProtDocument;

import java.util.List;
import java.util.Set;

import static org.hamcrest.Matchers.containsStringIgnoringCase;
import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.log;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.uniprot.store.indexer.DataStoreManager.StoreType.UNIPROT;

@ExtendWith(SpringExtension.class)
@WebMvcTest(controllers = UniProtKBViewByController.class)
@ContextConfiguration(classes = {DataStoreTestConfig.class, AsyncDownloadMocks.class})
@AutoConfigureWebClient
@ActiveProfiles({"viewbyTest", "viewbyGoTest"})
class UniProtKBViewByGoControllerIT {
    private static final String EMPTY_PARENT = "";
    private static final String ACCESSION_0 = "A0";
    private static final String ACCESSION_1 = "A1";
    private static final String ACCESSION_2 = "A2";
    private static final String ORGANISM_ID_0 = "29";
    private static final String ORGANISM_ID_1 = "324";
    private static final String ORGANISM_ID_2 = "994";
    private static final String GO_ID_0 = "GO:goId0";
    private static final String GO_NAME_0 = "goName0";
    private static final String GO_ID_1 = "GO:goId1";
    private static final String GO_NAME_1 = "goName1";
    private static final String GO_ID_2 = "GO:goId2";
    private static final String GO_NAME_2 = "goName2";
    public static final String PATH = "/uniprotkb/view/go";
    @Autowired
    private MockMvc mockMvc;
    @RegisterExtension
    static DataStoreManager dataStoreManager = new DataStoreManager();
    @MockBean
    private GoClient goClient;

    @TestConfiguration
    @Profile("viewbyGoTest")
    static class TestConfig {
        @Bean("uniProtKBSolrClient")
        public SolrClient uniProtKBSolrClient() {
            return dataStoreManager.getSolrClient(UNIPROT);
        }
    }

    @BeforeAll
    static void beforeAll() {
        dataStoreManager.addSolrClient(UNIPROT, SolrCollection.uniprot);
    }

    @AfterEach
    void tearDown() {
        dataStoreManager.cleanSolr(UNIPROT);
    }

    @Test
    void viewByGO_whenNoParentSpecifiedAndNoTraversalAndQuerySpecifiedWithField() throws Exception {
        prepareSingleRootNodeWithNoChildren();

        mockMvc.perform(
                        get(PATH)
                                .param("query", "organism_id:" + ORGANISM_ID_0)
                                .param("parent", EMPTY_PARENT))
                .andDo(log())
                .andExpect(jsonPath("$.results[0].id", is(GO_ID_0)))
                .andExpect(jsonPath("$.results[0].label", is(GO_NAME_0)))
                .andExpect(jsonPath("$.results[0].expand", is(false)))
                .andExpect(jsonPath("$.results[0].count", is(1)))
                .andExpect(jsonPath("$.results.size()", is(1)))
                .andExpect(jsonPath("$.ancestors.size()", is(0)));
    }

    @Test
    void viewByGO_whenNoParentSpecifiedAndNoTraversalAndFreeFormQuery() throws Exception {
        prepareSingleRootNodeWithNoChildren();

        mockMvc.perform(get(PATH).param("query", ORGANISM_ID_0).param("parent", EMPTY_PARENT))
                .andDo(log())
                .andExpect(jsonPath("$.results[0].id", is(GO_ID_0)))
                .andExpect(jsonPath("$.results[0].label", is(GO_NAME_0)))
                .andExpect(jsonPath("$.results[0].expand", is(false)))
                .andExpect(jsonPath("$.results[0].count", is(1)))
                .andExpect(jsonPath("$.results.size()", is(1)))
                .andExpect(jsonPath("$.ancestors.size()", is(0)));
    }

    @Test
    void viewByGO_whenNoParentSpecifiedAndTraversalAndQuerySpecifiedWithField() throws Exception {
        prepareSingleRootWithTwoLevelsOfChildren();

        mockMvc.perform(
                        get(PATH)
                                .param("query", "organism_id:" + ORGANISM_ID_2)
                                .param("parent", EMPTY_PARENT))
                .andDo(log())
                .andExpect(jsonPath("$.results[0].id", is(GO_ID_2)))
                .andExpect(jsonPath("$.results[0].label", is(GO_NAME_2)))
                .andExpect(jsonPath("$.results[0].expand", is(false)))
                .andExpect(jsonPath("$.results[0].count", is(1)))
                .andExpect(jsonPath("$.results.size()", is(1)))
                .andExpect(jsonPath("$.ancestors[0].id", is(GO_ID_0)))
                .andExpect(jsonPath("$.ancestors[0].label", is(GO_NAME_0)))
                .andExpect(jsonPath("$.ancestors[1].id", is(GO_ID_1)))
                .andExpect(jsonPath("$.ancestors[1].label", is(GO_NAME_1)))
                .andExpect(jsonPath("$.ancestors.size()", is(2)));
    }

    @Test
    void viewByGO_whenNoParentSpecifiedAndTraversalAndFreeFormQuery() throws Exception {
        prepareSingleRootWithTwoLevelsOfChildren();

        mockMvc.perform(get(PATH).param("query", ORGANISM_ID_2).param("parent", EMPTY_PARENT))
                .andDo(log())
                .andExpect(jsonPath("$.results[0].id", is(GO_ID_2)))
                .andExpect(jsonPath("$.results[0].label", is(GO_NAME_2)))
                .andExpect(jsonPath("$.results[0].expand", is(false)))
                .andExpect(jsonPath("$.results[0].count", is(1)))
                .andExpect(jsonPath("$.results.size()", is(1)))
                .andExpect(jsonPath("$.ancestors[0].id", is(GO_ID_0)))
                .andExpect(jsonPath("$.ancestors[0].label", is(GO_NAME_0)))
                .andExpect(jsonPath("$.ancestors[1].id", is(GO_ID_1)))
                .andExpect(jsonPath("$.ancestors[1].label", is(GO_NAME_1)))
                .andExpect(jsonPath("$.ancestors.size()", is(2)));
    }

    @Test
    void viewByGO_whenParentSpecifiedAndQuerySpecifiedWithField() throws Exception {
        prepareSingleRootWithTwoLevelsOfChildren();

        mockMvc.perform(
                        get(PATH)
                                .param("query", "organism_id:" + ORGANISM_ID_2)
                                .param("parent", GO_ID_0))
                .andDo(log())
                .andExpect(jsonPath("$.results[0].id", is(GO_ID_2)))
                .andExpect(jsonPath("$.results[0].label", is(GO_NAME_2)))
                .andExpect(jsonPath("$.results[0].expand", is(false)))
                .andExpect(jsonPath("$.results[0].count", is(1)))
                .andExpect(jsonPath("$.results.size()", is(1)))
                .andExpect(jsonPath("$.ancestors[0].id", is(GO_ID_1)))
                .andExpect(jsonPath("$.ancestors[0].label", is(GO_NAME_1)))
                .andExpect(jsonPath("$.ancestors.size()", is(1)));
    }

    @Test
    void viewByGO_whenParentNotSpecifiedAndTraversalAndFreeFormQuery() throws Exception {
        prepareSingleRootWithTwoLevelsOfChildren();

        mockMvc.perform(get(PATH).param("query", ORGANISM_ID_2).param("parent", EMPTY_PARENT))
                .andDo(log())
                .andExpect(jsonPath("$.results[0].id", is(GO_ID_2)))
                .andExpect(jsonPath("$.results[0].label", is(GO_NAME_2)))
                .andExpect(jsonPath("$.results[0].expand", is(false)))
                .andExpect(jsonPath("$.results[0].count", is(1)))
                .andExpect(jsonPath("$.results.size()", is(1)))
                .andExpect(jsonPath("$.ancestors[0].id", is(GO_ID_0)))
                .andExpect(jsonPath("$.ancestors[0].label", is(GO_NAME_0)))
                .andExpect(jsonPath("$.ancestors[1].id", is(GO_ID_1)))
                .andExpect(jsonPath("$.ancestors[1].label", is(GO_NAME_1)))
                .andExpect(jsonPath("$.ancestors.size()", is(2)));
    }

    @Test
    void viewByGO_whenParentSpecifiedAndTraversalAndFreeFormQuery() throws Exception {
        prepareSingleRootWithTwoLevelsOfChildren();

        mockMvc.perform(get(PATH).param("query", ORGANISM_ID_2).param("parent", GO_ID_0))
                .andDo(log())
                .andExpect(jsonPath("$.results[0].id", is(GO_ID_2)))
                .andExpect(jsonPath("$.results[0].label", is(GO_NAME_2)))
                .andExpect(jsonPath("$.results[0].expand", is(false)))
                .andExpect(jsonPath("$.results[0].count", is(1)))
                .andExpect(jsonPath("$.results.size()", is(1)))
                .andExpect(jsonPath("$.ancestors[0].id", is(GO_ID_1)))
                .andExpect(jsonPath("$.ancestors[0].label", is(GO_NAME_1)))
                .andExpect(jsonPath("$.ancestors.size()", is(1)));
    }

    @Test
    void viewByGO_emptyResults() throws Exception {
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
    void viewByGO_whenFreeFormQueryAndEmptyResults() throws Exception {
        prepareSingleRootNodeWithNoChildren();

        mockMvc.perform(get(PATH).param("query", ORGANISM_ID_1).param("parent", EMPTY_PARENT))
                .andDo(log())
                .andExpect(jsonPath("$.results.size()", is(0)))
                .andExpect(jsonPath("$.ancestors.size()", is(0)));
    }

    @Test
    void viewByGO_whenQueryNotSpecified() throws Exception {
        mockMvc.perform(get(PATH).param("parent", EMPTY_PARENT))
                .andDo(log())
                .andExpect(status().isBadRequest())
                .andExpect(
                        content()
                                .string(
                                        containsStringIgnoringCase(
                                                "query is a required parameter")));
    }

    private void prepareSingleRootWithTwoLevelsOfChildren() {
        mockGoRelation(GO_ID_0, GO_NAME_0, EMPTY_PARENT);
        saveUniProtDocument(ACCESSION_0, ORGANISM_ID_0, Set.of(removeGoPrefix(GO_ID_0)));
        mockGoRelation(GO_ID_1, GO_NAME_1, GO_ID_0);
        saveUniProtDocument(ACCESSION_1, ORGANISM_ID_1, Set.of(removeGoPrefix(GO_ID_0), removeGoPrefix(GO_ID_1)));
        mockGoRelation(GO_ID_2, GO_NAME_2, GO_ID_1);
        saveUniProtDocument(ACCESSION_2, ORGANISM_ID_2, Set.of(removeGoPrefix(GO_ID_0), removeGoPrefix(GO_ID_1), removeGoPrefix(GO_ID_2)));
    }

    private void prepareSingleRootNodeWithNoChildren() {
        mockGoRelation(GO_ID_0, GO_NAME_0, EMPTY_PARENT);
        saveUniProtDocument(ACCESSION_0, ORGANISM_ID_0, Set.of(removeGoPrefix(GO_ID_0)));
    }

    private static String removeGoPrefix(String id) {
        return id.split(":")[1];
    }

    private void mockGoRelation(String goId0, String goName0, String parent) {
        when(goClient.getChildren(parent)).thenReturn(List.of(getGoRelation(goId0, goName0)));
    }

    private void saveUniProtDocument(String accession, String organismId, Set<String> gos) {
        UniProtDocument uniProtDocument = new UniProtDocument();
        uniProtDocument.active = true;
        uniProtDocument.accession = accession;
        uniProtDocument.goIds = gos;
        uniProtDocument.organismTaxId = Integer.parseInt(organismId);
        uniProtDocument.taxLineageIds = List.of(Integer.parseInt(organismId));
        save(uniProtDocument);
    }

    void save(Document doc) {
        dataStoreManager.saveDocs(StoreType.UNIPROT, doc);
    }

    private GoRelation getGoRelation(String id, String name) {
        GoRelation goRelation = new GoRelation();
        goRelation.setId(id);
        goRelation.setName(name);
        goRelation.setRelation("is_a");
        goRelation.setHasChildren(true);
        return goRelation;
    }
}
