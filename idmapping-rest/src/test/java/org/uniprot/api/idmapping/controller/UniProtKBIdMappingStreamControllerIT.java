package org.uniprot.api.idmapping.controller;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpHeaders.ACCEPT;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.log;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.uniprot.api.idmapping.controller.utils.IdMappingUniProtKBITUtils.*;
import static org.uniprot.api.rest.output.UniProtMediaType.FASTA_MEDIA_TYPE_VALUE;

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
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.DefaultUriBuilderFactory;
import org.uniprot.api.common.repository.solrstream.FacetTupleStreamTemplate;
import org.uniprot.api.common.repository.stream.common.TupleStreamTemplate;
import org.uniprot.api.common.repository.stream.store.uniprotkb.TaxonomyLineageRepository;
import org.uniprot.api.idmapping.IdMappingREST;
import org.uniprot.api.idmapping.controller.utils.DataStoreTestConfig;
import org.uniprot.api.idmapping.controller.utils.JobOperation;
import org.uniprot.api.idmapping.model.IdMappingJob;
import org.uniprot.core.uniprotkb.UniProtKBEntry;
import org.uniprot.store.config.UniProtDataType;
import org.uniprot.store.datastore.UniProtStoreClient;
import org.uniprot.store.search.SolrCollection;
import org.uniprot.store.search.document.taxonomy.TaxonomyDocument;

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

    @Autowired private TaxonomyLineageRepository taxRepository;

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
        return List.of(SolrCollection.uniprot, SolrCollection.taxonomy);
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

        ReflectionTestUtils.setField(taxRepository, "solrClient", cloudSolrClient);

        TaxonomyDocument taxonomyDocument = createTaxonomyEntry(9606L);
        cloudSolrClient.addBean(SolrCollection.taxonomy.name(), taxonomyDocument);
        cloudSolrClient.commit(SolrCollection.taxonomy.name());
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

    @Test
    void streamIdMappingWithIsoform() throws Exception {
        // when
        IdMappingJob job = getJobOperation().createAndPutJobInCache();
        ResultActions response =
                performRequest(
                        get(getIdMappingResultPath(), job.getJobId())
                                .header(ACCEPT, MediaType.APPLICATION_JSON)
                                .param("includeIsoform", "true"));
        // then
        response.andDo(log())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.results.size()", Matchers.is(24)))
                .andExpect(
                        jsonPath(
                                "$.results.*.to.primaryAccession",
                                contains(
                                        "Q00001",
                                        "Q00002",
                                        "Q00003",
                                        "Q00004",
                                        "Q00005",
                                        "Q00005-2",
                                        "Q00006",
                                        "Q00007",
                                        "Q00008",
                                        "Q00009",
                                        "Q00010",
                                        "Q00010-2",
                                        "Q00011",
                                        "Q00012",
                                        "Q00013",
                                        "Q00014",
                                        "Q00015",
                                        "Q00015-2",
                                        "Q00016",
                                        "Q00017",
                                        "Q00018",
                                        "Q00019",
                                        "Q00020",
                                        "Q00020-2")));
    }

    @Test
    void streamOnlyIsoform() throws Exception {
        // when
        IdMappingJob job = getJobOperation().createAndPutJobInCache();
        ResultActions response =
                performRequest(
                        get(getIdMappingResultPath(), job.getJobId())
                                .header(ACCEPT, MediaType.APPLICATION_JSON)
                                .param("query", "is_isoform:true")
                                .param("includeIsoform", "true"));
        // then
        response.andDo(log())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.results.size()", Matchers.is(4)))
                .andExpect(
                        jsonPath(
                                "$.results.*.to.primaryAccession",
                                contains("Q00005-2", "Q00010-2", "Q00015-2", "Q00020-2")));
    }

    @Test
    void streamOnlyReviewedIsoform() throws Exception {
        // when
        IdMappingJob job = getJobOperation().createAndPutJobInCache();
        ResultActions response =
                performRequest(
                        get(getIdMappingResultPath(), job.getJobId())
                                .header(ACCEPT, MediaType.APPLICATION_JSON)
                                .param("query", "is_isoform:true AND reviewed:true")
                                .param("includeIsoform", "true"));
        // then
        response.andDo(log())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.results.size()", Matchers.is(2)))
                .andExpect(
                        jsonPath(
                                "$.results.*.to.primaryAccession",
                                contains("Q00010-2", "Q00020-2")));
    }

    @Test
    void streamOnlyIsoformWithSorting() throws Exception {
        // when
        IdMappingJob job = getJobOperation().createAndPutJobInCache();
        ResultActions response =
                performRequest(
                        get(getIdMappingResultPath(), job.getJobId())
                                .header(ACCEPT, MediaType.APPLICATION_JSON)
                                .param("query", "is_isoform:true")
                                .param("includeIsoform", "true")
                                .param("sort", "accession desc"));
        // then
        response.andDo(log())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.results.size()", Matchers.is(4)))
                .andExpect(
                        jsonPath(
                                "$.results.*.to.primaryAccession",
                                contains("Q00020-2", "Q00015-2", "Q00010-2", "Q00005-2")));
    }

    @Test
    void streamOnlyIsoformByQuery() throws Exception {
        // when
        IdMappingJob job = getJobOperation().createAndPutJobInCache();
        ResultActions response =
                performRequest(
                        get(getIdMappingResultPath(), job.getJobId())
                                .header(ACCEPT, MediaType.APPLICATION_JSON)
                                .param("query", "is_isoform:true")
                                .param("sort", "accession desc"));
        // then
        response.andDo(log())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.results.size()", Matchers.is(4)))
                .andExpect(
                        jsonPath(
                                "$.results.*.to.primaryAccession",
                                contains("Q00020-2", "Q00015-2", "Q00010-2", "Q00005-2")));
    }

    @Test // includeIsoform=false has lower precedence than query is_isoform:true
    void streamOnlyIsoformWithQuery() throws Exception {
        // when
        IdMappingJob job = getJobOperation().createAndPutJobInCache();
        ResultActions response =
                performRequest(
                        get(getIdMappingResultPath(), job.getJobId())
                                .header(ACCEPT, MediaType.APPLICATION_JSON)
                                .param("query", "is_isoform:true")
                                .param("includeIsoform", "false"));
        // then
        response.andDo(log())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.results.size()", Matchers.is(4)))
                .andExpect(
                        jsonPath(
                                "$.results.*.to.primaryAccession",
                                contains("Q00005-2", "Q00010-2", "Q00015-2", "Q00020-2")));
    }

    @Test
    void streamOnlyIsoformWithIncludeIsoformTrue() throws Exception {
        // when
        IdMappingJob job = getJobOperation().createAndPutJobInCache();
        ResultActions response =
                performRequest(
                        get(getIdMappingResultPath(), job.getJobId())
                                .header(ACCEPT, MediaType.APPLICATION_JSON)
                                .param("query", "is_isoform:false")
                                .param("includeIsoform", "true")
                                .param("sort", "accession desc"));
        // then
        response.andDo(log())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.results.size()", Matchers.is(20)))
                .andExpect(
                        jsonPath(
                                "$.results.*.to.primaryAccession",
                                contains(
                                        "Q00020", "Q00019", "Q00018", "Q00017", "Q00016", "Q00015",
                                        "Q00014", "Q00013", "Q00012", "Q00011", "Q00010", "Q00009",
                                        "Q00008", "Q00007", "Q00006", "Q00005", "Q00004", "Q00003",
                                        "Q00002", "Q00001")));
    }

    @Test
    void streamCanReturnLineageData() throws Exception {
        // when
        IdMappingJob job = getJobOperation().createAndPutJobInCache(6);

        ResultActions response =
                performRequest(
                        get(getIdMappingResultPath(), job.getJobId())
                                .header(ACCEPT, MediaType.APPLICATION_JSON)
                                .param("fields", "accession,lineage"));
        // then
        response.andDo(log())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().doesNotExist("Content-Disposition"))
                .andExpect(jsonPath("$.results.size()", is(6)))
                .andExpect(
                        jsonPath(
                                "$.results.*.to.primaryAccession",
                                containsInAnyOrder(
                                        "Q00001", "Q00002", "Q00003", "Q00004", "Q00005",
                                        "Q00006")))
                .andExpect(
                        jsonPath(
                                "$.results.*.to.lineages[0].taxonId",
                                contains(9607, 9607, 9607, 9607, 9607, 9607)))
                .andExpect(
                        jsonPath(
                                "$.results.*.to.lineages[1].taxonId",
                                contains(9608, 9608, 9608, 9608, 9608, 9608)));
    }

    @Test
    void testIdMappingWithProteinVersions() throws Exception {
        // when
        IdMappingJob job =
                getJobOperation()
                        .createAndPutJobInCache(
                                UNIPROTKB_AC_ID_STR, UNIPROTKB_STR, "Q00001,Q00002.2,Q00003.3");
        ResultActions response =
                performRequest(
                        get(getIdMappingResultPath(), job.getJobId())
                                .header(ACCEPT, APPLICATION_JSON_VALUE));
        // then
        response.andDo(log())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.results.size()", is(3)))
                .andExpect(
                        jsonPath(
                                "$.results.*.from",
                                containsInAnyOrder("Q00001", "Q00002.2", "Q00003.3")))
                .andExpect(
                        jsonPath(
                                "$.results.*.to.primaryAccession",
                                containsInAnyOrder("Q00001", "Q00002", "Q00003")));
    }

    @Test
    void testIdMappingWithTSVSubSequenceValid() throws Exception {
        // when
        IdMappingJob job =
                getJobOperation()
                        .createAndPutJobInCache(
                                UNIPROTKB_AC_ID_STR, UNIPROTKB_STR, "Q00001[10-20],Q00002[20-30]");
        ResultActions response =
                performRequest(
                        get(getIdMappingResultPath(), job.getJobId())
                                .header(ACCEPT, FASTA_MEDIA_TYPE_VALUE)
                                .param("subsequence", "true"));
        // then
        response.andDo(log())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, FASTA_MEDIA_TYPE_VALUE))
                .andExpect(content().string(containsString(">tr|Q00001|10-20\nLVVVTMATLSL\n")))
                .andExpect(content().string(containsString(">sp|Q00002|20-30\nLARPSFSLVED")));
    }

    @Test
    void testIdMappingWithTSVSubSequenceInValidContentType() throws Exception {
        // when
        IdMappingJob job =
                getJobOperation()
                        .createAndPutJobInCache(
                                UNIPROTKB_AC_ID_STR, UNIPROTKB_STR, "Q00001[10-20],Q00002[20-30]");
        ResultActions response =
                mockMvc.perform(
                        get(getIdMappingResultPath(), job.getJobId())
                                .header(ACCEPT, APPLICATION_JSON_VALUE)
                                .param("subsequence", "invalid"));
        // then
        response.andDo(log())
                .andExpect(status().is(HttpStatus.BAD_REQUEST.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(
                        jsonPath(
                                "$.messages.*",
                                containsInAnyOrder(
                                        "Invalid subsequence parameter value. Expected true or false",
                                        "Invalid content type received, 'application/json'. 'subsequence' parameter only accepted for 'text/plain;format=fasta' content type.")));
    }

    @Test
    void testIdMappingWithTSVSubSequenceInValidFromIds() throws Exception {
        // when
        IdMappingJob job =
                getJobOperation()
                        .createAndPutJobInCache(
                                UNIPROTKB_AC_ID_STR, UNIPROTKB_STR, "Q00001[10-20],Q00002,Q00003");
        ResultActions response =
                performRequest(
                        get(getIdMappingResultPath(), job.getJobId())
                                .header(ACCEPT, FASTA_MEDIA_TYPE_VALUE)
                                .param("subsequence", "true"));
        // then
        response.andDo(log())
                .andExpect(status().is(HttpStatus.BAD_REQUEST.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, FASTA_MEDIA_TYPE_VALUE))
                .andExpect(
                        content()
                                .string(
                                        containsString(
                                                "Invalid request received. Unable to compute fasta subsequence for IDs: Q00002,Q00003. Expected format is accession[begin-end], for example:Q00001[10-20]")));
    }
}
