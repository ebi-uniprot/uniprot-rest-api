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
import org.springframework.boot.test.mock.mockito.MockBean;
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
import org.uniprot.api.uniprotkb.common.repository.search.UniprotQueryRepository;
import org.uniprot.core.cv.ec.ECEntry;
import org.uniprot.cv.ec.ECRepo;
import org.uniprot.store.indexer.DataStoreManager;
import org.uniprot.store.search.SolrCollection;
import org.uniprot.store.search.document.uniprot.UniProtDocument;

import java.util.List;
import java.util.Optional;

import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.when;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {IdMappingDataStoreTestConfig.class, IdMappingREST.class})
@WebMvcTest(controllers = IdMappingGroupByController.class)
@AutoConfigureWebClient
@ActiveProfiles(profiles = {"offline", "idmapping"})
public class IdMappingGroupByECControllerIT extends IdMappingGroupByControllerIT {
    private static final String ORGANISM_ID_0 = "29";
    private static final String ORGANISM_ID_1 = "517";
    private static final String ORGANISM_ID_2 = "34959";
    private static final String ACCESSION_0 = "I00005";
    private static final String EC_ID_0 = "1.-.-.-";
    private static final String EC_LABEL_0 = "ec_label_0";
    private static final String ACCESSION_1 = "I00007";
    private static final String EC_ID_1 = "1.1.-.-";
    private static final String EC_LABEL_1 = "ec_label_1";
    private static final String ACCESSION_2 = "I00009";
    private static final String EC_ID_2 = "1.1.1.-";
    private static final String EC_LABEL_2 = "ec_label_2";
    public static final String PATH = "/idmapping/%s/groups/ec";
    @RegisterExtension static DataStoreManager dataStoreManager = new DataStoreManager();
    @MockBean
    private ECRepo ecRepo;
    @Autowired private MockMvc mockMvc;
    @Autowired private UniprotQueryRepository repository;

