package org.uniprot.api.uniprotkb.controller;

import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.log;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.uniprot.store.indexer.DataStoreManager.StoreType.UNIPROT;

import java.util.List;
import java.util.Optional;

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
import org.uniprot.api.uniprotkb.repository.search.impl.UniprotQueryRepository;
import org.uniprot.core.cv.ec.ECEntry;
import org.uniprot.cv.ec.ECRepo;
import org.uniprot.store.indexer.DataStoreManager;
import org.uniprot.store.search.SolrCollection;
import org.uniprot.store.search.document.uniprot.UniProtDocument;

@ExtendWith(SpringExtension.class)
@WebMvcTest(controllers = GroupByController.class)
@AutoConfigureWebClient
@ActiveProfiles({"offline"})
class GroupByECControllerIT extends GroupByControllerIT {
    private static final String ORGANISM_ID_0 = "29";
    private static final String ORGANISM_ID_1 = "517";
    private static final String ORGANISM_ID_2 = "34959";
    private static final String ACCESSION_0 = "A0";
    private static final String EC_ID_0 = "1.-.-.-";
    private static final String EC_LABEL_0 = "ec_label_0";
    private static final String ACCESSION_1 = "A1";
    private static final String EC_ID_1 = "1.1.-.-";
    private static final String EC_LABEL_1 = "ec_label_1";
    private static final String ACCESSION_2 = "A2";
    private static final String EC_ID_2 = "1.1.1.-";
    private static final String EC_LABEL_2 = "ec_label_2";
    public static final String PATH = "/uniprotkb/groups/ec";
    @RegisterExtension static DataStoreManager dataStoreManager = new DataStoreManager();
    @MockBean private ECRepo ecRepo;
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
    void getGroupByEC_whenNoParentSpecifiedAndNoTraversalAndQuerySpecifiedWithField()
            throws Exception {
        prepareSingleRootNodeWithNoChildren();

        mockMvc.perform(
                        get(PATH)
                                .param("query", "organism_id:" + ORGANISM_ID_0)
                                .param("parent", EMPTY_PARENT))
                .andDo(log())
                .andExpect(jsonPath("$.groups[0].id", is(EC_ID_0)))
                .andExpect(jsonPath("$.groups[0].label", is(EC_LABEL_0)))
                .andExpect(jsonPath("$.groups[0].expandable", is(false)))
                .andExpect(jsonPath("$.groups[0].count", is(1)))
                .andExpect(jsonPath("$.groups.size()", is(1)))
                .andExpect(jsonPath("$.ancestors.size()", is(0)))
                .andExpect(jsonPath("$.parent.label").doesNotExist())
                .andExpect(jsonPath("$.parent.count", is(1)));
    }

    @Test
    void getGroupByEC_whenNoParentSpecifiedAndNoTraversalAndFreeFormQuery() throws Exception {
        prepareSingleRootNodeWithNoChildren();

        mockMvc.perform(get(PATH).param("query", ORGANISM_ID_0).param("parent", EMPTY_PARENT))
                .andDo(log())
                .andExpect(jsonPath("$.groups[0].id", is(EC_ID_0)))
                .andExpect(jsonPath("$.groups[0].label", is(EC_LABEL_0)))
                .andExpect(jsonPath("$.groups[0].expandable", is(false)))
                .andExpect(jsonPath("$.groups[0].count", is(1)))
                .andExpect(jsonPath("$.groups.size()", is(1)))
                .andExpect(jsonPath("$.ancestors.size()", is(0)))
                .andExpect(jsonPath("$.parent.label").doesNotExist())
                .andExpect(jsonPath("$.parent.count", is(1)));
    }

