package org.uniprot.api.idmapping.controller;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpHeaders.ACCEPT;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.log;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.uniprot.api.idmapping.controller.utils.IdMappingUniProtKBITUtils.*;

import java.util.List;

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
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
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
import org.uniprot.core.uniprotkb.UniProtKBEntry;
import org.uniprot.store.config.UniProtDataType;
import org.uniprot.store.datastore.UniProtStoreClient;
import org.uniprot.store.search.SolrCollection;

/**
 * @author lgonzales
 * @since 08/03/2021
 */
@ActiveProfiles(profiles = "offline")
@ContextConfiguration(classes = {DataStoreTestConfig.class, IdMappingREST.class})
@WebMvcTest(UniProtKBIdMappingResultsController.class)
@AutoConfigureWebClient
@ExtendWith(value = {SpringExtension.class})
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class UniProtKBIdMappingStreamControllerIT extends AbstractIdMappingStreamControllerIT {

    private static final String UNIPROTKB_ID_MAPPING_STREAM_RESULT_PATH =
            "/idmapping/uniprotkb/results/stream/{jobId}";

    @Autowired private UniProtStoreClient<UniProtKBEntry> storeClient;

    @Qualifier("uniproKBfacetTupleStreamTemplate")
    @Autowired
    private FacetTupleStreamTemplate facetTupleStreamTemplate;

    @Qualifier("uniProtKBTupleStreamTemplate")
    @Autowired
    private TupleStreamTemplate tupleStreamTemplate;

    @Autowired protected JobOperation uniProtKBIdMappingJobOp;

    @Autowired private MockMvc mockMvc;

    @Autowired private RestTemplate uniProtKBRestTemplate;

    @Override
    protected String getIdMappingResultPath() {
        return UNIPROTKB_ID_MAPPING_STREAM_RESULT_PATH;
    }

    @Override
    protected JobOperation getJobOperation() {
        return uniProtKBIdMappingJobOp;
    }

    @Override
    protected MockMvc getMockMvc() {
        return mockMvc;
    }

    @Override
    protected UniProtDataType getUniProtDataType() {
        return UniProtDataType.UNIPROTKB;
    }

    @Override
    protected String getFieldValueForValidatedField(String searchField) {
        return getUniProtKbFieldValueForValidatedField(searchField);
    }

    @Override
    protected List<SolrCollection> getSolrCollections() {
        return List.of(SolrCollection.uniprot);
    }

    @Override
    protected TupleStreamTemplate getTupleStreamTemplate() {
        return tupleStreamTemplate;
    }

    @Override
    protected FacetTupleStreamTemplate getFacetTupleStreamTemplate() {
        return facetTupleStreamTemplate;
    }

    @BeforeAll
    void saveEntriesStore() throws Exception {

        when(uniProtKBRestTemplate.getUriTemplateHandler())
                .thenReturn(new DefaultUriBuilderFactory());
        when(uniProtKBRestTemplate.getForObject(any(), any())).thenReturn(SAMPLE_RDF);

        for (int i = 1; i <= 20; i++) {
            saveEntry(i, cloudSolrClient, storeClient);
        }
    }

    @Test
    void streamUniProtKBWithSuccess() throws Exception {
        // when
        IdMappingJob job = getJobOperation().createAndPutJobInCache();
        ResultActions response =
                performRequest(
                        get(getIdMappingResultPath(), job.getJobId())
                                .header(ACCEPT, MediaType.APPLICATION_JSON)
                                .param("query", "reviewed:true")
                                .param("fields", "accession,sequence")
                                .param("sort", "accession desc"));
        // then
        response.andDo(log())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.results.size()", is(10)))
                .andExpect(
                        jsonPath(
                                "$.results.*.from",
                                contains(
                                        "Q00020", "Q00018", "Q00016", "Q00014", "Q00012", "Q00010",
                                        "Q00008", "Q00006", "Q00004", "Q00002")))
                .andExpect(
                        jsonPath(
                                "$.results.*.to.primaryAccession",
                                contains(
                                        "Q00020", "Q00018", "Q00016", "Q00014", "Q00012", "Q00010",
                                        "Q00008", "Q00006", "Q00004", "Q00002")))
                .andExpect(jsonPath("$.results.*.to.sequence").exists())
                .andExpect(jsonPath("$.results.*.to.organism").doesNotExist());
    }
}
