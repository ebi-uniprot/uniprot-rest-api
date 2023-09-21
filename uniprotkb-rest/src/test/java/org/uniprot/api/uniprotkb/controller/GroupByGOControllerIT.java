package org.uniprot.api.uniprotkb.controller;

import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.log;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.uniprot.store.indexer.DataStoreManager.StoreType.UNIPROT;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.client.AutoConfigureWebClient;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.uniprot.api.uniprotkb.groupby.service.go.client.GOClient;
import org.uniprot.api.uniprotkb.groupby.service.go.client.GoRelation;
import org.uniprot.api.uniprotkb.repository.search.impl.UniprotQueryRepository;
import org.uniprot.store.indexer.DataStoreManager;
import org.uniprot.store.search.SolrCollection;
import org.uniprot.store.search.document.uniprot.UniProtDocument;

@ExtendWith(SpringExtension.class)
@WebMvcTest(controllers = GroupByController.class)
@AutoConfigureWebClient
@ActiveProfiles({"offline"})
class GroupByGOControllerIT extends GroupByControllerIT {
    private static final String ACCESSION_0 = "A0";
    private static final String ACCESSION_1 = "A1";
    private static final String ACCESSION_2 = "A2";
    private static final String ORGANISM_ID_0 = "29";
    private static final String ORGANISM_ID_1 = "324";
    private static final String ORGANISM_ID_2 = "994";
    private static final String GO_ID_0 = "GO:0000000";
    private static final String GO_NAME_0 = "goName0";
    private static final String GO_ID_1 = "GO:0000001";
    private static final String GO_NAME_1 = "goName1";
    private static final String GO_ID_2 = "GO:0000002";
    private static final String GO_NAME_2 = "goName2";
    public static final String PATH = "/uniprotkb/groups/go";
    @RegisterExtension static DataStoreManager dataStoreManager = new DataStoreManager();
    @MockBean private GOClient goClient;
    @Autowired private MockMvc mockMvc;
    @Autowired private UniprotQueryRepository repository;

