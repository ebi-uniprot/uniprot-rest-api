package org.uniprot.api.uniprotkb.controller;

import static org.hamcrest.core.Is.is;

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
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultHandlers;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.uniprot.api.support.data.common.taxonomy.repository.TaxonomyRepository;
import org.uniprot.api.uniprotkb.common.repository.search.UniprotQueryRepository;
import org.uniprot.core.json.parser.taxonomy.TaxonomyJsonConfig;
import org.uniprot.core.taxonomy.impl.TaxonomyEntryBuilder;
import org.uniprot.store.indexer.DataStoreManager;
import org.uniprot.store.search.SolrCollection;
import org.uniprot.store.search.document.taxonomy.TaxonomyDocument;
import org.uniprot.store.search.document.uniprot.UniProtDocument;

@ExtendWith(SpringExtension.class)
@WebMvcTest(controllers = GroupByController.class)
@AutoConfigureWebClient
@ActiveProfiles({"offline"})
class GroupByTaxonomyControllerIT extends GroupByControllerIT {
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
        dataStoreManager.addSolrClient(DataStoreManager.StoreType.UNIPROT, SolrCollection.uniprot);
        dataStoreManager.addSolrClient(
                DataStoreManager.StoreType.TAXONOMY, SolrCollection.taxonomy);
    }

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(
                uniprotQueryRepository,
                "solrClient",
                dataStoreManager.getSolrClient(DataStoreManager.StoreType.UNIPROT));
        ReflectionTestUtils.setField(
                taxonomyRepository,
                "solrClient",
                dataStoreManager.getSolrClient(DataStoreManager.StoreType.TAXONOMY));
    }

    @AfterEach
    void tearDown() {
        dataStoreManager.cleanSolr(DataStoreManager.StoreType.UNIPROT);
        dataStoreManager.cleanSolr(DataStoreManager.StoreType.TAXONOMY);
    }

    @Test
    void getGroupByTaxonomy_whenNoParentSpecifiedAndNoTraversalAndQuerySpecifiedWithField()
            throws Exception {
        prepareSingleRootNodeWithNoChildren();

        mockMvc.perform(
                        MockMvcRequestBuilders.get(PATH)
                                .param("query", "organism_id:" + TAX_ID_0_STRING))
                .andDo(MockMvcResultHandlers.log())
                .andExpect(MockMvcResultMatchers.jsonPath("$.groups[0].id", is(TAX_ID_0_STRING)))
                .andExpect(
                        MockMvcResultMatchers.jsonPath("$.groups[0].label", is(TAX_SCIENTIFIC_0)))
                .andExpect(MockMvcResultMatchers.jsonPath("$.groups[0].expandable", is(false)))
                .andExpect(MockMvcResultMatchers.jsonPath("$.groups[0].count", is(1)))
                .andExpect(MockMvcResultMatchers.jsonPath("$.groups.size()", is(1)))
                .andExpect(MockMvcResultMatchers.jsonPath("$.ancestors.size()", is(0)))
                .andExpect(MockMvcResultMatchers.jsonPath("$.parent.label").doesNotExist())
                .andExpect(MockMvcResultMatchers.jsonPath("$.parent.count", is(1)));
    }

    @Test
    void getGroupByTaxonomy_whenNoParentSpecifiedAndNoTraversalAndFreeFormQuery() throws Exception {
        prepareSingleRootNodeWithNoChildren();

        mockMvc.perform(MockMvcRequestBuilders.get(PATH).param("query", TAX_ID_0_STRING))
                .andDo(MockMvcResultHandlers.log())
                .andExpect(MockMvcResultMatchers.jsonPath("$.groups[0].id", is(TAX_ID_0_STRING)))
                .andExpect(
                        MockMvcResultMatchers.jsonPath("$.groups[0].label", is(TAX_SCIENTIFIC_0)))
                .andExpect(MockMvcResultMatchers.jsonPath("$.groups[0].expandable", is(false)))
                .andExpect(MockMvcResultMatchers.jsonPath("$.groups[0].count", is(1)))
                .andExpect(MockMvcResultMatchers.jsonPath("$.groups.size()", is(1)))
                .andExpect(MockMvcResultMatchers.jsonPath("$.ancestors.size()", is(0)))
                .andExpect(MockMvcResultMatchers.jsonPath("$.parent.label").doesNotExist())
                .andExpect(MockMvcResultMatchers.jsonPath("$.parent.count", is(1)));
    }

    @Test
    void getGroupByTaxonomy_whenNoParentSpecifiedAndTraversalAndQuerySpecifiedWithField()
            throws Exception {
        prepareSingleRootWithTwoLevelsOfChildren();

        mockMvc.perform(
                        MockMvcRequestBuilders.get(PATH)
                                .param("query", "organism_id:" + TAX_ID_2_STRING))
                .andDo(MockMvcResultHandlers.log())
                .andExpect(MockMvcResultMatchers.jsonPath("$.groups[0].id", is(TAX_ID_2_STRING)))
                .andExpect(
                        MockMvcResultMatchers.jsonPath("$.groups[0].label", is(TAX_SCIENTIFIC_2)))
                .andExpect(MockMvcResultMatchers.jsonPath("$.groups[0].expandable", is(false)))
                .andExpect(MockMvcResultMatchers.jsonPath("$.groups[0].count", is(1)))
                .andExpect(MockMvcResultMatchers.jsonPath("$.groups.size()", is(1)))
                .andExpect(MockMvcResultMatchers.jsonPath("$.ancestors[0].id", is(TAX_ID_0_STRING)))
                .andExpect(
                        MockMvcResultMatchers.jsonPath(
                                "$.ancestors[0].label", is(TAX_SCIENTIFIC_0)))
                .andExpect(MockMvcResultMatchers.jsonPath("$.ancestors[1].id", is(TAX_ID_1_STRING)))
                .andExpect(
                        MockMvcResultMatchers.jsonPath(
                                "$.ancestors[1].label", is(TAX_SCIENTIFIC_1)))
                .andExpect(MockMvcResultMatchers.jsonPath("$.ancestors.size()", is(2)))
                .andExpect(MockMvcResultMatchers.jsonPath("$.parent.label").doesNotExist())
                .andExpect(MockMvcResultMatchers.jsonPath("$.parent.count", is(1)));
    }

    @Test
    void getGroupByTaxonomy_whenNoParentSpecifiedAndTraversalAndFreeFormQuery() throws Exception {
        prepareSingleRootWithTwoLevelsOfChildren();

        mockMvc.perform(MockMvcRequestBuilders.get(PATH).param("query", TAX_ID_2_STRING))
                .andDo(MockMvcResultHandlers.log())
                .andExpect(MockMvcResultMatchers.jsonPath("$.groups[0].id", is(TAX_ID_2_STRING)))
                .andExpect(
                        MockMvcResultMatchers.jsonPath("$.groups[0].label", is(TAX_SCIENTIFIC_2)))
                .andExpect(MockMvcResultMatchers.jsonPath("$.groups[0].expandable", is(false)))
                .andExpect(MockMvcResultMatchers.jsonPath("$.groups[0].count", is(1)))
                .andExpect(MockMvcResultMatchers.jsonPath("$.groups.size()", is(1)))
                .andExpect(MockMvcResultMatchers.jsonPath("$.ancestors[0].id", is(TAX_ID_0_STRING)))
                .andExpect(
                        MockMvcResultMatchers.jsonPath(
                                "$.ancestors[0].label", is(TAX_SCIENTIFIC_0)))
                .andExpect(MockMvcResultMatchers.jsonPath("$.ancestors[1].id", is(TAX_ID_1_STRING)))
                .andExpect(
                        MockMvcResultMatchers.jsonPath(
                                "$.ancestors[1].label", is(TAX_SCIENTIFIC_1)))
                .andExpect(MockMvcResultMatchers.jsonPath("$.ancestors.size()", is(2)))
                .andExpect(MockMvcResultMatchers.jsonPath("$.parent.label").doesNotExist())
                .andExpect(MockMvcResultMatchers.jsonPath("$.parent.count", is(1)));
    }

    @Test
    void getGroupByTaxonomy_whenParentSpecifiedAndQuerySpecifiedWithField() throws Exception {
        prepareSingleRootWithTwoLevelsOfChildren();

        mockMvc.perform(
                        MockMvcRequestBuilders.get(PATH)
                                .param("query", "taxonomy_id:" + TAX_ID_1_STRING)
                                .param("parent", TAX_ID_0_STRING))
                .andDo(MockMvcResultHandlers.log())
                .andExpect(MockMvcResultMatchers.jsonPath("$.groups[0].id", is(TAX_ID_2_STRING)))
                .andExpect(
                        MockMvcResultMatchers.jsonPath("$.groups[0].label", is(TAX_SCIENTIFIC_2)))
                .andExpect(MockMvcResultMatchers.jsonPath("$.groups[0].expandable", is(false)))
                .andExpect(MockMvcResultMatchers.jsonPath("$.groups[0].count", is(1)))
                .andExpect(MockMvcResultMatchers.jsonPath("$.groups.size()", is(1)))
                .andExpect(MockMvcResultMatchers.jsonPath("$.ancestors[0].id", is(TAX_ID_1_STRING)))
                .andExpect(
                        MockMvcResultMatchers.jsonPath(
                                "$.ancestors[0].label", is(TAX_SCIENTIFIC_1)))
                .andExpect(MockMvcResultMatchers.jsonPath("$.ancestors.size()", is(1)))
                .andExpect(MockMvcResultMatchers.jsonPath("$.parent.label", is("scientific_9600")))
                .andExpect(MockMvcResultMatchers.jsonPath("$.parent.count", is(2)));
    }

    @Test
    void getGroupBy_Chebi() throws Exception {
        prepareSingleRootWithTwoLevelsOfChildren();

        getMockMvc()
                .perform(MockMvcRequestBuilders.get(getPath()).param("query", "CHEBI:1234"))
                .andDo(MockMvcResultHandlers.log())
                .andExpect(MockMvcResultMatchers.jsonPath("$.groups.size()", is(0)))
                .andExpect(MockMvcResultMatchers.jsonPath("$.ancestors.size()", is(0)))
                .andExpect(MockMvcResultMatchers.jsonPath("$.parent.label").doesNotExist())
                .andExpect(MockMvcResultMatchers.jsonPath("$.parent.count", is(0)));
    }

    @Test
    void getGroupByTaxonomy_whenParentNotSpecifiedAndTraversalAndFreeFormQuery() throws Exception {
        prepareSingleRootWithTwoLevelsOfChildren();

        mockMvc.perform(MockMvcRequestBuilders.get(PATH).param("query", TAX_ID_1_STRING))
                .andDo(MockMvcResultHandlers.log())
                .andExpect(MockMvcResultMatchers.jsonPath("$.groups[0].id", is(TAX_ID_2_STRING)))
                .andExpect(
                        MockMvcResultMatchers.jsonPath("$.groups[0].label", is(TAX_SCIENTIFIC_2)))
                .andExpect(MockMvcResultMatchers.jsonPath("$.groups[0].expandable", is(false)))
                .andExpect(MockMvcResultMatchers.jsonPath("$.groups[0].count", is(1)))
                .andExpect(MockMvcResultMatchers.jsonPath("$.groups.size()", is(1)))
                .andExpect(MockMvcResultMatchers.jsonPath("$.ancestors[0].id", is(TAX_ID_0_STRING)))
                .andExpect(
                        MockMvcResultMatchers.jsonPath(
                                "$.ancestors[0].label", is(TAX_SCIENTIFIC_0)))
                .andExpect(MockMvcResultMatchers.jsonPath("$.ancestors[1].id", is(TAX_ID_1_STRING)))
                .andExpect(
                        MockMvcResultMatchers.jsonPath(
                                "$.ancestors[1].label", is(TAX_SCIENTIFIC_1)))
                .andExpect(MockMvcResultMatchers.jsonPath("$.ancestors.size()", is(2)))
                .andExpect(MockMvcResultMatchers.jsonPath("$.parent.label").doesNotExist())
                .andExpect(MockMvcResultMatchers.jsonPath("$.parent.count", is(2)));
    }

    @Test
    void getGroupByTaxonomy_whenParentSpecifiedAndTraversalAndFreeFormQuery() throws Exception {
        prepareSingleRootWithTwoLevelsOfChildren();

        mockMvc.perform(
                        MockMvcRequestBuilders.get(PATH)
                                .param("query", TAX_ID_1_STRING)
                                .param("parent", TAX_ID_0_STRING))
                .andDo(MockMvcResultHandlers.log())
                .andExpect(MockMvcResultMatchers.jsonPath("$.groups[0].id", is(TAX_ID_2_STRING)))
                .andExpect(
                        MockMvcResultMatchers.jsonPath("$.groups[0].label", is(TAX_SCIENTIFIC_2)))
                .andExpect(MockMvcResultMatchers.jsonPath("$.groups[0].expandable", is(false)))
                .andExpect(MockMvcResultMatchers.jsonPath("$.groups[0].count", is(1)))
                .andExpect(MockMvcResultMatchers.jsonPath("$.groups.size()", is(1)))
                .andExpect(MockMvcResultMatchers.jsonPath("$.ancestors[0].id", is(TAX_ID_1_STRING)))
                .andExpect(
                        MockMvcResultMatchers.jsonPath(
                                "$.ancestors[0].label", is(TAX_SCIENTIFIC_1)))
                .andExpect(MockMvcResultMatchers.jsonPath("$.ancestors.size()", is(1)))
                .andExpect(MockMvcResultMatchers.jsonPath("$.parent.label", is("scientific_9600")))
                .andExpect(MockMvcResultMatchers.jsonPath("$.parent.count", is(2)));
    }

    private void prepareSingleRootWithTwoLevelsOfChildren() throws Exception {
        saveTaxonomyDocument((long) TAX_ID_0, TAX_SCIENTIFIC_0, null);
        saveUniProtDocument(ACCESSION_0, TAX_ID_0, List.of(TAX_ID_0));
        saveTaxonomyDocument((long) TAX_ID_1, TAX_SCIENTIFIC_1, (long) TAX_ID_0);
        saveUniProtDocument(ACCESSION_1, TAX_ID_1, List.of(TAX_ID_0, TAX_ID_1));
        saveTaxonomyDocument((long) TAX_ID_2, TAX_SCIENTIFIC_2, (long) TAX_ID_1);
        saveUniProtDocument(ACCESSION_2, TAX_ID_2, List.of(TAX_ID_0, TAX_ID_1, TAX_ID_2));
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
        saveTaxonomyDocument((long) TAX_ID_0, TAX_SCIENTIFIC_0, null);
        saveUniProtDocument(ACCESSION_0, TAX_ID_0, List.of(TAX_ID_0));
    }

    private void saveUniProtDocument(String accession, int organismId, List<Integer> taxonomies) {
        UniProtDocument uniProtDocument = new UniProtDocument();
        uniProtDocument.active = true;
        uniProtDocument.accession = accession;
        uniProtDocument.organismTaxId = organismId;
        uniProtDocument.taxLineageIds = taxonomies;
        uniProtDocument.chebi.add(CHEBI_ID);
        save(DataStoreManager.StoreType.UNIPROT, uniProtDocument);
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
        save(DataStoreManager.StoreType.TAXONOMY, taxonomyDocument);
    }
}
