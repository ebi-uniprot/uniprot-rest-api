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
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.uniprot.api.idmapping.controller.utils.IdMappingUniParcITUtils.*;

import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.autoconfigure.web.client.AutoConfigureWebClient;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
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
import org.uniprot.api.idmapping.controller.utils.DataStoreTestConfig;
import org.uniprot.api.idmapping.controller.utils.JobOperation;
import org.uniprot.api.idmapping.model.IdMappingJob;
import org.uniprot.api.rest.output.UniProtMediaType;
import org.uniprot.api.rest.respository.facet.impl.UniParcFacetConfig;
import org.uniprot.api.rest.service.RDFPrologs;
import org.uniprot.core.uniparc.UniParcEntry;
import org.uniprot.store.config.UniProtDataType;
import org.uniprot.store.datastore.UniProtStoreClient;
import org.uniprot.store.search.SolrCollection;

/**
 * @author lgonzales
 * @since 26/02/2021
 */
@ActiveProfiles(profiles = {"offline", "idmapping"})
@ContextConfiguration(classes = {DataStoreTestConfig.class, IdMappingREST.class})
@WebMvcTest(UniParcIdMappingResultsController.class)
@AutoConfigureWebClient
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class UniParcIdMappingResultsControllerIT extends AbstractIdMappingResultsControllerIT {

    private static final String UNIPARC_ID_MAPPING_RESULT = "/idmapping/uniparc/results/{jobId}";
    private static final String UNIPARC_ID_MAPPING__STREAM_RESULT =
            "/idmapping/uniparc/results/stream/{jobId}";

    @Autowired private UniParcFacetConfig facetConfig;

    @Autowired private UniProtStoreClient<UniParcEntry> storeClient;

    @Autowired private MockMvc mockMvc;

    @Qualifier("uniParcFacetTupleStreamTemplate")
    @Autowired
    private FacetTupleStreamTemplate facetTupleStreamTemplate;

    @Qualifier("uniParcTupleStreamTemplate")
    @Autowired
    private TupleStreamTemplate tupleStreamTemplate;

    @Autowired private JobOperation uniParcIdMappingJobOp;

    @Autowired private RestTemplate uniParcRestTemplate;

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
        when(uniParcRestTemplate.getUriTemplateHandler())
                .thenReturn(new DefaultUriBuilderFactory());
        when(uniParcRestTemplate.getForObject(any(), any())).thenReturn(SAMPLE_RDF);

        saveEntries(cloudSolrClient, storeClient);
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
                                        .param("fields", "upi,accession")
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
                .andExpect(jsonPath("$.results.*.to.uniParcCrossReferences.*.database").exists())
                .andExpect(
                        jsonPath("$.results.*.to.uniParcCrossReferences.*.organism")
                                .doesNotExist());
    }

    @Test
    void streamRDFCanReturnSuccess() throws Exception {
        // when
        IdMappingJob job = getJobOperation().createAndPutJobInCache();
        ;
        MockHttpServletRequestBuilder requestBuilder =
                get(UNIPARC_ID_MAPPING__STREAM_RESULT, job.getJobId())
                        .header(ACCEPT, UniProtMediaType.RDF_MEDIA_TYPE);

        MvcResult response = mockMvc.perform(requestBuilder).andReturn();

        // then
        mockMvc.perform(asyncDispatch(response))
                .andDo(log())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().doesNotExist("Content-Disposition"))
                .andExpect(content().string(startsWith(RDFPrologs.UNIPARC_RDF_PROLOG)))
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

    @Override
    protected String getDefaultSearchQuery() {
        return "9606";
    }
}
