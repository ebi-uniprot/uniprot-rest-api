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
import org.uniprot.core.cv.ec.ECEntry;
import org.uniprot.cv.ec.ECRepo;
import org.uniprot.store.indexer.DataStoreManager;
import org.uniprot.store.indexer.DataStoreManager.StoreType;
import org.uniprot.store.search.SolrCollection;
import org.uniprot.store.search.document.Document;
import org.uniprot.store.search.document.uniprot.UniProtDocument;

import java.util.List;
import java.util.Optional;

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
@ActiveProfiles({"viewbyTest", "viewbyECTest"})
class UniProtKBViewByEcControllerIT {
    private static final String EMPTY_PARENT = "";
    private static final String ORGANISM_ID_0 = "29";
    private static final String ORGANISM_ID_1 = "517";
    private static final String ORGANISM_ID_2 = "34959";
    private static final String INVALID_ORGANISM_ID = "36";
    private static final String ACCESSION_0 = "A0";
    private static final String EC_ID_0 = "1.-.-.-";
    private static final String EC_LABEL_0 = "ec_label_0";
    private static final String ACCESSION_1 = "A1";
    private static final String EC_ID_1 = "1.1.-.-";
    private static final String EC_LABEL_1 = "ec_label_1";
    private static final String ACCESSION_2 = "A2";
    private static final String EC_ID_2 = "1.1.1.-";
    private static final String EC_LABEL_2 = "ec_label_2";
    public static final String PATH = "/uniprotkb/view/ec";
    @Autowired private MockMvc mockMvc;
    @RegisterExtension static DataStoreManager dataStoreManager = new DataStoreManager();
    @MockBean private ECRepo ecRepo;

    @TestConfiguration
    @Profile("viewbyECTest")
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
    void viewByEC_whenNoParentSpecifiedAndNoTraversalAndQuerySpecifiedWithField() throws Exception {
        prepareSingleRootNodeWithNoChildren();

        mockMvc.perform(
                        get(PATH)
                                .param("query", "organism_id:" + ORGANISM_ID_0)
                                .param("parent", EMPTY_PARENT))
                .andDo(log())
                .andExpect(jsonPath("$.results[0].id", is(EC_ID_0)))
                .andExpect(jsonPath("$.results[0].label", is(EC_LABEL_0)))
                .andExpect(jsonPath("$.results[0].expand", is(false)))
                .andExpect(jsonPath("$.results[0].count", is(1)))
                .andExpect(jsonPath("$.results.size()", is(1)))
                .andExpect(jsonPath("$.ancestors.size()", is(0)));
    }

    @Test
    void viewByEC_whenNoParentSpecifiedAndNoTraversalAndFreeFormQuery() throws Exception {
        prepareSingleRootNodeWithNoChildren();

        mockMvc.perform(get(PATH).param("query", ORGANISM_ID_0).param("parent", EMPTY_PARENT))
                .andDo(log())
                .andExpect(jsonPath("$.results[0].id", is(EC_ID_0)))
                .andExpect(jsonPath("$.results[0].label", is(EC_LABEL_0)))
                .andExpect(jsonPath("$.results[0].expand", is(false)))
                .andExpect(jsonPath("$.results[0].count", is(1)))
                .andExpect(jsonPath("$.results.size()", is(1)))
                .andExpect(jsonPath("$.ancestors.size()", is(0)));
    }

    @Test
    void viewByEC_whenNoParentSpecifiedAndTraversalAndQuerySpecifiedWithField() throws Exception {
        prepareSingleRootWithTwoLevelsOfChildren();

        mockMvc.perform(
                        get(PATH)
                                .param("query", "organism_id:" + ORGANISM_ID_2)
                                .param("parent", EMPTY_PARENT))
                .andDo(log())
                .andExpect(jsonPath("$.results[0].id", is(EC_ID_2)))
                .andExpect(jsonPath("$.results[0].label", is(EC_LABEL_2)))
                .andExpect(jsonPath("$.results[0].expand", is(false)))
                .andExpect(jsonPath("$.results[0].count", is(1)))
                .andExpect(jsonPath("$.results.size()", is(1)))
                .andExpect(jsonPath("$.ancestors[0].id", is(EC_ID_0)))
                .andExpect(jsonPath("$.ancestors[0].label", is(EC_LABEL_0)))
                .andExpect(jsonPath("$.ancestors[1].id", is(EC_ID_1)))
                .andExpect(jsonPath("$.ancestors[1].label", is(EC_LABEL_1)))
                .andExpect(jsonPath("$.ancestors.size()", is(2)));
    }

    @Test
    void viewByEC_whenNoParentSpecifiedAndTraversalAndFreeFormQuery() throws Exception {
        prepareSingleRootWithTwoLevelsOfChildren();

        mockMvc.perform(get(PATH).param("query", ORGANISM_ID_2).param("parent", EMPTY_PARENT))
                .andDo(log())
                .andExpect(jsonPath("$.results[0].id", is(EC_ID_2)))
                .andExpect(jsonPath("$.results[0].label", is(EC_LABEL_2)))
                .andExpect(jsonPath("$.results[0].expand", is(false)))
                .andExpect(jsonPath("$.results[0].count", is(1)))
                .andExpect(jsonPath("$.results.size()", is(1)))
                .andExpect(jsonPath("$.ancestors[0].id", is(EC_ID_0)))
                .andExpect(jsonPath("$.ancestors[0].label", is(EC_LABEL_0)))
                .andExpect(jsonPath("$.ancestors[1].id", is(EC_ID_1)))
                .andExpect(jsonPath("$.ancestors[1].label", is(EC_LABEL_1)))
                .andExpect(jsonPath("$.ancestors.size()", is(2)));
    }

