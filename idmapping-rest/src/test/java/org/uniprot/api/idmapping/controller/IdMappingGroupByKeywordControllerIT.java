package org.uniprot.api.idmapping.controller;

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
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultHandlers;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.uniprot.api.idmapping.IdMappingREST;
import org.uniprot.api.idmapping.common.IdMappingDataStoreTestConfig;
import org.uniprot.api.idmapping.common.model.IdMappingJob;
import org.uniprot.api.rest.download.model.JobStatus;
import org.uniprot.api.support.data.common.keyword.repository.KeywordRepository;
import org.uniprot.api.uniprotkb.common.repository.search.UniprotQueryRepository;
import org.uniprot.core.cv.keyword.KeywordId;
import org.uniprot.core.cv.keyword.impl.KeywordEntryBuilder;
import org.uniprot.core.cv.keyword.impl.KeywordIdBuilder;
import org.uniprot.core.json.parser.keyword.KeywordJsonConfig;
import org.uniprot.store.indexer.DataStoreManager;
import org.uniprot.store.search.SolrCollection;
import org.uniprot.store.search.document.keyword.KeywordDocument;
import org.uniprot.store.search.document.uniprot.UniProtDocument;

import java.nio.ByteBuffer;
import java.util.List;

import static org.hamcrest.core.Is.is;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {IdMappingDataStoreTestConfig.class, IdMappingREST.class})
@WebMvcTest(controllers = IdMappingGroupByController.class)
@AutoConfigureWebClient
@ActiveProfiles(profiles = {"offline", "idmapping"})
public class IdMappingGroupByKeywordControllerIT extends IdMappingGroupByControllerIT {
    private static final String ACCESSION_0 = "I00005";
    private static final String ACCESSION_1 = "I00007";
    private static final String ACCESSION_2 = "I00009";
    private static final String ORGANISM_ID_0 = "29";
    private static final String ORGANISM_ID_1 = "324";
    private static final String ORGANISM_ID_2 = "994";
    private static final String KEYWORD_ID_0 = "KW-0000";
    private static final String KEYWORD_NAME_0 = "keywordName0";
    private static final String KEYWORD_ID_1 = "KW-0001";
    private static final String KEYWORD_NAME_1 = "keywordName1";
    private static final String KEYWORD_ID_2 = "KW-0002";
    private static final String KEYWORD_NAME_2 = "keywordName2";
    private static final String KEYWORD_ID_3 = "KW-0003";
    private static final String KEYWORD_NAME_3 = "keywordName3";
    private static final String KEYWORD_ID_4 = "KW-0004";
    private static final String KEYWORD_NAME_4 = "keywordName4";
    public static final String PATH = "/idmapping/%s/groups/keyword";
    @RegisterExtension static DataStoreManager dataStoreManager = new DataStoreManager();
    @Autowired private MockMvc mockMvc;
    @Autowired private UniprotQueryRepository uniprotQueryRepository;
    @Autowired private KeywordRepository keywordRepository;

