package org.uniprot.api.uniprotkb.controller;

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
import org.uniprot.api.rest.respository.taxonomy.TaxonomyRepository;
import org.uniprot.api.uniprotkb.repository.search.impl.UniprotQueryRepository;
import org.uniprot.core.json.parser.taxonomy.TaxonomyJsonConfig;
import org.uniprot.core.taxonomy.impl.TaxonomyEntryBuilder;
import org.uniprot.store.indexer.DataStoreManager;
import org.uniprot.store.indexer.DataStoreManager.StoreType;
import org.uniprot.store.search.SolrCollection;
import org.uniprot.store.search.document.Document;
import org.uniprot.store.search.document.taxonomy.TaxonomyDocument;
import org.uniprot.store.search.document.uniprot.UniProtDocument;

import java.util.List;

import static org.hamcrest.Matchers.containsStringIgnoringCase;
import static org.hamcrest.core.Is.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.log;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.uniprot.store.indexer.DataStoreManager.StoreType.*;

@ExtendWith(SpringExtension.class)
@WebMvcTest(controllers = UniProtKBGroupByController.class)
@AutoConfigureWebClient
@ActiveProfiles({"offline"})
class UniProtKBGroupByTaxonomyControllerIT {
    private static final String EMPTY_PARENT = "";
    private static final String ACCESSION_0 = "A0";
    private static final int TAX_ID_0 = 9600;
    private static final String TAX_ID_0_STRING = String.valueOf(TAX_ID_0);
    private static final String TAX_SCIENTIFIC_0 = "scientific_9600";
    private static final String ACCESSION_1 = "A1";
    private static final int TAX_ID_1 = 9601;
    private static final String TAX_ID_1_STRING = String.valueOf(TAX_ID_1);
    private static final String TAX_SCIENTIFIC_1 = "scientific_9601";
    private static final String ACCESSION_2 = "A2";
    private static final int TAX_ID_2 = 9602;
    private static final String TAX_ID_2_STRING = String.valueOf(TAX_ID_2);
    private static final String TAX_SCIENTIFIC_2 = "scientific_9602";
    public static final String PATH = "/uniprotkb/groups/taxonomy";
    @RegisterExtension static DataStoreManager dataStoreManager = new DataStoreManager();
    @Autowired private MockMvc mockMvc;
    @Autowired private UniprotQueryRepository uniprotQueryRepository;
    @Autowired private TaxonomyRepository taxonomyRepository;