    @Test
    void viewByEC_whenParentSpecifiedAndQuerySpecifiedWithField() throws Exception {
        prepareSingleRootWithTwoLevelsOfChildren();

        mockMvc.perform(
                        get(PATH)
                                .param("query", "organism_id:" + ORGANISM_ID_2)
                                .param("parent", EC_ID_0))
                .andDo(log())
                .andExpect(jsonPath("$.results[0].id", is(EC_ID_2)))
                .andExpect(jsonPath("$.results[0].label", is(EC_LABEL_2)))
                .andExpect(jsonPath("$.results[0].expand", is(false)))
                .andExpect(jsonPath("$.results[0].count", is(1)))
                .andExpect(jsonPath("$.results.size()", is(1)))
                .andExpect(jsonPath("$.ancestors[0].id", is(EC_ID_1)))
                .andExpect(jsonPath("$.ancestors[0].label", is(EC_LABEL_1)))
                .andExpect(jsonPath("$.ancestors.size()", is(1)));
    }

    @Test
    void viewByEC_whenParentNotSpecifiedAndTraversalAndFreeFormQuery() throws Exception {
        prepareSingleRootWithTwoLevelsOfChildren();

        mockMvc.perform(get(PATH).param("query", ORGANISM_ID_2).param("parent", EMPTY_PARENT))
                .andDo(log())
                .andExpect(jsonPath("$.results[0].id", is(EC_ID_2)))
                .andExpect(jsonPath("$.results[0].label", is(EC_LABEL_2)))
                .andExpect(jsonPath("$.results[0].expand", is(false)))
                .andExpect(jsonPath("$.results[0].count", is(1)))
                .andExpect(jsonPath("$.results.size()", is(1)))
                .andExpect(jsonPath("$.ancestors[0].id", is(EC_ID_0)))
                .andExpect(jsonPath("$.ancestors[0].label", is(EC_LABEL_0)))
                .andExpect(jsonPath("$.ancestors[1].id", is(EC_ID_1)))
                .andExpect(jsonPath("$.ancestors[1].label", is(EC_LABEL_1)))
                .andExpect(jsonPath("$.ancestors.size()", is(2)));
    }

    @Test
    void viewByEC_whenParentSpecifiedAndTraversalAndFreeFormQuery() throws Exception {
        prepareSingleRootWithTwoLevelsOfChildren();

        mockMvc.perform(get(PATH).param("query", ORGANISM_ID_2).param("parent", EC_ID_0))
                .andDo(log())
                .andExpect(jsonPath("$.results[0].id", is(EC_ID_2)))
                .andExpect(jsonPath("$.results[0].label", is(EC_LABEL_2)))
                .andExpect(jsonPath("$.results[0].expand", is(false)))
                .andExpect(jsonPath("$.results[0].count", is(1)))
                .andExpect(jsonPath("$.results.size()", is(1)))
                .andExpect(jsonPath("$.ancestors[0].id", is(EC_ID_1)))
                .andExpect(jsonPath("$.ancestors[0].label", is(EC_LABEL_1)))
                .andExpect(jsonPath("$.ancestors.size()", is(1)));
    }

    @Test
    void viewByEC_emptyResults() throws Exception {
        prepareSingleRootNodeWithNoChildren();

        mockMvc.perform(
                        get(PATH)
                                .param("query", "organism_id:" + INVALID_ORGANISM_ID)
                                .param("parent", EMPTY_PARENT))
                .andDo(log())
                .andExpect(jsonPath("$.results.size()", is(0)))
                .andExpect(jsonPath("$.ancestors.size()", is(0)));
    }

    @Test
    void viewByEC_whenFreeFormQueryAndEmptyResults() throws Exception {
        prepareSingleRootNodeWithNoChildren();

        mockMvc.perform(get(PATH).param("query", INVALID_ORGANISM_ID).param("parent", EMPTY_PARENT))
                .andDo(log())
                .andExpect(jsonPath("$.results.size()", is(0)))
                .andExpect(jsonPath("$.ancestors.size()", is(0)));
    }

    @Test
    void viewByEC_whenQueryNotSpecified() throws Exception {
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
        mockEcEntry(EC_ID_0, EC_LABEL_0);
        saveUniProtDocument(ACCESSION_0, ORGANISM_ID_0, List.of(EC_ID_0));
        mockEcEntry(EC_ID_1, EC_LABEL_1);
        saveUniProtDocument(ACCESSION_1, ORGANISM_ID_1, List.of(EC_ID_0, EC_ID_1));
        mockEcEntry(EC_ID_2, EC_LABEL_2);
        saveUniProtDocument(ACCESSION_2, ORGANISM_ID_2, List.of(EC_ID_0, EC_ID_1, EC_ID_2));
    }

    private void prepareSingleRootNodeWithNoChildren() {
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

    void save(Document doc) {
        dataStoreManager.saveDocs(StoreType.UNIPROT, doc);
    }
}
