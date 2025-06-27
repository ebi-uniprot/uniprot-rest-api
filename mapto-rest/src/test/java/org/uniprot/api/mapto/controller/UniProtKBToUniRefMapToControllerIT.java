package org.uniprot.api.mapto.controller;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpHeaders.ACCEPT;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.log;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.testcontainers.shaded.org.awaitility.Awaitility.await;
import static org.uniprot.api.rest.controller.AbstractStreamControllerIT.SAMPLE_RDF;
import static org.uniprot.store.search.SolrCollection.*;

import java.util.*;

import org.apache.solr.client.solrj.SolrClient;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.autoconfigure.web.client.AutoConfigureWebClient;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.DefaultUriBuilderFactory;
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

import com.jayway.jsonpath.JsonPath;

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
class UniProtKBToUniRefMapToControllerIT extends AbstractMapToControllerIT {

    @SpyBean private UniprotQueryRepository uniprotQueryRepository;
    @Autowired private UniRefQueryRepository uniRefQueryRepository;
    @Autowired private TaxonomyLineageRepository taxRepository;
    @Autowired protected MockMvc mockMvc;

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

    @MockBean(name = "uniRefRdfRestTemplate")
    private RestTemplate uniRefRdfRestTemplate;

    @BeforeEach
    void setUp() {
        when(uniRefRdfRestTemplate.getUriTemplateHandler())
                .thenReturn(new DefaultUriBuilderFactory());
        when(uniRefRdfRestTemplate.getForObject(any(), any())).thenReturn(SAMPLE_RDF);
    }

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
        String jobId = callRunAPIAndVerify(query);
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

    @Test
    void submitJobAndVerifyGetResultWithFacetsAndResults() throws Exception {
        // when
        String query = "accession:(P00001  OR P00002 OR P00003)";
        String jobId = callRunAPIAndVerify(query);
        await().until(() -> mapToJobRepository.existsById(jobId));
        await().until(isJobFinished(jobId));
        String jobResultsUrl = getDownloadAPIsBasePath() + "/results/{jobId}";
        MockHttpServletRequestBuilder requestBuilder =
                get(jobResultsUrl, jobId)
                        .header(ACCEPT, APPLICATION_JSON_VALUE)
                        .param("query", "*:*")
                        .param("facets", "identity");
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
                .andExpect(jsonPath("$.results.size()", is(9)))
                .andExpect(
                        jsonPath(
                                "$.results.*.id",
                                contains(
                                        "UniRef100_P00001",
                                        "UniRef100_P00002",
                                        "UniRef100_P00003",
                                        "UniRef50_P00001",
                                        "UniRef50_P00002",
                                        "UniRef50_P00003",
                                        "UniRef90_P00001",
                                        "UniRef90_P00002",
                                        "UniRef90_P00003")));
    }