    @Test
    void getGroupByEC_whenNoParentSpecifiedAndTraversalAndQuerySpecifiedWithField()
            throws Exception {
        prepareSingleRootWithTwoLevelsOfChildren();

        mockMvc.perform(
                        get(PATH)
                                .param("query", "organism_id:" + ORGANISM_ID_2)
                                .param("parent", EMPTY_PARENT))
                .andDo(log())
                .andExpect(jsonPath("$.groups[0].id", is(EC_ID_2)))
                .andExpect(jsonPath("$.groups[0].label", is(EC_LABEL_2)))
                .andExpect(jsonPath("$.groups[0].expandable", is(false)))
                .andExpect(jsonPath("$.groups[0].count", is(1)))
                .andExpect(jsonPath("$.groups.size()", is(1)))
                .andExpect(jsonPath("$.ancestors[0].id", is(EC_ID_0)))
                .andExpect(jsonPath("$.ancestors[0].label", is(EC_LABEL_0)))
                .andExpect(jsonPath("$.ancestors[1].id", is(EC_ID_1)))
                .andExpect(jsonPath("$.ancestors[1].label", is(EC_LABEL_1)))
                .andExpect(jsonPath("$.ancestors.size()", is(2)))
                .andExpect(jsonPath("$.parent.label").doesNotExist())
                .andExpect(jsonPath("$.parent.count", is(1)));
    }

    @Test
    void getGroupByEC_whenNoParentSpecifiedAndTraversalAndFreeFormQuery() throws Exception {
        prepareSingleRootWithTwoLevelsOfChildren();

        mockMvc.perform(get(PATH).param("query", ORGANISM_ID_2).param("parent", EMPTY_PARENT))
                .andDo(print())
                .andExpect(jsonPath("$.groups[0].id", is(EC_ID_2)))
                .andExpect(jsonPath("$.groups[0].label", is(EC_LABEL_2)))
                .andExpect(jsonPath("$.groups[0].expandable", is(false)))
                .andExpect(jsonPath("$.groups[0].count", is(1)))
                .andExpect(jsonPath("$.groups.size()", is(1)))
                .andExpect(jsonPath("$.ancestors[0].id", is(EC_ID_0)))
                .andExpect(jsonPath("$.ancestors[0].label", is(EC_LABEL_0)))
                .andExpect(jsonPath("$.ancestors[1].id", is(EC_ID_1)))
                .andExpect(jsonPath("$.ancestors[1].label", is(EC_LABEL_1)))
                .andExpect(jsonPath("$.ancestors.size()", is(2)))
                .andExpect(jsonPath("$.parent.label").doesNotExist())
                .andExpect(jsonPath("$.parent.count", is(1)));
    }

    @Test
    void getGroupByEC_whenParentSpecifiedAndQuerySpecifiedWithField() throws Exception {
        prepareSingleRootWithTwoLevelsOfChildren();

        mockMvc.perform(
                        get(PATH)
                                .param("query", "organism_id:" + ORGANISM_ID_2)
                                .param("parent", EC_ID_0))
                .andDo(log())
                .andExpect(jsonPath("$.groups[0].id", is(EC_ID_2)))
                .andExpect(jsonPath("$.groups[0].label", is(EC_LABEL_2)))
                .andExpect(jsonPath("$.groups[0].expandable", is(false)))
                .andExpect(jsonPath("$.groups[0].count", is(1)))
                .andExpect(jsonPath("$.groups.size()", is(1)))
                .andExpect(jsonPath("$.ancestors[0].id", is(EC_ID_1)))
                .andExpect(jsonPath("$.ancestors[0].label", is(EC_LABEL_1)))
                .andExpect(jsonPath("$.ancestors.size()", is(1)))
                .andExpect(jsonPath("$.parent.label", is("ec_label_0")))
                .andExpect(jsonPath("$.parent.count", is(1)));
    }