    @BeforeAll
    static void beforeAll() {
        dataStoreManager.addSolrClient(UNIPROT, SolrCollection.uniprot);
        dataStoreManager.addSolrClient(TAXONOMY, SolrCollection.taxonomy);
    }

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(uniprotQueryRepository, "solrClient", dataStoreManager.getSolrClient(UNIPROT));
        ReflectionTestUtils.setField(taxonomyRepository, "solrClient", dataStoreManager.getSolrClient(TAXONOMY));
    }

    @AfterEach
    void tearDown() {
        dataStoreManager.cleanSolr(UNIPROT);
        dataStoreManager.cleanSolr(TAXONOMY);
    }

    @Test
    void viewByTaxonomy_whenNoParentSpecifiedAndNoTraversalAndQuerySpecifiedWithField()
            throws Exception {
        prepareSingleRootNodeWithNoChildren();

        mockMvc.perform(
                        get(PATH)
                                .param("query", "organism_id:" + TAX_ID_0_STRING)
                                .param("parent", EMPTY_PARENT))
                .andDo(log())
                .andExpect(jsonPath("$.results[0].id", is(TAX_ID_0_STRING)))
                .andExpect(jsonPath("$.results[0].label", is(TAX_SCIENTIFIC_0)))
                .andExpect(jsonPath("$.results[0].expand", is(false)))
                .andExpect(jsonPath("$.results[0].count", is(1)))
                .andExpect(jsonPath("$.results.size()", is(1)))
                .andExpect(jsonPath("$.ancestors.size()", is(0)));
    }

    @Test
    void viewByTaxonomy_whenNoParentSpecifiedAndNoTraversalAndFreeFormQuery() throws Exception {
        prepareSingleRootNodeWithNoChildren();

        mockMvc.perform(get(PATH).param("query", TAX_ID_0_STRING).param("parent", EMPTY_PARENT))
                .andDo(log())
                .andExpect(jsonPath("$.results[0].id", is(TAX_ID_0_STRING)))
                .andExpect(jsonPath("$.results[0].label", is(TAX_SCIENTIFIC_0)))
                .andExpect(jsonPath("$.results[0].expand", is(false)))
                .andExpect(jsonPath("$.results[0].count", is(1)))
                .andExpect(jsonPath("$.results.size()", is(1)))
                .andExpect(jsonPath("$.ancestors.size()", is(0)));
    }

    @Test
    void viewByTaxonomy_whenNoParentSpecifiedAndTraversalAndQuerySpecifiedWithField()
            throws Exception {
        prepareSingleRootWithTwoLevelsOfChildren();

        mockMvc.perform(
                        get(PATH)
                                .param("query", "organism_id:" + TAX_ID_2_STRING)
                                .param("parent", EMPTY_PARENT))
                .andDo(log())
                .andExpect(jsonPath("$.results[0].id", is(TAX_ID_2_STRING)))
                .andExpect(jsonPath("$.results[0].label", is(TAX_SCIENTIFIC_2)))
                .andExpect(jsonPath("$.results[0].expand", is(false)))
                .andExpect(jsonPath("$.results[0].count", is(1)))
                .andExpect(jsonPath("$.results.size()", is(1)))
                .andExpect(jsonPath("$.ancestors[0].id", is(TAX_ID_0_STRING)))
                .andExpect(jsonPath("$.ancestors[0].label", is(TAX_SCIENTIFIC_0)))
                .andExpect(jsonPath("$.ancestors[1].id", is(TAX_ID_1_STRING)))
                .andExpect(jsonPath("$.ancestors[1].label", is(TAX_SCIENTIFIC_1)))
                .andExpect(jsonPath("$.ancestors.size()", is(2)));
    }

    @Test
    void viewByTaxonomy_whenNoParentSpecifiedAndTraversalAndFreeFormQuery() throws Exception {
        prepareSingleRootWithTwoLevelsOfChildren();

        mockMvc.perform(get(PATH).param("query", TAX_ID_2_STRING).param("parent", EMPTY_PARENT))
                .andDo(log())
                .andExpect(jsonPath("$.results[0].id", is(TAX_ID_2_STRING)))
                .andExpect(jsonPath("$.results[0].label", is(TAX_SCIENTIFIC_2)))
                .andExpect(jsonPath("$.results[0].expand", is(false)))
                .andExpect(jsonPath("$.results[0].count", is(1)))
                .andExpect(jsonPath("$.results.size()", is(1)))
                .andExpect(jsonPath("$.ancestors[0].id", is(TAX_ID_0_STRING)))
                .andExpect(jsonPath("$.ancestors[0].label", is(TAX_SCIENTIFIC_0)))
                .andExpect(jsonPath("$.ancestors[1].id", is(TAX_ID_1_STRING)))
                .andExpect(jsonPath("$.ancestors[1].label", is(TAX_SCIENTIFIC_1)))
                .andExpect(jsonPath("$.ancestors.size()", is(2)));
    }

    @Test
    void viewByTaxonomy_whenParentSpecifiedAndQuerySpecifiedWithField() throws Exception {
        prepareSingleRootWithTwoLevelsOfChildren();

        mockMvc.perform(
                        get(PATH)
                                .param("query", "taxonomy_id:" + TAX_ID_1_STRING)
                                .param("parent", TAX_ID_0_STRING))
                .andDo(log())
                .andExpect(jsonPath("$.results[0].id", is(TAX_ID_2_STRING)))
                .andExpect(jsonPath("$.results[0].label", is(TAX_SCIENTIFIC_2)))
                .andExpect(jsonPath("$.results[0].expand", is(false)))
                .andExpect(jsonPath("$.results[0].count", is(1)))
                .andExpect(jsonPath("$.results.size()", is(1)))
                .andExpect(jsonPath("$.ancestors[0].id", is(TAX_ID_1_STRING)))
                .andExpect(jsonPath("$.ancestors[0].label", is(TAX_SCIENTIFIC_1)))
                .andExpect(jsonPath("$.ancestors.size()", is(1)));
    }

    @Test
    void viewByTaxonomy_whenParentNotSpecifiedAndTraversalAndFreeFormQuery() throws Exception {
        prepareSingleRootWithTwoLevelsOfChildren();

        mockMvc.perform(get(PATH).param("query", TAX_ID_1_STRING).param("parent", EMPTY_PARENT))
                .andDo(log())
                .andExpect(jsonPath("$.results[0].id", is(TAX_ID_2_STRING)))
                .andExpect(jsonPath("$.results[0].label", is(TAX_SCIENTIFIC_2)))
                .andExpect(jsonPath("$.results[0].expand", is(false)))
                .andExpect(jsonPath("$.results[0].count", is(1)))
                .andExpect(jsonPath("$.results.size()", is(1)))
                .andExpect(jsonPath("$.ancestors[0].id", is(TAX_ID_0_STRING)))
                .andExpect(jsonPath("$.ancestors[0].label", is(TAX_SCIENTIFIC_0)))
                .andExpect(jsonPath("$.ancestors[1].id", is(TAX_ID_1_STRING)))
                .andExpect(jsonPath("$.ancestors[1].label", is(TAX_SCIENTIFIC_1)))
                .andExpect(jsonPath("$.ancestors.size()", is(2)));
    }

    @Test
    void viewByTaxonomy_whenParentSpecifiedAndTraversalAndFreeFormQuery() throws Exception {
        prepareSingleRootWithTwoLevelsOfChildren();

        mockMvc.perform(get(PATH).param("query", TAX_ID_1_STRING).param("parent", TAX_ID_0_STRING))
                .andDo(log())
                .andExpect(jsonPath("$.results[0].id", is(TAX_ID_2_STRING)))
                .andExpect(jsonPath("$.results[0].label", is(TAX_SCIENTIFIC_2)))
                .andExpect(jsonPath("$.results[0].expand", is(false)))
                .andExpect(jsonPath("$.results[0].count", is(1)))
                .andExpect(jsonPath("$.results.size()", is(1)))
                .andExpect(jsonPath("$.ancestors[0].id", is(TAX_ID_1_STRING)))
                .andExpect(jsonPath("$.ancestors[0].label", is(TAX_SCIENTIFIC_1)))
                .andExpect(jsonPath("$.ancestors.size()", is(1)));
    }

    @Test
    void viewByTaxonomy_emptyResults() throws Exception {
        prepareSingleRootNodeWithNoChildren();

        mockMvc.perform(
                        get(PATH)
                                .param("query", "organism_id:" + TAX_ID_1_STRING)
                                .param("parent", EMPTY_PARENT))
                .andDo(log())
                .andExpect(jsonPath("$.results.size()", is(0)))
                .andExpect(jsonPath("$.ancestors.size()", is(0)));
    }

    @Test
    void viewByTaxonomy_whenFreeFormQueryAndEmptyResults() throws Exception {
        prepareSingleRootNodeWithNoChildren();

        mockMvc.perform(get(PATH).param("query", TAX_ID_1_STRING).param("parent", EMPTY_PARENT))
                .andDo(log())
                .andExpect(jsonPath("$.results.size()", is(0)))
                .andExpect(jsonPath("$.ancestors.size()", is(0)));
    }

    @Test
    void viewByTaxonomy_whenQueryNotSpecified() throws Exception {
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
        saveTaxonomyDocument((long) TAX_ID_0, TAX_SCIENTIFIC_0, null);
        saveUniProtDocument(ACCESSION_0, TAX_ID_0, List.of(TAX_ID_0));
        saveTaxonomyDocument((long) TAX_ID_1, TAX_SCIENTIFIC_1, (long) TAX_ID_0);
        saveUniProtDocument(ACCESSION_1, TAX_ID_1, List.of(TAX_ID_0, TAX_ID_1));
        saveTaxonomyDocument((long) TAX_ID_2, TAX_SCIENTIFIC_2, (long) TAX_ID_1);
        saveUniProtDocument(ACCESSION_2, TAX_ID_2, List.of(TAX_ID_0, TAX_ID_1, TAX_ID_2));
    }

    private void prepareSingleRootNodeWithNoChildren() throws Exception {
        saveTaxonomyDocument((long) TAX_ID_0, TAX_SCIENTIFIC_0, null);
        saveUniProtDocument(ACCESSION_0, TAX_ID_0, List.of(TAX_ID_0));
    }

    private void saveUniProtDocument(String accession, int organismId, List<Integer> taxonomies) {
        UniProtDocument uniProtDocument = new UniProtDocument();
        uniProtDocument.active = true;
        uniProtDocument.accession = accession;
        uniProtDocument.organismTaxId = organismId;
        uniProtDocument.taxLineageIds = taxonomies;
        save(UNIPROT, uniProtDocument);
    }

    private void saveTaxonomyDocument(Long taxId, String scientificName, Long parent)
            throws Exception {
        byte[] scientific00s =
                TaxonomyJsonConfig.getInstance()
                        .getFullObjectMapper()
                        .writeValueAsBytes(
                                new TaxonomyEntryBuilder()
                                        .taxonId(taxId)
                                        .scientificName(scientificName)
                                        .build());
        TaxonomyDocument taxonomyDocument =
                TaxonomyDocument.builder()
                        .id(String.valueOf(taxId))
                        .taxId(taxId)
                        .parent(parent)
                        .active(true)
                        .taxonomyObj(scientific00s)
                        .build();
        save(TAXONOMY, taxonomyDocument);
    }

    void save(StoreType type, Document doc) {
        dataStoreManager.saveDocs(type, doc);
    }
}
