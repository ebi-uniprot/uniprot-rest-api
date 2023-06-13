package org.uniprot.api.idmapping.controller;

import static org.hamcrest.Matchers.*;
import static org.hamcrest.Matchers.contains;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpHeaders.ACCEPT;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.log;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.uniprot.api.idmapping.controller.utils.IdMappingUniRefITUtils.getUniRefFieldValueForValidatedField;
import static org.uniprot.api.idmapping.controller.utils.IdMappingUniRefITUtils.saveEntries;

import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeAll;
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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.DefaultUriBuilderFactory;
import org.uniprot.api.common.repository.solrstream.FacetTupleStreamTemplate;
import org.uniprot.api.common.repository.stream.common.TupleStreamTemplate;
import org.uniprot.api.idmapping.IdMappingREST;
import org.uniprot.api.idmapping.controller.utils.DataStoreTestConfig;
import org.uniprot.api.idmapping.controller.utils.JobOperation;
import org.uniprot.api.idmapping.model.IdMappingJob;
import org.uniprot.core.uniref.UniRefEntryLight;
import org.uniprot.store.config.UniProtDataType;
import org.uniprot.store.config.returnfield.factory.ReturnFieldConfigFactory;
import org.uniprot.store.datastore.UniProtStoreClient;
import org.uniprot.store.search.SolrCollection;

/**
 * @author lgonzales
 * @since 08/03/2021
 */
@ActiveProfiles(profiles = {"offline", "idmapping"})
@ContextConfiguration(classes = {DataStoreTestConfig.class, IdMappingREST.class})
@WebMvcTest(UniRefIdMappingResultsController.class)
@AutoConfigureWebClient
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class UniRefIdMappingStreamControllerIT extends AbstractIdMappingStreamControllerIT {

    private static final String UNIREF_ID_MAPPING_STREAM_RESULT_PATH =
            "/idmapping/uniref/results/stream/{jobId}";

    @Autowired private UniProtStoreClient<UniRefEntryLight> storeClient;

    @Qualifier("uniRefFacetTupleStreamTemplate")
    @Autowired
    private FacetTupleStreamTemplate facetTupleStreamTemplate;

    @Qualifier("uniRefTupleStreamTemplate")
    @Autowired
    private TupleStreamTemplate tupleStreamTemplate;

    @Autowired protected JobOperation uniRefIdMappingJobOp;

    @Autowired private MockMvc mockMvc;

    @MockBean(name = "idMappingRdfRestTemplate")
    private RestTemplate idMappingRdfRestTemplate;

    @Override
    protected String getIdMappingResultPath() {
        return UNIREF_ID_MAPPING_STREAM_RESULT_PATH;
    }

    @Override
    protected JobOperation getJobOperation() {
        return uniRefIdMappingJobOp;
    }

    @Override
    protected MockMvc getMockMvc() {
        return mockMvc;
    }

    @Override
    protected UniProtDataType getUniProtDataType() {
        return UniProtDataType.UNIREF;
    }

    @Override
    protected String getFieldValueForValidatedField(String searchField) {
        return getUniRefFieldValueForValidatedField(searchField);
    }

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
    protected Stream<Arguments> getAllReturnedFields() {
        return ReturnFieldConfigFactory.getReturnFieldConfig(UniProtDataType.UNIREF)
                .getReturnFields().stream()
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
        when(idMappingRdfRestTemplate.getUriTemplateHandler())
                .thenReturn(new DefaultUriBuilderFactory());
        when(idMappingRdfRestTemplate.getForObject(any(), any())).thenReturn(SAMPLE_RDF);

        saveEntries(cloudSolrClient, storeClient);
    }

    @Test
    void streamUniRefWithSuccess() throws Exception {
        // when
        IdMappingJob job = getJobOperation().createAndPutJobInCache();

        ResultActions response =
                performRequest(
                        get(getIdMappingResultPath(), job.getJobId())
                                .param("query", "identity:0.5")
                                .param("fields", "id,name,common_taxon,sequence")
                                .param("sort", "id desc")
                                .param("download", "true")
                                .header(ACCEPT, APPLICATION_JSON_VALUE));

        // then
        response.andDo(log())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.results.size()", is(20)))
                .andExpect(
                        jsonPath(
                                "$.results.*.from",
                                contains(
                                        "Q00020", "Q00019", "Q00018", "Q00017", "Q00016", "Q00015",
                                        "Q00014", "Q00013", "Q00012", "Q00011", "Q00010", "Q00009",
                                        "Q00008", "Q00007", "Q00006", "Q00005", "Q00004", "Q00003",
                                        "Q00002", "Q00001")))
                .andExpect(
                        jsonPath(
                                "$.results.*.to.id",
                                contains(
                                        "UniRef50_P03920",
                                        "UniRef50_P03919",
                                        "UniRef50_P03918",
                                        "UniRef50_P03917",
                                        "UniRef50_P03916",
                                        "UniRef50_P03915",
                                        "UniRef50_P03914",
                                        "UniRef50_P03913",
                                        "UniRef50_P03912",
                                        "UniRef50_P03911",
                                        "UniRef50_P03910",
                                        "UniRef50_P03909",
                                        "UniRef50_P03908",
                                        "UniRef50_P03907",
                                        "UniRef50_P03906",
                                        "UniRef50_P03905",
                                        "UniRef50_P03904",
                                        "UniRef50_P03903",
                                        "UniRef50_P03902",
                                        "UniRef50_P03901")))
                .andExpect(jsonPath("$.results.*.to.commonTaxon").exists())
                .andExpect(jsonPath("$.results.*.to.members").doesNotExist());
    }
}