    @Test
    void getGroupByEC_whenParentNotSpecifiedAndTraversalAndFreeFormQuery() throws Exception {
        prepareSingleRootWithTwoLevelsOfChildren();

        mockMvc.perform(get(PATH).param("query", ORGANISM_ID_2).param("parent", EMPTY_PARENT))
                .andDo(log())
                .andExpect(jsonPath("$.groups[0].id", is(EC_ID_2)))
                .andExpect(jsonPath("$.groups[0].label", is(EC_LABEL_2)))
                .andExpect(jsonPath("$.groups[0].expandable", is(false)))
                .andExpect(jsonPath("$.groups[0].count", is(1)))
                .andExpect(jsonPath("$.groups.size()", is(1)))
                .andExpect(jsonPath("$.ancestors[0].id", is(EC_ID_0)))
                .andExpect(jsonPath("$.ancestors[0].label", is(EC_LABEL_0)))
                .andExpect(jsonPath("$.ancestors[1].id", is(EC_ID_1)))
                .andExpect(jsonPath("$.ancestors[1].label", is(EC_LABEL_1)))
                .andExpect(jsonPath("$.ancestors.size()", is(2)))
                .andExpect(jsonPath("$.parent.label").doesNotExist())
                .andExpect(jsonPath("$.parent.count", is(1)));
    }

    @Test
    void getGroupByEC_whenParentSpecifiedAndTraversalAndFreeFormQuery() throws Exception {
        prepareSingleRootWithTwoLevelsOfChildren();

        mockMvc.perform(get(PATH).param("query", ORGANISM_ID_2).param("parent", EC_ID_0))
                .andDo(log())
                .andExpect(jsonPath("$.groups[0].id", is(EC_ID_2)))
                .andExpect(jsonPath("$.groups[0].label", is(EC_LABEL_2)))
                .andExpect(jsonPath("$.groups[0].expandable", is(false)))
                .andExpect(jsonPath("$.groups[0].count", is(1)))
                .andExpect(jsonPath("$.groups.size()", is(1)))
                .andExpect(jsonPath("$.ancestors[0].id", is(EC_ID_1)))
                .andExpect(jsonPath("$.ancestors[0].label", is(EC_LABEL_1)))
                .andExpect(jsonPath("$.ancestors.size()", is(1)))
                .andExpect(jsonPath("$.parent.label", is("ec_label_0")))
                .andExpect(jsonPath("$.parent.count", is(1)));
    }

    private void prepareSingleRootWithTwoLevelsOfChildren() {
        mockEcEntry(EC_ID_0, EC_LABEL_0);
        saveUniProtDocument(ACCESSION_0, ORGANISM_ID_0, List.of(EC_ID_0));
        mockEcEntry(EC_ID_1, EC_LABEL_1);
        saveUniProtDocument(ACCESSION_1, ORGANISM_ID_1, List.of(EC_ID_0, EC_ID_1));
        mockEcEntry(EC_ID_2, EC_LABEL_2);
        saveUniProtDocument(ACCESSION_2, ORGANISM_ID_2, List.of(EC_ID_0, EC_ID_1, EC_ID_2));
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
        mockEcEntry(EC_ID_0, EC_LABEL_0);
        saveUniProtDocument(ACCESSION_0, ORGANISM_ID_0, List.of(EC_ID_0));
    }

    private void saveUniProtDocument(String accession, String organismId, List<String> ecs) {
        UniProtDocument uniProtDocument = new UniProtDocument();
        uniProtDocument.active = true;
        uniProtDocument.accession = accession;
        uniProtDocument.ecNumbers = ecs;
        uniProtDocument.organismTaxId = Integer.parseInt(organismId);
        uniProtDocument.taxLineageIds = List.of(Integer.parseInt(organismId));
        save(uniProtDocument);
    }

    private void mockEcEntry(String ecId, String label) {
        when(ecRepo.getEC(ecId))
                .thenReturn(
                        Optional.of(
                                new ECEntry() {
                                    @Override
                                    public String getId() {
                                        return ecId;
                                    }

                                    @Override
                                    public String getLabel() {
                                        return label;
                                    }
                                }));
    }
}
