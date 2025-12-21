package org.uniprot.api.idmapping.controller;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpHeaders.ACCEPT;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.log;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.uniprot.api.idmapping.common.IdMappingUniParcITUtils.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.provider.Arguments;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.autoconfigure.web.client.AutoConfigureWebClient;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.DefaultUriBuilderFactory;
import org.uniprot.api.common.repository.search.facet.FacetConfig;
import org.uniprot.api.common.repository.solrstream.FacetTupleStreamTemplate;
import org.uniprot.api.common.repository.stream.common.TupleStreamTemplate;
import org.uniprot.api.idmapping.IdMappingREST;
import org.uniprot.api.idmapping.common.IdMappingDataStoreTestConfig;
import org.uniprot.api.idmapping.common.JobOperation;
import org.uniprot.api.idmapping.common.UniParcIdMappingResultsJobOperation;
import org.uniprot.api.idmapping.common.model.IdMappingJob;
import org.uniprot.api.idmapping.common.model.IdMappingResult;
import org.uniprot.api.idmapping.common.request.IdMappingJobRequest;
import org.uniprot.api.idmapping.common.response.model.IdMappingStringPair;
import org.uniprot.api.idmapping.common.service.IdMappingJobCacheService;
import org.uniprot.api.rest.download.model.JobStatus;
import org.uniprot.api.rest.output.UniProtMediaType;
import org.uniprot.api.rest.respository.facet.impl.UniParcFacetConfig;
import org.uniprot.core.uniparc.UniParcEntryLight;
import org.uniprot.core.uniparc.impl.UniParcCrossReferencePair;
import org.uniprot.store.config.UniProtDataType;
import org.uniprot.store.config.idmapping.IdMappingFieldConfig;
import org.uniprot.store.config.returnfield.factory.ReturnFieldConfigFactory;
import org.uniprot.store.datastore.UniProtStoreClient;
import org.uniprot.store.search.SolrCollection;

/**
 * @author lgonzales
 * @since 26/02/2021
 */