    @BeforeAll
    static void beforeAll() {
        dataStoreManager.addSolrClient(UNIPROT, SolrCollection.uniprot);
    }

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(
                repository, "solrClient", dataStoreManager.getSolrClient(UNIPROT));
    }

    @AfterEach
    void tearDown() {
        dataStoreManager.cleanSolr(UNIPROT);
    }

    @Test
    void getGroupByGO_whenNoParentSpecifiedAndNoTraversalAndQuerySpecifiedWithField()
            throws Exception {
        prepareSingleRootNodeWithNoChildren();

        mockMvc.perform(get(PATH).param("query", "organism_id:" + ORGANISM_ID_0))
                .andDo(log())
                .andExpect(jsonPath("$.groups[0].id", is(GO_ID_0)))
                .andExpect(jsonPath("$.groups[0].label", is(GO_NAME_0)))
                .andExpect(jsonPath("$.groups[0].expandable", is(false)))
                .andExpect(jsonPath("$.groups[0].count", is(1)))
                .andExpect(jsonPath("$.groups.size()", is(1)))
                .andExpect(jsonPath("$.ancestors.size()", is(0)))
                .andExpect(jsonPath("$.parent.label").doesNotExist())
                .andExpect(jsonPath("$.parent.count", is(1)));
    }

    @Test
    void getGroupByGO_whenNoParentSpecifiedAndNoTraversalAndFreeFormQuery() throws Exception {
        prepareSingleRootNodeWithNoChildren();

        mockMvc.perform(get(PATH).param("query", ORGANISM_ID_0))
                .andDo(log())
                .andExpect(jsonPath("$.groups[0].id", is(GO_ID_0)))
                .andExpect(jsonPath("$.groups[0].label", is(GO_NAME_0)))
                .andExpect(jsonPath("$.groups[0].expandable", is(false)))
                .andExpect(jsonPath("$.groups[0].count", is(1)))
                .andExpect(jsonPath("$.groups.size()", is(1)))
                .andExpect(jsonPath("$.ancestors.size()", is(0)))
                .andExpect(jsonPath("$.parent.label").doesNotExist())
                .andExpect(jsonPath("$.parent.count", is(1)));
    }

    @Test
    void getGroupByGO_whenNoParentSpecifiedAndTraversalAndQuerySpecifiedWithField()
            throws Exception {
        prepareSingleRootWithTwoLevelsOfChildren();

        mockMvc.perform(get(PATH).param("query", "organism_id:" + ORGANISM_ID_2))
                .andDo(log())
                .andExpect(jsonPath("$.groups[0].id", is(GO_ID_2)))
                .andExpect(jsonPath("$.groups[0].label", is(GO_NAME_2)))
                .andExpect(jsonPath("$.groups[0].expandable", is(false)))
                .andExpect(jsonPath("$.groups[0].count", is(1)))
                .andExpect(jsonPath("$.groups.size()", is(1)))
                .andExpect(jsonPath("$.ancestors[0].id", is(GO_ID_0)))
                .andExpect(jsonPath("$.ancestors[0].label", is(GO_NAME_0)))
                .andExpect(jsonPath("$.ancestors[1].id", is(GO_ID_1)))
                .andExpect(jsonPath("$.ancestors[1].label", is(GO_NAME_1)))
                .andExpect(jsonPath("$.ancestors.size()", is(2)))
                .andExpect(jsonPath("$.parent.label").doesNotExist())
                .andExpect(jsonPath("$.parent.count", is(1)));
    }

    @Test
    void getGroupByGO_whenNoParentSpecifiedAndTraversalAndFreeFormQuery() throws Exception {
        prepareSingleRootWithTwoLevelsOfChildren();

        mockMvc.perform(get(PATH).param("query", ORGANISM_ID_2))
                .andDo(log())
                .andExpect(jsonPath("$.groups[0].id", is(GO_ID_2)))
                .andExpect(jsonPath("$.groups[0].label", is(GO_NAME_2)))
                .andExpect(jsonPath("$.groups[0].expandable", is(false)))
                .andExpect(jsonPath("$.groups[0].count", is(1)))
                .andExpect(jsonPath("$.groups.size()", is(1)))
                .andExpect(jsonPath("$.ancestors[0].id", is(GO_ID_0)))
                .andExpect(jsonPath("$.ancestors[0].label", is(GO_NAME_0)))
                .andExpect(jsonPath("$.ancestors[1].id", is(GO_ID_1)))
                .andExpect(jsonPath("$.ancestors[1].label", is(GO_NAME_1)))
                .andExpect(jsonPath("$.ancestors.size()", is(2)))
                .andExpect(jsonPath("$.parent.label").doesNotExist())
                .andExpect(jsonPath("$.parent.count", is(1)));
    }

    @Test
    void getGroupByGO_whenParentSpecifiedAndQuerySpecifiedWithField() throws Exception {
        prepareSingleRootWithTwoLevelsOfChildren();
        GoRelation goRelation = new GoRelation();
        goRelation.setId(GO_ID_0);
        goRelation.setName(GO_NAME_0);
        when(goClient.getGoEntry(GO_ID_0)).thenReturn(Optional.of(goRelation));

        mockMvc.perform(
                        get(PATH)
                                .param("query", "organism_id:" + ORGANISM_ID_2)
                                .param("parent", GO_ID_0))
                .andDo(log())
                .andExpect(jsonPath("$.groups[0].id", is(GO_ID_2)))
                .andExpect(jsonPath("$.groups[0].label", is(GO_NAME_2)))
                .andExpect(jsonPath("$.groups[0].expandable", is(false)))
                .andExpect(jsonPath("$.groups[0].count", is(1)))
                .andExpect(jsonPath("$.groups.size()", is(1)))
                .andExpect(jsonPath("$.ancestors[0].id", is(GO_ID_1)))
                .andExpect(jsonPath("$.ancestors[0].label", is(GO_NAME_1)))
                .andExpect(jsonPath("$.ancestors.size()", is(1)))
                .andExpect(jsonPath("$.parent.label", is("goName0")))
                .andExpect(jsonPath("$.parent.count", is(1)));
    }

    @Test
    void getGroupByGO_whenParentNotSpecifiedAndTraversalAndFreeFormQuery() throws Exception {
        prepareSingleRootWithTwoLevelsOfChildren();

        mockMvc.perform(get(PATH).param("query", ORGANISM_ID_2))
                .andDo(log())
                .andExpect(jsonPath("$.groups[0].id", is(GO_ID_2)))
                .andExpect(jsonPath("$.groups[0].label", is(GO_NAME_2)))
                .andExpect(jsonPath("$.groups[0].expandable", is(false)))
                .andExpect(jsonPath("$.groups[0].count", is(1)))
                .andExpect(jsonPath("$.groups.size()", is(1)))
                .andExpect(jsonPath("$.ancestors[0].id", is(GO_ID_0)))
                .andExpect(jsonPath("$.ancestors[0].label", is(GO_NAME_0)))
                .andExpect(jsonPath("$.ancestors[1].id", is(GO_ID_1)))
                .andExpect(jsonPath("$.ancestors[1].label", is(GO_NAME_1)))
                .andExpect(jsonPath("$.ancestors.size()", is(2)))
                .andExpect(jsonPath("$.parent.label").doesNotExist())
                .andExpect(jsonPath("$.parent.count", is(1)));
    }

    @Test
    void getGroupByGO_whenParentSpecifiedAndTraversalAndFreeFormQuery() throws Exception {
        prepareSingleRootWithTwoLevelsOfChildren();
        GoRelation goRelation = new GoRelation();
        goRelation.setId(GO_ID_0);
        goRelation.setName(GO_NAME_0);
        when(goClient.getGoEntry(GO_ID_0)).thenReturn(Optional.of(goRelation));

        mockMvc.perform(get(PATH).param("query", ORGANISM_ID_2).param("parent", GO_ID_0))
                .andDo(log())
                .andExpect(jsonPath("$.groups[0].id", is(GO_ID_2)))
                .andExpect(jsonPath("$.groups[0].label", is(GO_NAME_2)))
                .andExpect(jsonPath("$.groups[0].expandable", is(false)))
                .andExpect(jsonPath("$.groups[0].count", is(1)))
                .andExpect(jsonPath("$.groups.size()", is(1)))
                .andExpect(jsonPath("$.ancestors[0].id", is(GO_ID_1)))
                .andExpect(jsonPath("$.ancestors[0].label", is(GO_NAME_1)))
                .andExpect(jsonPath("$.ancestors.size()", is(1)))
                .andExpect(jsonPath("$.parent.label", is("goName0")))
                .andExpect(jsonPath("$.parent.count", is(1)));
    }

    private void prepareSingleRootWithTwoLevelsOfChildren() {
        mockGoRelation(GO_ID_0, GO_NAME_0, null);
        saveUniProtDocument(ACCESSION_0, ORGANISM_ID_0, Set.of(removeGoPrefix(GO_ID_0)));
        mockGoRelation(GO_ID_1, GO_NAME_1, GO_ID_0);
        saveUniProtDocument(
                ACCESSION_1,
                ORGANISM_ID_1,
                Set.of(removeGoPrefix(GO_ID_0), removeGoPrefix(GO_ID_1)));
        mockGoRelation(GO_ID_2, GO_NAME_2, GO_ID_1);
        saveUniProtDocument(
                ACCESSION_2,
                ORGANISM_ID_2,
                Set.of(removeGoPrefix(GO_ID_0), removeGoPrefix(GO_ID_1), removeGoPrefix(GO_ID_2)));
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
    protected void prepareSingleRootNodeWithNoChildren() {
        mockGoRelation(GO_ID_0, GO_NAME_0, null);
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

    private GoRelation getGoRelation(String id, String name) {
        GoRelation goRelation = new GoRelation();
        goRelation.setId(id);
        goRelation.setName(name);
        goRelation.setRelation("is_a");
        goRelation.setHasChildren(true);
        return goRelation;
    }
}
