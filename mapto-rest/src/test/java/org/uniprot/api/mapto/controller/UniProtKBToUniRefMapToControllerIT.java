package org.uniprot.api.mapto.controller;

import com.jayway.jsonpath.JsonPath;
import org.apache.solr.client.solrj.SolrClient;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.autoconfigure.web.client.AutoConfigureWebClient;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.uniprot.api.common.repository.solrstream.FacetTupleStreamTemplate;
import org.uniprot.api.common.repository.stream.common.TupleStreamTemplate;
import org.uniprot.api.common.repository.stream.store.uniprotkb.TaxonomyLineageRepository;
import org.uniprot.api.idmapping.common.service.TestConfig;
import org.uniprot.api.mapto.MapToREST;
import org.uniprot.api.mapto.common.RedisConfiguration;
import org.uniprot.api.rest.validation.error.ErrorHandlerConfig;
import org.uniprot.api.uniprotkb.common.repository.UniProtKBDataStoreTestConfig;
import org.uniprot.api.uniprotkb.common.repository.search.UniprotQueryRepository;
import org.uniprot.api.uniprotkb.common.utils.UniProtKBAsyncDownloadUtils;
import org.uniprot.api.uniref.common.repository.UniRefDataStoreTestConfig;
import org.uniprot.api.uniref.common.repository.search.UniRefQueryRepository;
import org.uniprot.api.uniref.common.util.UniRefAsyncDownloadUtils;
import org.uniprot.core.uniprotkb.UniProtKBEntry;
import org.uniprot.core.uniref.UniRefEntryLight;
import org.uniprot.store.datastore.UniProtStoreClient;
import org.uniprot.store.search.SolrCollection;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.http.HttpHeaders.ACCEPT;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.log;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.testcontainers.shaded.org.awaitility.Awaitility.await;
import static org.uniprot.api.mapto.controller.UniProtKBUniRefMapToController.UNIPROTKB_UNIREF;
import static org.uniprot.store.search.SolrCollection.*;

@ActiveProfiles(profiles = {"offline", "idmapping"})
@WebMvcTest({UniProtKBUniRefMapToController.class})
@ContextConfiguration(
        classes = {
            TestConfig.class,
            UniRefDataStoreTestConfig.class,
            UniProtKBDataStoreTestConfig.class,
            MapToREST.class,
            ErrorHandlerConfig.class,
            RedisConfiguration.class
        })
