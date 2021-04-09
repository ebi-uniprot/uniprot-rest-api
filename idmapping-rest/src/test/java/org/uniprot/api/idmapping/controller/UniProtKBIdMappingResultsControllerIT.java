package org.uniprot.api.idmapping.controller;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.startsWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpHeaders.ACCEPT;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.log;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.uniprot.api.idmapping.controller.utils.IdMappingUniProtKBITUtils.*;
import static org.uniprot.api.idmapping.controller.utils.IdMappingUniProtKBITUtils.UNIPROTKB_AC_ID_STR;
import static org.uniprot.api.idmapping.controller.utils.IdMappingUniProtKBITUtils.UNIPROTKB_STR;

import java.util.List;

import org.hamcrest.Matchers;
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
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.DefaultUriBuilderFactory;
import org.uniprot.api.common.repository.search.facet.FacetConfig;
import org.uniprot.api.common.repository.solrstream.FacetTupleStreamTemplate;
import org.uniprot.api.common.repository.stream.common.TupleStreamTemplate;
import org.uniprot.api.idmapping.IdMappingREST;
import org.uniprot.api.idmapping.controller.utils.DataStoreTestConfig;
import org.uniprot.api.idmapping.controller.utils.JobOperation;
import org.uniprot.api.idmapping.model.IdMappingJob;
import org.uniprot.api.rest.output.UniProtMediaType;
import org.uniprot.api.rest.respository.facet.impl.UniProtKBFacetConfig;
import org.uniprot.api.rest.service.RDFPrologs;
import org.uniprot.core.uniprotkb.UniProtKBEntry;
import org.uniprot.store.config.UniProtDataType;
import org.uniprot.store.datastore.UniProtStoreClient;
import org.uniprot.store.search.SolrCollection;

/**
 * @author sahmad
 * @created 18/02/2021
 */
