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
import static org.uniprot.api.idmapping.common.IdMappingUniRefITUtils.getUniRefFieldValueForValidatedField;
import static org.uniprot.api.idmapping.common.IdMappingUniRefITUtils.saveEntries;

import java.util.Collections;
import java.util.List;
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
import org.uniprot.api.idmapping.common.AbstractJobOperation;
import org.uniprot.api.idmapping.common.IdMappingDataStoreTestConfig;
import org.uniprot.api.idmapping.common.JobOperation;
import org.uniprot.api.idmapping.common.model.IdMappingJob;
import org.uniprot.api.rest.output.UniProtMediaType;
import org.uniprot.api.rest.respository.facet.impl.UniRefFacetConfig;
import org.uniprot.api.rest.service.RdfPrologs;
import org.uniprot.core.uniref.UniRefEntryLight;
import org.uniprot.store.config.UniProtDataType;
import org.uniprot.store.config.returnfield.factory.ReturnFieldConfigFactory;
import org.uniprot.store.datastore.UniProtStoreClient;
import org.uniprot.store.indexer.converters.UniRefDocumentConverter;
import org.uniprot.store.indexer.uniprot.mockers.TaxonomyRepoMocker;
import org.uniprot.store.search.SolrCollection;

/**
 * @author lgonzales
 * @since 26/02/2021
 */