    @BeforeAll
    static void beforeAll() {
        dataStoreManager.addSolrClient(DataStoreManager.StoreType.UNIPROT, SolrCollection.uniprot);
    }

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(
                repository,
                "solrClient",
                dataStoreManager.getSolrClient(DataStoreManager.StoreType.UNIPROT));
    }

    @AfterEach
    void tearDown() {
        dataStoreManager.cleanSolr(DataStoreManager.StoreType.UNIPROT);
    }

    @Test
    void getGroupByEC_whenNoParentSpecifiedAndNoTraversalAndQuerySpecifiedWithField()
            throws Exception {
        prepareSingleRootNodeWithNoChildren();
        IdMappingJob job =
                idMappingResultJobOp.createAndPutJobInCache(
                        this.maxToIdsWithFacetsAllowed - 1, JobStatus.FINISHED);

        mockMvc.perform(
                        MockMvcRequestBuilders.get(getUrlWithJobId(job.getJobId()))
                                .param("query", "organism_id:" + ORGANISM_ID_0))
                .andDo(MockMvcResultHandlers.log())
                .andExpect(MockMvcResultMatchers.jsonPath("$.groups[0].id", is(EC_ID_0)))
                .andExpect(MockMvcResultMatchers.jsonPath("$.groups[0].label", is(EC_LABEL_0)))
                .andExpect(MockMvcResultMatchers.jsonPath("$.groups[0].expandable", is(false)))
                .andExpect(MockMvcResultMatchers.jsonPath("$.groups[0].count", is(1)))
                .andExpect(MockMvcResultMatchers.jsonPath("$.groups.size()", is(1)))
                .andExpect(MockMvcResultMatchers.jsonPath("$.ancestors.size()", is(0)))
                .andExpect(MockMvcResultMatchers.jsonPath("$.parent.label").doesNotExist())
                .andExpect(MockMvcResultMatchers.jsonPath("$.parent.count", is(1)));
    }

    @Test
    void getGroupByEC_whenNoParentSpecifiedAndNoTraversalAndFreeFormQuery() throws Exception {
        prepareSingleRootNodeWithNoChildren();
        IdMappingJob job =
                idMappingResultJobOp.createAndPutJobInCache(
                        this.maxToIdsWithFacetsAllowed - 1, JobStatus.FINISHED);

        mockMvc.perform(MockMvcRequestBuilders.get(getUrlWithJobId(job.getJobId())).param("query", ORGANISM_ID_0))
                .andDo(MockMvcResultHandlers.log())
                .andExpect(MockMvcResultMatchers.jsonPath("$.groups[0].id", is(EC_ID_0)))
                .andExpect(MockMvcResultMatchers.jsonPath("$.groups[0].label", is(EC_LABEL_0)))
                .andExpect(MockMvcResultMatchers.jsonPath("$.groups[0].expandable", is(false)))
                .andExpect(MockMvcResultMatchers.jsonPath("$.groups[0].count", is(1)))
                .andExpect(MockMvcResultMatchers.jsonPath("$.groups.size()", is(1)))
                .andExpect(MockMvcResultMatchers.jsonPath("$.ancestors.size()", is(0)))
                .andExpect(MockMvcResultMatchers.jsonPath("$.parent.label").doesNotExist())
                .andExpect(MockMvcResultMatchers.jsonPath("$.parent.count", is(1)));
    }

    @Test
    void getGroupByEC_whenNoParentSpecifiedAndTraversalAndQuerySpecifiedWithField()
            throws Exception {
        prepareSingleRootWithTwoLevelsOfChildren();
        IdMappingJob job =
                idMappingResultJobOp.createAndPutJobInCache(
                        this.maxToIdsWithFacetsAllowed - 1, JobStatus.FINISHED);

        mockMvc.perform(
                        MockMvcRequestBuilders.get(getUrlWithJobId(job.getJobId()))
                                .param("query", "organism_id:" + ORGANISM_ID_2))
                .andDo(MockMvcResultHandlers.log())
                .andExpect(MockMvcResultMatchers.jsonPath("$.groups[0].id", is(EC_ID_2)))
                .andExpect(MockMvcResultMatchers.jsonPath("$.groups[0].label", is(EC_LABEL_2)))
                .andExpect(MockMvcResultMatchers.jsonPath("$.groups[0].expandable", is(false)))
                .andExpect(MockMvcResultMatchers.jsonPath("$.groups[0].count", is(1)))
                .andExpect(MockMvcResultMatchers.jsonPath("$.groups.size()", is(1)))
                .andExpect(MockMvcResultMatchers.jsonPath("$.ancestors[0].id", is(EC_ID_0)))
                .andExpect(MockMvcResultMatchers.jsonPath("$.ancestors[0].label", is(EC_LABEL_0)))
                .andExpect(MockMvcResultMatchers.jsonPath("$.ancestors[1].id", is(EC_ID_1)))
                .andExpect(MockMvcResultMatchers.jsonPath("$.ancestors[1].label", is(EC_LABEL_1)))
                .andExpect(MockMvcResultMatchers.jsonPath("$.ancestors.size()", is(2)))
                .andExpect(MockMvcResultMatchers.jsonPath("$.parent.label").doesNotExist())
                .andExpect(MockMvcResultMatchers.jsonPath("$.parent.count", is(1)));
    }

    @Test
    void getGroupByEC_whenNoParentSpecifiedAndTraversalAndFreeFormQuery() throws Exception {
        prepareSingleRootWithTwoLevelsOfChildren();
        IdMappingJob job =
                idMappingResultJobOp.createAndPutJobInCache(
                        this.maxToIdsWithFacetsAllowed - 1, JobStatus.FINISHED);

        mockMvc.perform(MockMvcRequestBuilders.get(getUrlWithJobId(job.getJobId())).param("query", ORGANISM_ID_2))
                .andDo(MockMvcResultHandlers.log())
                .andExpect(MockMvcResultMatchers.jsonPath("$.groups[0].id", is(EC_ID_2)))
                .andExpect(MockMvcResultMatchers.jsonPath("$.groups[0].label", is(EC_LABEL_2)))
                .andExpect(MockMvcResultMatchers.jsonPath("$.groups[0].expandable", is(false)))
                .andExpect(MockMvcResultMatchers.jsonPath("$.groups[0].count", is(1)))
                .andExpect(MockMvcResultMatchers.jsonPath("$.groups.size()", is(1)))
                .andExpect(MockMvcResultMatchers.jsonPath("$.ancestors[0].id", is(EC_ID_0)))
                .andExpect(MockMvcResultMatchers.jsonPath("$.ancestors[0].label", is(EC_LABEL_0)))
                .andExpect(MockMvcResultMatchers.jsonPath("$.ancestors[1].id", is(EC_ID_1)))
                .andExpect(MockMvcResultMatchers.jsonPath("$.ancestors[1].label", is(EC_LABEL_1)))
                .andExpect(MockMvcResultMatchers.jsonPath("$.ancestors.size()", is(2)))
                .andExpect(MockMvcResultMatchers.jsonPath("$.parent.label").doesNotExist())
                .andExpect(MockMvcResultMatchers.jsonPath("$.parent.count", is(1)));
    }

    @Test
    void getGroupByEC_whenParentSpecifiedAndQuerySpecifiedWithField() throws Exception {
        prepareSingleRootWithTwoLevelsOfChildren();
        IdMappingJob job =
                idMappingResultJobOp.createAndPutJobInCache(
                        this.maxToIdsWithFacetsAllowed - 1, JobStatus.FINISHED);

        mockMvc.perform(
                        MockMvcRequestBuilders.get(getUrlWithJobId(job.getJobId()))
                                .param("query", "organism_id:" + ORGANISM_ID_2)
                                .param("parent", EC_ID_0))
                .andDo(MockMvcResultHandlers.log())
                .andExpect(MockMvcResultMatchers.jsonPath("$.groups[0].id", is(EC_ID_2)))
                .andExpect(MockMvcResultMatchers.jsonPath("$.groups[0].label", is(EC_LABEL_2)))
                .andExpect(MockMvcResultMatchers.jsonPath("$.groups[0].expandable", is(false)))
                .andExpect(MockMvcResultMatchers.jsonPath("$.groups[0].count", is(1)))
                .andExpect(MockMvcResultMatchers.jsonPath("$.groups.size()", is(1)))
                .andExpect(MockMvcResultMatchers.jsonPath("$.ancestors[0].id", is(EC_ID_1)))
                .andExpect(MockMvcResultMatchers.jsonPath("$.ancestors[0].label", is(EC_LABEL_1)))
                .andExpect(MockMvcResultMatchers.jsonPath("$.ancestors.size()", is(1)))
                .andExpect(MockMvcResultMatchers.jsonPath("$.parent.label", is("ec_label_0")))
                .andExpect(MockMvcResultMatchers.jsonPath("$.parent.count", is(1)));
    }

    @Test
    void getGroupByEC_whenParentNotSpecifiedAndTraversalAndFreeFormQuery() throws Exception {
        prepareSingleRootWithTwoLevelsOfChildren();
        IdMappingJob job =
                idMappingResultJobOp.createAndPutJobInCache(
                        this.maxToIdsWithFacetsAllowed - 1, JobStatus.FINISHED);

        mockMvc.perform(MockMvcRequestBuilders.get(getUrlWithJobId(job.getJobId())).param("query", ORGANISM_ID_2))
                .andDo(MockMvcResultHandlers.log())
                .andExpect(MockMvcResultMatchers.jsonPath("$.groups[0].id", is(EC_ID_2)))
                .andExpect(MockMvcResultMatchers.jsonPath("$.groups[0].label", is(EC_LABEL_2)))
                .andExpect(MockMvcResultMatchers.jsonPath("$.groups[0].expandable", is(false)))
                .andExpect(MockMvcResultMatchers.jsonPath("$.groups[0].count", is(1)))
                .andExpect(MockMvcResultMatchers.jsonPath("$.groups.size()", is(1)))
                .andExpect(MockMvcResultMatchers.jsonPath("$.ancestors[0].id", is(EC_ID_0)))
                .andExpect(MockMvcResultMatchers.jsonPath("$.ancestors[0].label", is(EC_LABEL_0)))
                .andExpect(MockMvcResultMatchers.jsonPath("$.ancestors[1].id", is(EC_ID_1)))
                .andExpect(MockMvcResultMatchers.jsonPath("$.ancestors[1].label", is(EC_LABEL_1)))
                .andExpect(MockMvcResultMatchers.jsonPath("$.ancestors.size()", is(2)))
                .andExpect(MockMvcResultMatchers.jsonPath("$.parent.label").doesNotExist())
                .andExpect(MockMvcResultMatchers.jsonPath("$.parent.count", is(1)));
    }

    @Test
    void getGroupByEC_whenParentSpecifiedAndTraversalAndFreeFormQuery() throws Exception {
        prepareSingleRootWithTwoLevelsOfChildren();
        IdMappingJob job =
                idMappingResultJobOp.createAndPutJobInCache(
                        this.maxToIdsWithFacetsAllowed - 1, JobStatus.FINISHED);

        mockMvc.perform(
                        MockMvcRequestBuilders.get(getUrlWithJobId(job.getJobId()))
                                .param("query", ORGANISM_ID_2)
                                .param("parent", EC_ID_0))
                .andDo(MockMvcResultHandlers.log())
                .andExpect(MockMvcResultMatchers.jsonPath("$.groups[0].id", is(EC_ID_2)))
                .andExpect(MockMvcResultMatchers.jsonPath("$.groups[0].label", is(EC_LABEL_2)))
                .andExpect(MockMvcResultMatchers.jsonPath("$.groups[0].expandable", is(false)))
                .andExpect(MockMvcResultMatchers.jsonPath("$.groups[0].count", is(1)))
                .andExpect(MockMvcResultMatchers.jsonPath("$.groups.size()", is(1)))
                .andExpect(MockMvcResultMatchers.jsonPath("$.ancestors[0].id", is(EC_ID_1)))
                .andExpect(MockMvcResultMatchers.jsonPath("$.ancestors[0].label", is(EC_LABEL_1)))
                .andExpect(MockMvcResultMatchers.jsonPath("$.ancestors.size()", is(1)))
                .andExpect(MockMvcResultMatchers.jsonPath("$.parent.label", is("ec_label_0")))
                .andExpect(MockMvcResultMatchers.jsonPath("$.parent.count", is(1)));
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
        uniProtDocument.chebi.add(CHEBI_ID);
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