@ActiveProfiles(profiles = "offline")
@ContextConfiguration(classes = {DataStoreTestConfig.class, IdMappingREST.class})
@WebMvcTest(UniProtKBIdMappingResultsController.class)
@AutoConfigureWebClient
@ExtendWith(value = {SpringExtension.class})
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class UniProtKBIdMappingResultsControllerIT extends AbstractIdMappingResultsControllerIT {
    private static final String UNIPROTKB_ID_MAPPING_RESULT_PATH =
            "/idmapping/uniprotkb/results/{jobId}";
    private static final String UNIPROTKB_ID_MAPPING_STREAM_RESULT_PATH =
            "/idmapping/uniprotkb/results/stream/{jobId}";

    @Autowired private UniProtKBFacetConfig facetConfig;

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

    @Override
    protected MockMvc getMockMvc() {
        return mockMvc;
    }

    @Override
    protected String getIdMappingResultPath() {
        return UNIPROTKB_ID_MAPPING_RESULT_PATH;
    }

    @Override
    protected UniProtDataType getUniProtDataType() {
        return UniProtDataType.UNIPROTKB;
    }

    @Override
    protected FacetConfig getFacetConfig() {
        return facetConfig;
    }

    @Override
    protected JobOperation getJobOperation() {
        return uniProtKBIdMappingJobOp;
    }

    @Override
    protected String getFieldValueForValidatedField(String searchField) {
        return getUniProtKbFieldValueForValidatedField(searchField);
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
    void testUniProtKBToUniProtKBMapping() throws Exception {
        // when
        IdMappingJob job =
                getJobOperation()
                        .createAndPutJobInCache(
                                UNIPROTKB_AC_ID_STR, UNIPROTKB_STR, "Q00001,Q00002");
        ResultActions response =
                mockMvc.perform(
                        get(UNIPROTKB_ID_MAPPING_RESULT_PATH, job.getJobId())
                                .header(ACCEPT, MediaType.APPLICATION_JSON));
        // then
        response.andDo(log())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.results.size()", Matchers.is(2)))
                .andExpect(jsonPath("$.results.*.from", contains("Q00001", "Q00002")))
                .andExpect(
                        jsonPath("$.results.*.to.primaryAccession", contains("Q00001", "Q00002")));
    }

    @Test
    void testIdMappingWithSuccess() throws Exception {
        // when
        IdMappingJob job = getJobOperation().createAndPutJobInCache();
        ResultActions response =
                mockMvc.perform(
                        get(getIdMappingResultPath(), job.getJobId())
                                .header(ACCEPT, MediaType.APPLICATION_JSON)
                                .param("facets", "reviewed,proteins_with")
                                .param("query", "reviewed:true")
                                .param("fields", "accession,sequence")
                                .param("sort", "accession desc")
                                .param("size", "6"));
        // then
        response.andDo(log())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.facets.size()", is(2)))
                .andExpect(jsonPath("$.facets.*.name", contains("reviewed", "proteins_with")))
                .andExpect(jsonPath("$.facets[0].values.size()", is(1)))
                .andExpect(jsonPath("$.facets[0].values.*.value", contains("true")))
                .andExpect(
                        jsonPath("$.facets[0].values.*.label", contains("Reviewed (Swiss-Prot)")))
                .andExpect(jsonPath("$.facets[0].values.*.count", contains(10)))
                .andExpect(jsonPath("$.results.size()", is(6)))
                .andExpect(
                        jsonPath(
                                "$.results.*.from",
                                contains(
                                        "Q00020", "Q00018", "Q00016", "Q00014", "Q00012",
                                        "Q00010")))
                .andExpect(
                        jsonPath(
                                "$.results.*.to.primaryAccession",
                                contains(
                                        "Q00020", "Q00018", "Q00016", "Q00014", "Q00012",
                                        "Q00010")))
                .andExpect(jsonPath("$.results.*.to.sequence").exists())
                .andExpect(jsonPath("$.results.*.to.organism").doesNotExist());
    }

    @Test
    void testCanSortMultipleFieldsWithSuccess() throws Exception {
        // when
        IdMappingJob job =
                getJobOperation()
                        .createAndPutJobInCache(
                                UNIPROTKB_AC_ID_STR, UNIPROTKB_STR, "Q00001,Q00002");
        ResultActions response =
                mockMvc.perform(
                        get(UNIPROTKB_ID_MAPPING_RESULT_PATH, job.getJobId())
                                .header(ACCEPT, MediaType.APPLICATION_JSON)
                                .param("facets", "proteins_with,reviewed")
                                .param("sort", "gene desc , accession asc"));
        // then
        response.andDo(log())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.results.size()", Matchers.is(2)))
                .andExpect(jsonPath("$.results.*.from", contains("Q00002", "Q00001")))
                .andExpect(
                        jsonPath("$.results.*.to.primaryAccession", contains("Q00002", "Q00001")));
    }

    @Test
    void streamRDFCanReturnSuccess() throws Exception {
        // when
        IdMappingJob job =
                getJobOperation()
                        .createAndPutJobInCache(UNIPROTKB_AC_ID_STR, UNIPROTKB_STR, "Q00001");
        MockHttpServletRequestBuilder requestBuilder =
                get(UNIPROTKB_ID_MAPPING_STREAM_RESULT_PATH, job.getJobId())
                        .header(ACCEPT, UniProtMediaType.RDF_MEDIA_TYPE);

        MvcResult response = mockMvc.perform(requestBuilder).andReturn();

        // then
        mockMvc.perform(asyncDispatch(response))
                .andDo(log())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().doesNotExist("Content-Disposition"))
                .andExpect(content().string(startsWith(RDFPrologs.UNIPROT_RDF_PROLOG)))
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
                                                "From\tEntry\tEntry Name\tReviewed\tProtein names\tGene Names\tOrganism\tLength")))
                .andExpect(
                        content()
                                .string(
                                        containsString(
                                                "Q00001\tQ00001\tFGFR2_HUMAN\tunreviewed\tFibroblast growth factor receptor 2, FGFR-2, EC 2.7.10.1 (K-sam, KGFR) (Keratinocyte growth factor receptor) (CD antigen CD332)\tFGFR2 BEK KGFR KSAM; gene 1 gene 1 gene 1\tHomo sapiens (Human)\t821\n"
                                                        + "Q00002\tQ00002\tFGFR2_HUMAN\treviewed\tFibroblast growth factor receptor 2, FGFR-2, EC 2.7.10.1 (K-sam, KGFR) (Keratinocyte growth factor receptor) (CD antigen CD332)\tFGFR2 BEK KGFR KSAM; gene 2 gene 2 gene 2\tHomo sapiens (Human)\t821\n"
                                                        + "Q00003\tQ00003\tFGFR2_HUMAN\tunreviewed\tFibroblast growth factor receptor 2, FGFR-2, EC 2.7.10.1 (K-sam, KGFR) (Keratinocyte growth factor receptor) (CD antigen CD332)\tFGFR2 BEK KGFR KSAM; gene 3 gene 3 gene 3\tHomo sapiens (Human)\t821\n"
                                                        + "Q00004\tQ00004\tFGFR2_HUMAN\treviewed\tFibroblast growth factor receptor 2, FGFR-2, EC 2.7.10.1 (K-sam, KGFR) (Keratinocyte growth factor receptor) (CD antigen CD332)\tFGFR2 BEK KGFR KSAM; gene 4 gene 4 gene 4\tHomo sapiens (Human)\t821\n"
                                                        + "Q00005\tQ00005\tFGFR2_HUMAN\tunreviewed\tFibroblast growth factor receptor 2, FGFR-2, EC 2.7.10.1 (K-sam, KGFR) (Keratinocyte growth factor receptor) (CD antigen CD332)\tFGFR2 BEK KGFR KSAM; gene 5 gene 5 gene 5\tHomo sapiens (Human)\t821\n")));
    }

    @Override
    protected String getDefaultSearchQuery() {
        return "FGF1"; // geneName
    }
}