@ActiveProfiles(profiles = {"offline", "idmapping"})
@ContextConfiguration(classes = {IdMappingDataStoreTestConfig.class, IdMappingREST.class})
@WebMvcTest(UniParcIdMappingResultsController.class)
@AutoConfigureWebClient
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class UniParcIdMappingResultsControllerIT extends AbstractIdMappingResultsControllerIT {

    private static final String UNIPARC_ID_MAPPING_RESULT = "/idmapping/uniparc/results/{jobId}";
    private static final String UNIPARC_ID_MAPPING__STREAM_RESULT =
            "/idmapping/uniparc/results/stream/{jobId}";

    @Autowired private UniParcFacetConfig facetConfig;

    @Autowired private UniProtStoreClient<UniParcEntryLight> storeClient;

    @Autowired UniProtStoreClient<UniParcCrossReferencePair> xrefStoreClient;

    @Autowired private MockMvc mockMvc;

    @Qualifier("uniParcFacetTupleStreamTemplate")
    @Autowired
    private FacetTupleStreamTemplate facetTupleStreamTemplate;

    @Qualifier("uniParcTupleStreamTemplate")
    @Autowired
    private TupleStreamTemplate tupleStreamTemplate;

    @Autowired private JobOperation uniParcIdMappingJobOp;

    @MockBean(name = "idMappingRdfRestTemplate")
    private RestTemplate uniParcRestTemplate;

    @Override
    protected List<SolrCollection> getSolrCollections() {
        return List.of(SolrCollection.uniparc);
    }

    @Override
    protected TupleStreamTemplate getTupleStreamTemplate() {
        return tupleStreamTemplate;
    }

    @Override
    protected FacetTupleStreamTemplate getFacetTupleStreamTemplate() {
        return facetTupleStreamTemplate;
    }

    @Override
    protected MockMvc getMockMvc() {
        return mockMvc;
    }

    @Override
    protected String getIdMappingResultPath() {
        return UNIPARC_ID_MAPPING_RESULT;
    }

    @Override
    protected UniProtDataType getUniProtDataType() {
        return UniProtDataType.UNIPARC;
    }

    @Override
    protected FacetConfig getFacetConfig() {
        return facetConfig;
    }

    @Override
    protected JobOperation getJobOperation() {
        return uniParcIdMappingJobOp;
    }

    @Override
    protected String getFieldValueForValidatedField(String fieldName) {
        return getUniParcFieldValueForValidatedField(fieldName);
    }

    @BeforeAll
    void saveEntriesStore() throws Exception {
        saveEntries(cloudSolrClient, storeClient, xrefStoreClient);
    }

    @BeforeEach
    void setUp() {
        when(uniParcRestTemplate.getUriTemplateHandler())
                .thenReturn(new DefaultUriBuilderFactory());
        when(uniParcRestTemplate.getForObject(any(), any())).thenReturn(SAMPLE_RDF);
    }

    @Test
    void testIdMappingWithSuccess() throws Exception {
        // when
        IdMappingJob job = getJobOperation().createAndPutJobInCache(this.maxIdsWithFacets);
        ResultActions response =
                getMockMvc()
                        .perform(
                                get(getIdMappingResultPath(), job.getJobId())
                                        .param("query", "database:EnsemblMetazoa")
                                        .param("facets", "organism_name,database_facet")
                                        .param("fields", "upi,accession,gene")
                                        .param("sort", "length desc")
                                        .param("size", "10")
                                        .header(ACCEPT, APPLICATION_JSON_VALUE));

        // then
        response.andDo(log())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.facets.size()", is(2)))
                .andExpect(jsonPath("$.facets.*.name", contains("organism_name", "database_facet")))
                .andExpect(jsonPath("$.facets[0].values.size()", is(2)))
                .andExpect(
                        jsonPath(
                                "$.facets[0].values.*.value",
                                contains("Homo sapiens", "Torpedo californica")))
                .andExpect(jsonPath("$.facets[0].values.*.count", contains(3, 3)))
                .andExpect(jsonPath("$.results.size()", is(3)))
                .andExpect(jsonPath("$.results.*.from", contains("Q00009", "Q00006", "Q00003")))
                .andExpect(
                        jsonPath(
                                "$.results.*.to.uniParcId",
                                contains("UPI0000283A09", "UPI0000283A06", "UPI0000283A03")))
                .andExpect(jsonPath("$.results.*.to.uniProtKBAccessions").exists())
                .andExpect(jsonPath("$.results.*.to.oldestCrossRefCreated").exists())
                .andExpect(jsonPath("$.results.*.to.mostRecentCrossRefUpdated").exists())
                .andExpect(jsonPath("$.results.*.to.geneNames").exists());
    }

    @Test
    void streamRdfCanReturnSuccess() throws Exception {
        // when
        IdMappingJob job = getJobOperation().createAndPutJobInCache();

        MockHttpServletRequestBuilder requestBuilder =
                get(UNIPARC_ID_MAPPING__STREAM_RESULT, job.getJobId())
                        .header(ACCEPT, UniProtMediaType.RDF_MEDIA_TYPE);

        MvcResult response = mockMvc.perform(requestBuilder).andReturn();

        // then
        mockMvc.perform(asyncDispatch(response))
                .andDo(log())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().doesNotExist("Content-Disposition"))
                .andExpect(content().string(equalTo(SAMPLE_RDF)));
    }

    @Test
    void testGetResultsInTSV() throws Exception {
        // when
        MediaType mediaType = UniProtMediaType.TSV_MEDIA_TYPE;
        IdMappingJob job = getJobOperation().createAndPutJobInCache();
        MockHttpServletRequestBuilder requestBuilder =
                get(getIdMappingResultPath(), job.getJobId()).header(ACCEPT, mediaType);

        ResultActions response = getMockMvc().perform(requestBuilder);

        // then
        response.andDo(log())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, mediaType.toString()))
                .andExpect(content().contentTypeCompatibleWith(mediaType))
                .andExpect(
                        content()
                                .string(
                                        containsString(
                                                "From\tEntry\tOrganisms\tUniProtKB\tFirst seen\tLast seen\tLength")))
                .andExpect(
                        content()
                                .string(
                                        containsString(
                                                "Q00001\tUPI0000283A01\tName 7787; Name 9606\tP10001; P12301\t2017-02-12\t2017-04-23\t11")))
                .andExpect(
                        content()
                                .string(
                                        containsString(
                                                "Q00002\tUPI0000283A02\tName 7787; Name 9606\tP10002; P12302\t2017-02-12\t2017-04-23\t12")))
                .andExpect(
                        content()
                                .string(
                                        containsString(
                                                "Q00003\tUPI0000283A03\tName 7787; Name 9606\tP10003; P12303\t2017-02-12")))
                .andExpect(
                        content()
                                .string(
                                        containsString(
                                                "Q00004\tUPI0000283A04\tName 7787; Name 9606\tP10004; P12304\t2017-02-12\t2017-04-23\t14")))
                .andExpect(
                        content()
                                .string(
                                        containsString(
                                                "Q00005\tUPI0000283A05\tName 7787; Name 9606\tP10005; P12305\t2017-02-12\t2017-04-23\t15")));
    }

    @Test
    void testIdMappingFromProteomeToUniParc() throws Exception {
        // given
        UniParcIdMappingResultsJobOperation jobOperation =
                (UniParcIdMappingResultsJobOperation) getJobOperation();
        List<IdMappingStringPair> pairs = new ArrayList<>();
        List<String> proteomeIds = new ArrayList<>();
        for (int i = 1; i < 10; i++) {
            String proteomeId = String.format("UP%09d", i);
            String uniParcId1 = String.format(UPI_PREF + "%02d", i);
            String uniParcId2 = String.format(UPI_PREF + "%02d", i + 1);
            pairs.add(new IdMappingStringPair(proteomeId, uniParcId1));
            pairs.add(new IdMappingStringPair(proteomeId, uniParcId2));
            proteomeIds.add(proteomeId);
        }
        String jobId = UUID.randomUUID().toString();
        IdMappingJobRequest request =
                jobOperation.createRequest(
                        IdMappingFieldConfig.PROTEOME_STR,
                        IdMappingFieldConfig.UNIPARC_STR,
                        String.join(",", proteomeIds));
        IdMappingResult result = IdMappingResult.builder().mappedIds(pairs).build();
        JobStatus jobStatus = JobStatus.FINISHED;
        IdMappingJob job = jobOperation.createJob(jobId, request, result, jobStatus);
        IdMappingJobCacheService cacheService = jobOperation.getIdMappingJobCacheService();

        if (!cacheService.exists(jobId)) {
            cacheService.put(jobId, job); // put the finished job in cache
        }
        // when
        ResultActions response =
                mockMvc.perform(
                        get(getIdMappingResultPath(), jobId)
                                .header(ACCEPT, APPLICATION_JSON_VALUE));
        // then
        response.andDo(log())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.results.size()", is(5)))
                .andExpect(
                        jsonPath(
                                "$.results.*.from",
                                containsInAnyOrder(
                                        "UP000000001",
                                        "UP000000001",
                                        "UP000000002",
                                        "UP000000002",
                                        "UP000000003")))
                .andExpect(
                        jsonPath(
                                "$.results.*.to.uniParcId",
                                containsInAnyOrder(
                                        "UPI0000283A01",
                                        "UPI0000283A02",
                                        "UPI0000283A02",
                                        "UPI0000283A03",
                                        "UPI0000283A03")));
    }

    @Override
    protected String getDefaultSearchQuery() {
        return "9606";
    }

    @Override
    protected Stream<Arguments> getAllReturnedFields() {
        return ReturnFieldConfigFactory.getReturnFieldConfig(getUniProtDataType())
                .getReturnFields()
                .stream()
                .map(
                        returnField -> {
                            String lightPath =
                                    returnField.getPaths().get(returnField.getPaths().size() - 1);
                            return Arguments.of(
                                    returnField.getName(), Collections.singletonList(lightPath));
                        });
    }
}
