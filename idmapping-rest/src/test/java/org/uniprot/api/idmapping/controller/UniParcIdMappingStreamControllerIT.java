package org.uniprot.api.idmapping.controller;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.autoconfigure.web.client.AutoConfigureWebClient;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.DefaultUriBuilderFactory;
import org.uniprot.api.common.repository.solrstream.FacetTupleStreamTemplate;
import org.uniprot.api.common.repository.stream.common.TupleStreamTemplate;
import org.uniprot.api.idmapping.IdMappingREST;
import org.uniprot.api.idmapping.controller.utils.DataStoreTestConfig;
import org.uniprot.api.idmapping.controller.utils.JobOperation;
import org.uniprot.core.uniparc.UniParcEntry;
import org.uniprot.store.config.UniProtDataType;
import org.uniprot.store.datastore.UniProtStoreClient;
import org.uniprot.store.search.SolrCollection;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.uniprot.api.idmapping.controller.utils.IdMappingUniParcITUtils.*;

/**
 * @author lgonzales
 * @since 08/03/2021
 */
@ActiveProfiles(profiles = "offline")
@ContextConfiguration(classes = {DataStoreTestConfig.class, IdMappingREST.class})
@WebMvcTest(UniParcIdMappingResultsController.class)
@AutoConfigureWebClient
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class UniParcIdMappingStreamControllerIT extends AbstractIdMappingBasicControllerIT{

    private static final String UNIPARC_ID_MAPPING_STREAM_RESULT_PATH =
            "/idmapping/uniparc/results/stream/{jobId}";

    @Autowired
    private UniProtStoreClient<UniParcEntry> storeClient;

    @Qualifier("uniParcFacetTupleStreamTemplate")
    @Autowired
    private FacetTupleStreamTemplate facetTupleStreamTemplate;

    @Qualifier("uniParcTupleStreamTemplate")
    @Autowired
    private TupleStreamTemplate tupleStreamTemplate;

    @Autowired protected JobOperation uniParcIdMappingJobOp;

    @Autowired private MockMvc mockMvc;

    @Autowired private RestTemplate uniParcRestTemplate;

    @Override
    protected String getIdMappingResultPath() {
        return UNIPARC_ID_MAPPING_STREAM_RESULT_PATH;
    }

    @Override
    protected JobOperation getJobOperation() {
        return uniParcIdMappingJobOp;
    }

    @Override
    protected MockMvc getMockMvc() {
        return mockMvc;
    }

    @Override
    protected UniProtDataType getUniProtDataType() {
        return UniProtDataType.UNIPARC;
    }

    @Override
    protected String getFieldValueForValidatedField(String searchField) {
        return getUniParcFieldValueForValidatedField(searchField);
    }

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
    protected ResultActions performRequest(MockHttpServletRequestBuilder requestBuilder) throws Exception {
        MvcResult response = mockMvc.perform(requestBuilder).andReturn();
        return mockMvc.perform(asyncDispatch(response));
    }

    @BeforeAll
    void saveEntriesStore() throws Exception {
        when(uniParcRestTemplate.getUriTemplateHandler())
                .thenReturn(new DefaultUriBuilderFactory());
        when(uniParcRestTemplate.getForObject(any(), any())).thenReturn(SAMPLE_RDF);

        saveEntries(cloudSolrClient, storeClient);
    }
}