    @BeforeAll
    static void beforeAll() {
        dataStoreManager.addSolrClient(DataStoreManager.StoreType.UNIPROT, SolrCollection.uniprot);
        dataStoreManager.addSolrClient(DataStoreManager.StoreType.KEYWORD, SolrCollection.keyword);
    }

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(
                uniprotQueryRepository,
                "solrClient",
                dataStoreManager.getSolrClient(DataStoreManager.StoreType.UNIPROT));
        ReflectionTestUtils.setField(
                keywordRepository,
                "solrClient",
                dataStoreManager.getSolrClient(DataStoreManager.StoreType.KEYWORD));
    }

    @AfterEach
    void tearDown() {
        dataStoreManager.cleanSolr(DataStoreManager.StoreType.UNIPROT);
        dataStoreManager.cleanSolr(DataStoreManager.StoreType.KEYWORD);
    }

    @Test
    void getGroupByKeyword_whenNoParentSpecifiedAndNoTraversalAndQuerySpecifiedWithField()
            throws Exception {
        prepareSingleRootNodeWithNoChildren();
        IdMappingJob job =
                idMappingResultJobOp.createAndPutJobInCache(
                        this.maxToIdsWithFacetsAllowed - 1, JobStatus.FINISHED);

        mockMvc.perform(
                        MockMvcRequestBuilders.get(getUrlWithJobId(job.getJobId()))
                                .param("query", "organism_id:" + ORGANISM_ID_0))
                .andDo(MockMvcResultHandlers.log())
                .andExpect(MockMvcResultMatchers.jsonPath("$.groups[0].id", is(KEYWORD_ID_0)))
                .andExpect(MockMvcResultMatchers.jsonPath("$.groups[0].label", is(KEYWORD_NAME_0)))
                .andExpect(MockMvcResultMatchers.jsonPath("$.groups[0].expandable", is(false)))
                .andExpect(MockMvcResultMatchers.jsonPath("$.groups[0].count", is(1)))
                .andExpect(MockMvcResultMatchers.jsonPath("$.groups.size()", is(1)))
                .andExpect(MockMvcResultMatchers.jsonPath("$.ancestors.size()", is(0)))
                .andExpect(MockMvcResultMatchers.jsonPath("$.parent.label").doesNotExist())
                .andExpect(MockMvcResultMatchers.jsonPath("$.parent.count", is(1)));
    }

    @Test
    void getGroupByKeyword_whenNoParentSpecifiedAndNoTraversalAndFreeFormQuery() throws Exception {
        prepareSingleRootNodeWithNoChildren();
        IdMappingJob job =
                idMappingResultJobOp.createAndPutJobInCache(
                        this.maxToIdsWithFacetsAllowed - 1, JobStatus.FINISHED);

        mockMvc.perform(MockMvcRequestBuilders.get(getUrlWithJobId(job.getJobId())).param("query", ORGANISM_ID_0))
                .andDo(MockMvcResultHandlers.log())
                .andExpect(MockMvcResultMatchers.jsonPath("$.groups[0].id", is(KEYWORD_ID_0)))
                .andExpect(MockMvcResultMatchers.jsonPath("$.groups[0].label", is(KEYWORD_NAME_0)))
                .andExpect(MockMvcResultMatchers.jsonPath("$.groups[0].expandable", is(false)))
                .andExpect(MockMvcResultMatchers.jsonPath("$.groups[0].count", is(1)))
                .andExpect(MockMvcResultMatchers.jsonPath("$.groups.size()", is(1)))
                .andExpect(MockMvcResultMatchers.jsonPath("$.ancestors.size()", is(0)))
                .andExpect(MockMvcResultMatchers.jsonPath("$.parent.label").doesNotExist())
                .andExpect(MockMvcResultMatchers.jsonPath("$.parent.count", is(1)));
    }

    @Test
    void getGroupByKeyword_whenNoParentSpecifiedAndTraversalAndQuerySpecifiedWithField()
            throws Exception {
        prepareSingleRootWithTwoLevelsOfChildren();
        IdMappingJob job =
                idMappingResultJobOp.createAndPutJobInCache(
                        this.maxToIdsWithFacetsAllowed - 1, JobStatus.FINISHED);

        mockMvc.perform(
                        MockMvcRequestBuilders.get(getUrlWithJobId(job.getJobId()))
                                .param("query", "organism_id:" + ORGANISM_ID_2))
                .andDo(MockMvcResultHandlers.log())
                .andExpect(MockMvcResultMatchers.jsonPath("$.groups[0].id", is(KEYWORD_ID_2)))
                .andExpect(MockMvcResultMatchers.jsonPath("$.groups[0].label", is(KEYWORD_NAME_2)))
                .andExpect(MockMvcResultMatchers.jsonPath("$.groups[0].expandable", is(false)))
                .andExpect(MockMvcResultMatchers.jsonPath("$.groups[0].count", is(1)))
                .andExpect(MockMvcResultMatchers.jsonPath("$.groups.size()", is(1)))
                .andExpect(MockMvcResultMatchers.jsonPath("$.ancestors[0].id", is(KEYWORD_ID_0)))
                .andExpect(
                        MockMvcResultMatchers.jsonPath("$.ancestors[0].label", is(KEYWORD_NAME_0)))
                .andExpect(MockMvcResultMatchers.jsonPath("$.ancestors[1].id", is(KEYWORD_ID_1)))
                .andExpect(
                        MockMvcResultMatchers.jsonPath("$.ancestors[1].label", is(KEYWORD_NAME_1)))
                .andExpect(MockMvcResultMatchers.jsonPath("$.ancestors.size()", is(2)))
                .andExpect(MockMvcResultMatchers.jsonPath("$.parent.label").doesNotExist())
                .andExpect(MockMvcResultMatchers.jsonPath("$.parent.count", is(1)));
    }

    @Test
    void getGroupByKeyword_whenNoParentSpecifiedAndTraversalAndFreeFormQuery() throws Exception {
        prepareSingleRootWithTwoLevelsOfChildren();
        IdMappingJob job =
                idMappingResultJobOp.createAndPutJobInCache(
                        this.maxToIdsWithFacetsAllowed - 1, JobStatus.FINISHED);

        mockMvc.perform(MockMvcRequestBuilders.get(getUrlWithJobId(job.getJobId())).param("query", ORGANISM_ID_2))
                .andDo(MockMvcResultHandlers.log())
                .andExpect(MockMvcResultMatchers.jsonPath("$.groups[0].id", is(KEYWORD_ID_2)))
                .andExpect(MockMvcResultMatchers.jsonPath("$.groups[0].label", is(KEYWORD_NAME_2)))
                .andExpect(MockMvcResultMatchers.jsonPath("$.groups[0].expandable", is(false)))
                .andExpect(MockMvcResultMatchers.jsonPath("$.groups[0].count", is(1)))
                .andExpect(MockMvcResultMatchers.jsonPath("$.groups.size()", is(1)))
                .andExpect(MockMvcResultMatchers.jsonPath("$.ancestors[0].id", is(KEYWORD_ID_0)))
                .andExpect(
                        MockMvcResultMatchers.jsonPath("$.ancestors[0].label", is(KEYWORD_NAME_0)))
                .andExpect(MockMvcResultMatchers.jsonPath("$.ancestors[1].id", is(KEYWORD_ID_1)))
                .andExpect(
                        MockMvcResultMatchers.jsonPath("$.ancestors[1].label", is(KEYWORD_NAME_1)))
                .andExpect(MockMvcResultMatchers.jsonPath("$.ancestors.size()", is(2)))
                .andExpect(MockMvcResultMatchers.jsonPath("$.parent.label").doesNotExist())
                .andExpect(MockMvcResultMatchers.jsonPath("$.parent.count", is(1)));
    }

    @Test
    void getGroupByKeyword_whenParentSpecifiedAndQuerySpecifiedWithField() throws Exception {
        prepareSingleRootWithTwoLevelsOfChildrenAndSidePaths();
        IdMappingJob job =
                idMappingResultJobOp.createAndPutJobInCache(
                        this.maxToIdsWithFacetsAllowed - 1, JobStatus.FINISHED);

        mockMvc.perform(
                        MockMvcRequestBuilders.get(getUrlWithJobId(job.getJobId()))
                                .param("query", "organism_id:" + ORGANISM_ID_2)
                                .param("parent", KEYWORD_ID_0))
                .andDo(MockMvcResultHandlers.log())
                .andExpect(MockMvcResultMatchers.jsonPath("$.groups[0].id", is(KEYWORD_ID_2)))
                .andExpect(MockMvcResultMatchers.jsonPath("$.groups[0].label", is(KEYWORD_NAME_2)))
                .andExpect(MockMvcResultMatchers.jsonPath("$.groups[0].expandable", is(false)))
                .andExpect(MockMvcResultMatchers.jsonPath("$.groups[0].count", is(1)))
                .andExpect(MockMvcResultMatchers.jsonPath("$.groups.size()", is(1)))
                .andExpect(MockMvcResultMatchers.jsonPath("$.ancestors[0].id", is(KEYWORD_ID_1)))
                .andExpect(
                        MockMvcResultMatchers.jsonPath("$.ancestors[0].label", is(KEYWORD_NAME_1)))
                .andExpect(MockMvcResultMatchers.jsonPath("$.ancestors.size()", is(1)))
                .andExpect(MockMvcResultMatchers.jsonPath("$.parent.label", is("keywordName0")))
                .andExpect(MockMvcResultMatchers.jsonPath("$.parent.count", is(1)));
    }

    @Test
    void getGroupByKeyword_whenParentNotSpecifiedAndTraversalAndFreeFormQuery() throws Exception {
        prepareSingleRootWithTwoLevelsOfChildren();
        IdMappingJob job =
                idMappingResultJobOp.createAndPutJobInCache(
                        this.maxToIdsWithFacetsAllowed - 1, JobStatus.FINISHED);

        mockMvc.perform(MockMvcRequestBuilders.get(getUrlWithJobId(job.getJobId())).param("query", KEYWORD_ID_2))
                .andDo(MockMvcResultHandlers.log())
                .andExpect(MockMvcResultMatchers.jsonPath("$.groups[0].id", is(KEYWORD_ID_2)))
                .andExpect(MockMvcResultMatchers.jsonPath("$.groups[0].label", is(KEYWORD_NAME_2)))
                .andExpect(MockMvcResultMatchers.jsonPath("$.groups[0].expandable", is(false)))
                .andExpect(MockMvcResultMatchers.jsonPath("$.groups[0].count", is(1)))
                .andExpect(MockMvcResultMatchers.jsonPath("$.groups.size()", is(1)))
                .andExpect(MockMvcResultMatchers.jsonPath("$.ancestors[0].id", is(KEYWORD_ID_0)))
                .andExpect(
                        MockMvcResultMatchers.jsonPath("$.ancestors[0].label", is(KEYWORD_NAME_0)))
                .andExpect(MockMvcResultMatchers.jsonPath("$.ancestors[1].id", is(KEYWORD_ID_1)))
                .andExpect(
                        MockMvcResultMatchers.jsonPath("$.ancestors[1].label", is(KEYWORD_NAME_1)))
                .andExpect(MockMvcResultMatchers.jsonPath("$.ancestors.size()", is(2)))
                .andExpect(MockMvcResultMatchers.jsonPath("$.parent.label").doesNotExist())
                .andExpect(MockMvcResultMatchers.jsonPath("$.parent.count", is(1)));
    }

    @Test
    void getGroupByKeyword_whenParentSpecifiedAndTraversalAndFreeFormQuery() throws Exception {
        prepareSingleRootWithTwoLevelsOfChildrenAndSidePaths();
        IdMappingJob job =
                idMappingResultJobOp.createAndPutJobInCache(
                        this.maxToIdsWithFacetsAllowed - 1, JobStatus.FINISHED);

        mockMvc.perform(
                        MockMvcRequestBuilders.get(getUrlWithJobId(job.getJobId()))
                                .param("query", KEYWORD_ID_2)
                                .param("parent", KEYWORD_ID_0))
                .andDo(MockMvcResultHandlers.log())
                .andExpect(MockMvcResultMatchers.jsonPath("$.groups[0].id", is(KEYWORD_ID_2)))
                .andExpect(MockMvcResultMatchers.jsonPath("$.groups[0].label", is(KEYWORD_NAME_2)))
                .andExpect(MockMvcResultMatchers.jsonPath("$.groups[0].expandable", is(false)))
                .andExpect(MockMvcResultMatchers.jsonPath("$.groups[0].count", is(1)))
                .andExpect(MockMvcResultMatchers.jsonPath("$.groups.size()", is(1)))
                .andExpect(MockMvcResultMatchers.jsonPath("$.ancestors[0].id", is(KEYWORD_ID_1)))
                .andExpect(
                        MockMvcResultMatchers.jsonPath("$.ancestors[0].label", is(KEYWORD_NAME_1)))
                .andExpect(MockMvcResultMatchers.jsonPath("$.ancestors.size()", is(1)))
                .andExpect(MockMvcResultMatchers.jsonPath("$.parent.label", is("keywordName0")))
                .andExpect(MockMvcResultMatchers.jsonPath("$.parent.count", is(1)));
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
        uniProtDocument.chebi.add(CHEBI_ID);
        save(DataStoreManager.StoreType.UNIPROT, uniProtDocument);
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
        save(DataStoreManager.StoreType.KEYWORD, keywordDocument);
    }
}