    @Test
    void submitJobAndVerifyGetResultWithEmptyResult() throws Exception {
        // when
        String randomString = UUID.randomUUID().toString();
        String query = randomString;
        String jobId = callRunAPIAndVerify(query);
        await().until(() -> mapToJobRepository.existsById(jobId));
        await().until(isJobFinished(jobId));
        String jobResultsUrl = getDownloadAPIsBasePath() + "/results/{jobId}";
        MockHttpServletRequestBuilder requestBuilder =
                get(jobResultsUrl, jobId)
                        .header(ACCEPT, APPLICATION_JSON_VALUE)
                        .param("query", "*:*");
        ResultActions response = mockMvc.perform(requestBuilder);
        // then
        response.andDo(log())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
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
    protected MockMvc getMockMvc() {
        return this.mockMvc;
    }

    @Override
    protected String getQueryLessThanPageSize() {
        return "accession:(P00001  OR P00002 OR P00003)";
    }

    @Override
    protected String getQueryBeyondEnrichmentLimits() {
        return "accession:(P00001  OR P00002 OR P00003 OR P00004 OR P00005)";
    }

    @Override
    protected void verifyResultsWithSize(String resultsJson) {
        List<String> uniRefIds = JsonPath.read(resultsJson, "$.results.*.id");
        assertTrue(
                uniRefIds.containsAll(
                        List.of(
                                "UniRef100_P00001",
                                "UniRef100_P00002",
                                "UniRef100_P00003",
                                "UniRef100_P00004",
                                "UniRef50_P00001")));
        assertEquals(5, uniRefIds.size());
        String accession =
                JsonPath.read(resultsJson, "$.results[4].representativeMember.accessions[0]");
        assertEquals("P12301", accession);
        String memberId = JsonPath.read(resultsJson, "$.results[4].representativeMember.memberId");
        assertEquals("P12301_HUMAN", memberId);
    }

    @Override
    protected void verifyResultsWithSort(String resultsJson) {
        List<String> uniRefIds = JsonPath.read(resultsJson, "$.results.*.id");
        List<String> expected =
                List.of(
                        "UniRef90_P00003",
                        "UniRef90_P00002",
                        "UniRef90_P00001",
                        "UniRef50_P00003",
                        "UniRef50_P00002",
                        "UniRef50_P00001",
                        "UniRef100_P00003",
                        "UniRef100_P00002",
                        "UniRef100_P00001");
        assertEquals(new LinkedList<>(expected), new LinkedList<>(uniRefIds));
        assertEquals(9, uniRefIds.size());
        String accession =
                JsonPath.read(resultsJson, "$.results[8].representativeMember.accessions[0]");
        assertEquals("P12301", accession);
        String memberId = JsonPath.read(resultsJson, "$.results[8].representativeMember.memberId");
        assertEquals("P12301_HUMAN", memberId);
    }

    @Override
    protected void verifyResultsWithPaginationPageOne(String resultsJson) {
        List<String> uniRefIds = JsonPath.read(resultsJson, "$.results.*.id");
        List<String> expected =
                List.of(
                        "UniRef100_P00001",
                        "UniRef100_P00002",
                        "UniRef100_P00003",
                        "UniRef100_P00004",
                        "UniRef50_P00001",
                        "UniRef50_P00002",
                        "UniRef50_P00003",
                        "UniRef50_P00004",
                        "UniRef90_P00001",
                        "UniRef90_P00002");
        assertEquals(new LinkedList<>(expected), new LinkedList<>(uniRefIds));
        assertEquals(10, uniRefIds.size());
        String accession =
                JsonPath.read(resultsJson, "$.results[0].representativeMember.accessions[0]");
        assertEquals("P12301", accession);
        String memberId = JsonPath.read(resultsJson, "$.results[0].representativeMember.memberId");
        assertEquals("P12301_HUMAN", memberId);
    }

    @Override
    protected void verifyResultsWithPaginationPageTwo(String resultsJson) {
        List<String> uniRefIds = JsonPath.read(resultsJson, "$.results.*.id");
        List<String> expected = List.of("UniRef90_P00003", "UniRef90_P00004");
        assertEquals(new LinkedList<>(expected), new LinkedList<>(uniRefIds));
        assertEquals(2, uniRefIds.size());
    }

    @Override
    protected void verifyResultsWithFilter(String resultsJson) {
        List<String> uniRefIds = JsonPath.read(resultsJson, "$.results.*.id");
        List<String> expected = List.of("UniRef90_P00003");
        assertEquals(new LinkedList<>(expected), new LinkedList<>(uniRefIds));
        assertEquals(1, uniRefIds.size());
        String accession =
                JsonPath.read(resultsJson, "$.results[0].representativeMember.accessions[0]");
        assertEquals("P12303", accession);
        String memberId = JsonPath.read(resultsJson, "$.results[0].representativeMember.memberId");
        assertEquals("P12303_HUMAN", memberId);
    }

    @Override
    protected void verifyResultsStream(String resultsJson) {
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

    @Override
    protected String getFacets() {
        return "identity";
    }

    @Override
    protected void mockServerError() {
        doThrow(new RuntimeException(SERVER_ERROR))
                .when(uniprotQueryRepository)
                .searchPage(any(), any());
    }
}