@ExtendWith(SpringExtension.class)
@AutoConfigureWebClient
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class UniProtKBToUniRefMapToControllerIT extends MapToControllerIT {

    @Autowired private UniprotQueryRepository uniprotQueryRepository;
    @Autowired private UniRefQueryRepository uniRefQueryRepository;
    @Autowired private TaxonomyLineageRepository taxRepository;

    @Autowired
    @Qualifier("uniProtKBTupleStream")
    private TupleStreamTemplate uniProtKBTupleStreamTemplate;

    @Qualifier("uniProtKBFacetTupleStreamTemplate")
    @Autowired
    private FacetTupleStreamTemplate uniProtKBFacetTupleStreamTemplate;

    @Qualifier("uniRefFacetTupleStreamTemplate")
    @Autowired
    private FacetTupleStreamTemplate unirefFacetTupleStreamTemplate;

    @Autowired
    @Qualifier("uniRefTupleStreamTemplate")
    private TupleStreamTemplate unirefTupleStreamTemplate;

    @Autowired
    @Qualifier("uniProtKBSolrClient")
    private SolrClient uniProtKBSolrClient;

    @Qualifier("uniProtStoreClient")
    @Autowired
    private UniProtStoreClient<UniProtKBEntry> uniProtKBStoreClient;

    @Qualifier("uniRefLightStoreClient")
    @Autowired
    private UniProtStoreClient<UniRefEntryLight> uniRefStoreClient;

    @BeforeAll
    public void runSaveEntriesInSolrAndStore() throws Exception {
        UniProtKBAsyncDownloadUtils.saveEntriesInSolrAndStore(
                uniprotQueryRepository,
                cloudSolrClient,
                uniProtKBSolrClient,
                uniProtKBStoreClient,
                taxRepository);
        UniProtKBAsyncDownloadUtils.saveEntries(cloudSolrClient, uniProtKBStoreClient, 20);
        UniRefAsyncDownloadUtils.saveEntriesInSolrAndStore(
                uniRefQueryRepository, cloudSolrClient, solrClient, uniRefStoreClient, 20, "P");
    }

    @Test
    void submitJobAndVerifyGetResultWithFacets() throws Exception {
        // when
        String query = "accession:(P00001  OR P00002 OR P00003)";
        String jobId = callRunAPIAndVerify(query, false);
        await().until(() -> mapToJobRepository.existsById(jobId));
        await().until(isJobFinished(jobId));
        String jobResultsUrl = getDownloadAPIsBasePath() + "/results/{jobId}";
        MockHttpServletRequestBuilder requestBuilder =
                get(jobResultsUrl, jobId)
                        .header(ACCEPT, APPLICATION_JSON_VALUE)
                        .param("query", "*:*")
                        .param("facets", "identity")
                        .param("size", "0");
        ResultActions response = mockMvc.perform(requestBuilder);
        // then
        response.andDo(log())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.facets").exists())
                .andExpect(jsonPath("$.facets.size()", is(1)))
                .andExpect(jsonPath("$.facets[0].label", equalTo("Clusters")))
                .andExpect(jsonPath("$.facets[0].name", equalTo("identity")))
                .andExpect(jsonPath("$.facets[0].allowMultipleSelection", is(true)))
                .andExpect(jsonPath("$.facets[0].values.size()", is(3)))
                // Verify first value (100%)
                .andExpect(jsonPath("$.facets[0].values[0].label", is("100%")))
                .andExpect(jsonPath("$.facets[0].values[0].value", is("1.0")))
                .andExpect(jsonPath("$.facets[0].values[0].count", is(3)))
                // Verify second value (90%)
                .andExpect(jsonPath("$.facets[0].values[1].label", is("90%")))
                .andExpect(jsonPath("$.facets[0].values[1].value", is("0.9")))
                .andExpect(jsonPath("$.facets[0].values[1].count", is(3)))
                // Verify third value (50%)
                .andExpect(jsonPath("$.facets[0].values[2].label", is("50%")))
                .andExpect(jsonPath("$.facets[0].values[2].value", is("0.5")))
                .andExpect(jsonPath("$.facets[0].values[2].count", is(3)))
                .andExpect(jsonPath("$.results.size()", is(0)));
    }

    @Override
    protected Map<String, String> getSortQuery() {
        return Map.of("query", "*:*", "sort", "id desc");
    }

    @Override
    protected Map<String, String> getFilterQuery() {
        return Map.of("query", "id:UniRef90_P00003");
    }

    @Override
    protected String getQueryInLimits() {
        return "accession:(P00001  OR P00002 OR P00003 OR P00004)";
    }

    @Override
    protected void verifyResultsWithLimit(String resultsJson) {
        List<String> organisms = JsonPath.read(resultsJson, "$.results.*.id");
        assertTrue(
                organisms.containsAll(
                        List.of(
                                "UniRef100_P00001",
                                "UniRef100_P00002",
                                "UniRef100_P00003",
                                "UniRef100_P00004",
                                "UniRef50_P00001")));
        assertEquals(5, organisms.size());
        String accession =
                JsonPath.read(resultsJson, "$.results[4].representativeMember.accessions[0]");
        assertEquals("P12301", accession);
        String memberId = JsonPath.read(resultsJson, "$.results[4].representativeMember.memberId");
        assertEquals("P12301_HUMAN", memberId);
    }

    @Override
    protected String getPath() {
        return UNIPROTKB_UNIREF;
    }

    @Override
    protected void verifyResultsWithSort(String resultsJson) {
        List<String> uniRefIds = JsonPath.read(resultsJson, "$.results.*.id");
        List<String> expected =
                List.of(
                        "UniRef90_P00004",
                        "UniRef90_P00003",
                        "UniRef90_P00002",
                        "UniRef90_P00001",
                        "UniRef50_P00004",
                        "UniRef50_P00003",
                        "UniRef50_P00002",
                        "UniRef50_P00001",
                        "UniRef100_P00004",
                        "UniRef100_P00003",
                        "UniRef100_P00002",
                        "UniRef100_P00001");
        assertEquals(new LinkedList<>(uniRefIds), new LinkedList<>(expected));
        assertEquals(12, uniRefIds.size());
        String accession =
                JsonPath.read(resultsJson, "$.results[11].representativeMember.accessions[0]");
        assertEquals("P12301", accession);
        String memberId = JsonPath.read(resultsJson, "$.results[11].representativeMember.memberId");
        assertEquals("P12301_HUMAN", memberId);
    }

    @Override
    protected void verifyResultsWithFilter(String resultsJson) {
        List<String> organisms = JsonPath.read(resultsJson, "$.results.*.id");
        List<String> expected = List.of("UniRef90_P00003");
        assertEquals(new LinkedList<>(expected), new LinkedList<>(organisms));
        assertEquals(1, organisms.size());
        String accession =
                JsonPath.read(resultsJson, "$.results[0].representativeMember.accessions[0]");
        assertEquals("P12303", accession);
        String memberId = JsonPath.read(resultsJson, "$.results[0].representativeMember.memberId");
        assertEquals("P12303_HUMAN", memberId);
    }

    @Override
    protected void verifyResults(String resultsJson) {
        List<String> organisms = JsonPath.read(resultsJson, "$.results.*.id");
        assertTrue(
                organisms.containsAll(
                        List.of(
                                "UniRef100_P00001",
                                "UniRef100_P00002",
                                "UniRef100_P00003",
                                "UniRef100_P00004",
                                "UniRef90_P00001",
                                "UniRef90_P00002",
                                "UniRef90_P00003",
                                "UniRef90_P00004",
                                "UniRef50_P00001",
                                "UniRef50_P00002",
                                "UniRef50_P00003",
                                "UniRef50_P00004")));
        assertEquals(12, organisms.size());
        String accession =
                JsonPath.read(resultsJson, "$.results[0].representativeMember.accessions[0]");
        assertEquals("P12301", accession);
        String memberId = JsonPath.read(resultsJson, "$.results[0].representativeMember.memberId");
        assertEquals("P12301_HUMAN", memberId);
    }

    @Override
    protected String getDownloadAPIsBasePath() {
        return UniProtKBUniRefMapToController.RESOURCE_PATH;
    }

    @Override
    protected List<SolrCollection> getSolrCollections() {
        return List.of(uniprot, uniref, taxonomy);
    }

    @Override
    protected Collection<TupleStreamTemplate> getTupleStreamTemplates() {
        return List.of(uniProtKBTupleStreamTemplate, unirefTupleStreamTemplate);
    }

    @Override
    protected Collection<FacetTupleStreamTemplate> getFacetTupleStreamTemplates() {
        return List.of(uniProtKBFacetTupleStreamTemplate, unirefFacetTupleStreamTemplate);
    }

    @Override
    protected int getTotalEntries() {
        return 12;
    }
}