@ActiveProfiles(profiles = {"offline", "idmapping"})
@ContextConfiguration(classes = {IdMappingDataStoreTestConfig.class, IdMappingREST.class})
@WebMvcTest(UniRefIdMappingResultsController.class)
@AutoConfigureWebClient
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class UniRefIdMappingResultsControllerIT extends AbstractIdMappingResultsControllerIT {

    private static final String UNIREF_ID_MAPPING_RESULT = "/idmapping/uniref/results/{jobId}";
    private static final String UNIREF_ID_MAPPING_STREAM_RESULT =
            "/idmapping/uniref/results/stream/{jobId}";

    private final UniRefDocumentConverter documentConverter =
            new UniRefDocumentConverter(TaxonomyRepoMocker.getTaxonomyRepo());

    @Autowired private UniRefFacetConfig facetConfig;

    @Autowired private UniProtStoreClient<UniRefEntryLight> storeClient;

    @Autowired private MockMvc mockMvc;

    @Qualifier("uniRefFacetTupleStreamTemplate")
    @Autowired
    private FacetTupleStreamTemplate facetTupleStreamTemplate;

    @Qualifier("uniRefTupleStreamTemplate")
    @Autowired
    private TupleStreamTemplate tupleStreamTemplate;

    @Autowired private JobOperation uniRefIdMappingJobOp;

    @MockBean(name = "idMappingRdfRestTemplate")
    private RestTemplate uniRefRestTemplate;

    @Override
    protected List<SolrCollection> getSolrCollections() {
        return List.of(SolrCollection.uniref);
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
        return UNIREF_ID_MAPPING_RESULT;
    }

    @Override
    protected UniProtDataType getUniProtDataType() {
        return UniProtDataType.UNIREF;
    }

    @Override
    protected FacetConfig getFacetConfig() {
        return facetConfig;
    }

    @Override
    protected JobOperation getJobOperation() {
        return uniRefIdMappingJobOp;
    }

    @Override
    protected String getFieldValueForValidatedField(String searchField) {
        return getUniRefFieldValueForValidatedField(searchField);
    }

    @Override
    protected Stream<Arguments> getAllReturnedFields() {
        return ReturnFieldConfigFactory.getReturnFieldConfig(UniProtDataType.UNIREF)
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

    @BeforeAll
    void saveEntriesStore() throws Exception {
        saveEntries(cloudSolrClient, storeClient);
    }

    @BeforeEach
    void setUp() {
        when(uniRefRestTemplate.getUriTemplateHandler()).thenReturn(new DefaultUriBuilderFactory());
        when(uniRefRestTemplate.getForObject(any(), any())).thenReturn(SAMPLE_RDF);
    }

    @Test
    void testIdMappingWithSuccess() throws Exception {
        // when
        IdMappingJob job = getJobOperation().createAndPutJobInCache(this.maxIdsWithFacets);
        ResultActions response =
                getMockMvc()
                        .perform(
                                get(getIdMappingResultPath(), job.getJobId())
                                        .param("query", "identity:0.5")
                                        .param("facets", "identity")
                                        .param("fields", "id,name,common_taxon,members")
                                        .param("sort", "id desc")
                                        .param("size", "6")
                                        .header(ACCEPT, APPLICATION_JSON_VALUE));

        // then
        response.andDo(log())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.facets.size()", is(1)))
                .andExpect(jsonPath("$.facets.*.name", contains("identity")))
                .andExpect(jsonPath("$.facets[0].values.size()", is(1)))
                .andExpect(jsonPath("$.facets[0].values.*.value", contains("0.5")))
                .andExpect(jsonPath("$.facets[0].values.*.count", contains(10)))
                .andExpect(jsonPath("$.results.size()", is(6)))
                .andExpect(
                        jsonPath(
                                "$.results.*.from",
                                contains(
                                        "Q00010", "Q00009", "Q00008", "Q00007", "Q00006",
                                        "Q00005")))
                .andExpect(
                        jsonPath(
                                "$.results.*.to.id",
                                contains(
                                        "UniRef50_P03910",
                                        "UniRef50_P03909",
                                        "UniRef50_P03908",
                                        "UniRef50_P03907",
                                        "UniRef50_P03906",
                                        "UniRef50_P03905")))
                .andExpect(
                        jsonPath(
                                "$.results[0].to.members",
                                contains(
                                        "P12310", "P32110", "P32102", "P32103", "P32104", "P32105",
                                        "P32106", "P32107", "P32108", "P32109")))
                .andExpect(jsonPath("$.results.*.to.commonTaxon").exists())
                .andExpect(jsonPath("$.results.*.to.sequence").doesNotExist());
    }

    @Test
    void testIdMappingWithInvalidCompleteValue() throws Exception {
        // when
        IdMappingJob job =
                getJobOperation().createAndPutJobInCache(AbstractJobOperation.DEFAULT_IDS_COUNT);
        ResultActions response =
                getMockMvc()
                        .perform(
                                get(getIdMappingResultPath(), job.getJobId())
                                        .param("query", "*")
                                        .param("complete", "INVALID")
                                        .header(ACCEPT, APPLICATION_JSON_VALUE));

        // then
        response.andDo(log())
                .andExpect(status().is(HttpStatus.BAD_REQUEST.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.url", not(emptyOrNullString())))
                .andExpect(
                        jsonPath(
                                "$.messages",
                                contains(
                                        "'complete' parameter value has invalid format. It should be true or false.")));
    }

    @Test
    void testIdMappingWithSuccessComplete() throws Exception {
        // when
        IdMappingJob job =
                getJobOperation().createAndPutJobInCache(AbstractJobOperation.DEFAULT_IDS_COUNT);
        ResultActions response =
                getMockMvc()
                        .perform(
                                get(getIdMappingResultPath(), job.getJobId())
                                        .param("query", "*")
                                        .param("complete", "true")
                                        .param("sort", "id desc")
                                        .param("size", "5")
                                        .header(ACCEPT, APPLICATION_JSON_VALUE));

        // then
        response.andDo(log())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.results.size()", is(5)))
                .andExpect(
                        jsonPath(
                                "$.results.*.from",
                                contains("Q00020", "Q00019", "Q00018", "Q00017", "Q00016")))
                .andExpect(
                        jsonPath(
                                "$.results.*.to.id",
                                contains(
                                        "UniRef50_P03920",
                                        "UniRef50_P03919",
                                        "UniRef50_P03918",
                                        "UniRef50_P03917",
                                        "UniRef50_P03916")))
                .andExpect(
                        jsonPath(
                                "$.results[0].to.members",
                                contains(
                                        "P12320", "P32120", "P32102", "P32103", "P32104", "P32105",
                                        "P32106", "P32107", "P32108", "P32109", "P32110", "P32111",
                                        "P32112", "P32113", "P32114", "P32115", "P32116", "P32117",
                                        "P32118", "P32119")))
                .andExpect(
                        jsonPath(
                                "$.results[0].to.organisms.*.taxonId",
                                contains(
                                        9600, 9626, 9608, 9609, 9610, 9611, 9612, 9613, 9614, 9615,
                                        9616, 9617, 9618, 9619, 9620, 9621, 9622, 9623, 9624,
                                        9625)))
                .andExpect(jsonPath("$.results.*.to.commonTaxon").exists())
                .andExpect(jsonPath("$.results.*.to.representativeMember").exists());
    }

    @Test
    void testIdMappingWithSuccessNotComplete() throws Exception {
        // when
        IdMappingJob job =
                getJobOperation().createAndPutJobInCache(AbstractJobOperation.DEFAULT_IDS_COUNT);
        ResultActions response =
                getMockMvc()
                        .perform(
                                get(getIdMappingResultPath(), job.getJobId())
                                        .param("query", "*")
                                        .param("complete", "false")
                                        .param("sort", "id desc")
                                        .param("size", "5")
                                        .header(ACCEPT, APPLICATION_JSON_VALUE));

        // then
        response.andDo(log())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.results.size()", is(5)))
                .andExpect(
                        jsonPath(
                                "$.results.*.from",
                                contains("Q00020", "Q00019", "Q00018", "Q00017", "Q00016")))
                .andExpect(
                        jsonPath(
                                "$.results.*.to.id",
                                contains(
                                        "UniRef50_P03920",
                                        "UniRef50_P03919",
                                        "UniRef50_P03918",
                                        "UniRef50_P03917",
                                        "UniRef50_P03916")))
                .andExpect(
                        jsonPath(
                                "$.results[0].to.members",
                                contains(
                                        "P12320", "P32120", "P32102", "P32103", "P32104", "P32105",
                                        "P32106", "P32107", "P32108", "P32109")))
                .andExpect(
                        jsonPath(
                                "$.results[0].to.organisms.*.taxonId",
                                contains(
                                        9600, 9626, 9608, 9609, 9610, 9611, 9612, 9613, 9614,
                                        9615)))
                .andExpect(jsonPath("$.results.*.to.commonTaxon").exists())
                .andExpect(jsonPath("$.results.*.to.representativeMember").exists());
    }

    @Test
    void streamRdfCanReturnSuccess() throws Exception {
        // when
        IdMappingJob job = getJobOperation().createAndPutJobInCache();
        ;
        MockHttpServletRequestBuilder requestBuilder =
                get(UNIREF_ID_MAPPING_STREAM_RESULT, job.getJobId())
                        .header(ACCEPT, UniProtMediaType.RDF_MEDIA_TYPE);

        MvcResult response = mockMvc.perform(requestBuilder).andReturn();

        // then
        mockMvc.perform(asyncDispatch(response))
                .andDo(log())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().doesNotExist("Content-Disposition"))
                .andExpect(content().string(startsWith(RdfPrologs.UNIREF_PROLOG)))
                .andExpect(
                        content()
                                .string(
                                        containsString(
                                                "    <sample>text</sample>\n"
                                                        + "    <anotherSample>text2</anotherSample>\n"
                                                        + "    <someMore>text3</someMore>\n\n"
                                                        + "</rdf:RDF>")));
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
                                                "From\tCluster ID\tCluster Name\tCommon taxon\tSize\tDate of creation")))
                .andExpect(
                        content()
                                .string(
                                        containsString(
                                                "Q00001\tUniRef50_P03901\tCluster: MoeK5 01\tHomo sapiens\t1\t2019-08-27\n"
                                                        + "Q00002\tUniRef50_P03902\tCluster: MoeK5 02\tHomo sapiens\t2\t2019-08-27\n"
                                                        + "Q00003\tUniRef50_P03903\tCluster: MoeK5 03\tHomo sapiens\t3\t2019-08-27\n"
                                                        + "Q00004\tUniRef50_P03904\tCluster: MoeK5 04\tHomo sapiens\t4\t2019-08-27\n"
                                                        + "Q00005\tUniRef50_P03905\tCluster: MoeK5 05\tHomo sapiens\t5\t2019-08-27")));
    }

    @Override
    protected String getDefaultSearchQuery() {
        return "Homo sapiens";
    }
}
