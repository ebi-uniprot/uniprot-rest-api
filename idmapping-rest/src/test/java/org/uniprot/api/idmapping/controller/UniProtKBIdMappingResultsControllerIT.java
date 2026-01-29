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
import static org.uniprot.api.idmapping.common.IdMappingUniProtKBITUtils.*;
import static org.uniprot.api.rest.output.UniProtMediaType.FASTA_MEDIA_TYPE_VALUE;
import static org.uniprot.api.rest.output.header.HttpCommonHeaderConfig.X_TOTAL_RESULTS;

import java.util.List;

import org.hamcrest.Matchers;
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
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.DefaultUriBuilderFactory;
import org.uniprot.api.common.repository.search.facet.FacetConfig;
import org.uniprot.api.common.repository.solrstream.FacetTupleStreamTemplate;
import org.uniprot.api.common.repository.stream.common.TupleStreamTemplate;
import org.uniprot.api.common.repository.stream.store.uniprotkb.TaxonomyLineageRepository;
import org.uniprot.api.idmapping.IdMappingREST;
import org.uniprot.api.idmapping.common.IdMappingDataStoreTestConfig;
import org.uniprot.api.idmapping.common.JobOperation;
import org.uniprot.api.idmapping.common.model.IdMappingJob;
import org.uniprot.api.idmapping.common.repository.UniprotKBMappingRepository;
import org.uniprot.api.rest.output.UniProtMediaType;
import org.uniprot.api.rest.respository.facet.impl.UniProtKBFacetConfig;
import org.uniprot.core.uniprotkb.DeletedReason;
import org.uniprot.core.uniprotkb.UniProtKBEntry;
import org.uniprot.store.config.UniProtDataType;
import org.uniprot.store.datastore.UniProtStoreClient;
import org.uniprot.store.search.SolrCollection;
import org.uniprot.store.search.document.taxonomy.TaxonomyDocument;

/**
 * @author sahmad
 * @created 18/02/2021
 */
@ActiveProfiles(profiles = {"offline", "idmapping"})
@ContextConfiguration(classes = {IdMappingDataStoreTestConfig.class, IdMappingREST.class})
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

    @Autowired private UniprotKBMappingRepository repository;

    @Autowired private UniProtStoreClient<UniProtKBEntry> uniProtStoreClient;

    @Qualifier("uniproKBfacetTupleStreamTemplate")
    @Autowired
    private FacetTupleStreamTemplate facetTupleStreamTemplate;

    @Qualifier("uniProtKBTupleStreamTemplate")
    @Autowired
    private TupleStreamTemplate tupleStreamTemplate;

    @Autowired protected JobOperation uniProtKBIdMappingJobOp;

    @Autowired private MockMvc mockMvc;

    @MockBean(name = "idMappingRdfRestTemplate")
    private RestTemplate uniProtKBRestTemplate;

    @Autowired private TaxonomyLineageRepository taxRepository;

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
        for (int i = 1; i <= this.maxFromIdsAllowed; i++) {
            saveEntry(i, cloudSolrClient, uniProtStoreClient);
        }

        saveInactiveEntry(cloudSolrClient);
        ReflectionTestUtils.setField(repository, "solrClient", cloudSolrClient);

        ReflectionTestUtils.setField(taxRepository, "solrClient", cloudSolrClient);

        TaxonomyDocument taxonomyDocument = createTaxonomyEntry(9606L);
        cloudSolrClient.addBean(SolrCollection.taxonomy.name(), taxonomyDocument);
        cloudSolrClient.commit(SolrCollection.taxonomy.name());
    }

    @BeforeEach
    void setUp() {
        when(uniProtKBRestTemplate.getUriTemplateHandler())
                .thenReturn(new DefaultUriBuilderFactory());
        when(uniProtKBRestTemplate.getForObject(any(), any())).thenReturn(SAMPLE_RDF);
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
    void testUniProtKBToUniProtKBInactiveEntriesMapping() throws Exception {
        // when
        IdMappingJob job =
                getJobOperation()
                        .createAndPutJobInCache(
                                UNIPROTKB_AC_ID_STR, UNIPROTKB_STR, "Q00001,I8FBX0");
        ResultActions response =
                mockMvc.perform(
                        get(UNIPROTKB_ID_MAPPING_RESULT_PATH, job.getJobId())
                                .param("fields", "accession")
                                .header(ACCEPT, MediaType.APPLICATION_JSON));
        // then
        response.andDo(log())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.results.size()", Matchers.is(2)))
                .andExpect(jsonPath("$.results.*.from", contains("Q00001", "I8FBX0")))
                .andExpect(
                        jsonPath("$.results.*.to.primaryAccession", contains("Q00001", "I8FBX0")))
                .andExpect(
                        jsonPath(
                                "$.results.*.to.entryType",
                                contains("UniProtKB unreviewed (TrEMBL)", "Inactive")))
                .andExpect(
                        jsonPath("$.results[1].to.extraAttributes.uniParcId", is("UPI0001661588")))
                .andExpect(
                        jsonPath(
                                "$.results[1].to.inactiveReason.inactiveReasonType", is("DELETED")))
                .andExpect(
                        jsonPath(
                                "$.results[1].to.inactiveReason.deletedReason",
                                is(DeletedReason.PROTEOME_EXCLUSION.getName())));
    }

    @Test
    void testIdMappingWithSuccess() throws Exception {
        // when
        IdMappingJob job = getJobOperation().createAndPutJobInCache(this.maxIdsWithFacets);
        ResultActions response =
                mockMvc.perform(
                        get(getIdMappingResultPath(), job.getJobId())
                                .header(ACCEPT, MediaType.APPLICATION_JSON)
                                .param("facets", "reviewed,proteins_with")
                                .param("query", "reviewed:true")
                                .param("fields", "accession,sequence")
                                .param("sort", "accession desc")
                                .param("size", "3"));
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
                .andExpect(jsonPath("$.facets[0].values.*.count", contains(5)))
                .andExpect(jsonPath("$.results.size()", is(3)))
                .andExpect(jsonPath("$.results.*.from", contains("Q00010", "Q00008", "Q00006")))
                .andExpect(
                        jsonPath(
                                "$.results.*.to.primaryAccession",
                                contains("Q00010", "Q00008", "Q00006")))
                .andExpect(jsonPath("$.results.*.to.sequence").exists())
                .andExpect(jsonPath("$.results.*.to.organism").doesNotExist());
    }

    @Test
    void testIdMappingWithIsoform() throws Exception {
        // when
        IdMappingJob job = getJobOperation().createAndPutJobInCache(this.maxFromIdsAllowed);
        ResultActions response =
                mockMvc.perform(
                        get(getIdMappingResultPath(), job.getJobId())
                                .header(ACCEPT, MediaType.APPLICATION_JSON)
                                .param("includeIsoform", "true")
                                .param("size", "10"));
        // then
        response.andDo(log())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.results.size()", Matchers.is(10)))
                .andExpect(
                        header().string(
                                        X_TOTAL_RESULTS,
                                        String.valueOf(this.maxFromIdsAllowed + 4)))
                .andExpect(header().string(HttpHeaders.LINK, notNullValue()))
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
                                        "Q00009")));
    }

    @Test
    void testIdMappingFilteringIsoform() throws Exception {
        // when
        IdMappingJob job = getJobOperation().createAndPutJobInCache(this.maxFromIdsAllowed);
        ResultActions response =
                mockMvc.perform(
                        get(getIdMappingResultPath(), job.getJobId())
                                .header(ACCEPT, MediaType.APPLICATION_JSON)
                                .param("includeIsoform", "false")
                                .param("size", "10"));
        // then
        response.andDo(log())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.results.size()", Matchers.is(10)))
                .andExpect(header().string(X_TOTAL_RESULTS, String.valueOf(this.maxFromIdsAllowed)))
                .andExpect(header().string(HttpHeaders.LINK, notNullValue()))
                .andExpect(
                        jsonPath(
                                "$.results.*.to.primaryAccession",
                                contains(
                                        "Q00001", "Q00002", "Q00003", "Q00004", "Q00005", "Q00006",
                                        "Q00007", "Q00008", "Q00009", "Q00010")));
    }

    @Test
    void testIdMappingWithSortedIsoform() throws Exception {
        // when
        IdMappingJob job = getJobOperation().createAndPutJobInCache(this.maxFromIdsAllowed);
        ResultActions response =
                mockMvc.perform(
                        get(getIdMappingResultPath(), job.getJobId())
                                .header(ACCEPT, MediaType.APPLICATION_JSON)
                                .param("includeIsoform", "true")
                                .param("sort", "accession desc")
                                .param("size", "10"));
        // then
        response.andDo(log())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.results.size()", Matchers.is(10)))
                .andExpect(
                        header().string(
                                        X_TOTAL_RESULTS,
                                        String.valueOf(this.maxFromIdsAllowed + 4)))
                .andExpect(header().string(HttpHeaders.LINK, notNullValue()))
                .andExpect(
                        jsonPath(
                                "$.results.*.to.primaryAccession",
                                contains(
                                        "Q00020-2",
                                        "Q00020",
                                        "Q00019",
                                        "Q00018",
                                        "Q00017",
                                        "Q00016",
                                        "Q00015-2",
                                        "Q00015",
                                        "Q00014",
                                        "Q00013")));
    }

    @Test
    void testIdMappingQueryIsoform() throws Exception {
        // when
        IdMappingJob job = getJobOperation().createAndPutJobInCache(this.maxFromIdsAllowed);
        ResultActions response =
                mockMvc.perform(
                        get(getIdMappingResultPath(), job.getJobId())
                                .header(ACCEPT, MediaType.APPLICATION_JSON)
                                .param("query", "is_isoform:true")
                                .param("includeIsoform", "false")
                                .param("size", "10"));
        // then
        response.andDo(log())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.results.size()", Matchers.is(4)))
                .andExpect(header().string(X_TOTAL_RESULTS, String.valueOf(4)))
                .andExpect(header().string(HttpHeaders.LINK, nullValue()))
                .andExpect(
                        jsonPath(
                                "$.results.*.to.primaryAccession",
                                contains("Q00005-2", "Q00010-2", "Q00015-2", "Q00020-2")));
    }

    @Test
    void testIdMappingIsoform() throws Exception {
        // when
        IdMappingJob job = getJobOperation().createAndPutJobInCache(this.maxFromIdsAllowed);
        ResultActions response =
                mockMvc.perform(
                        get(getIdMappingResultPath(), job.getJobId())
                                .header(ACCEPT, MediaType.APPLICATION_JSON)
                                .param("query", "is_isoform:false")
                                .param("includeIsoform", "true")
                                .param("size", "10"));
        // then
        response.andDo(log())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.results.size()", Matchers.is(10)))
                .andExpect(header().string(X_TOTAL_RESULTS, String.valueOf(20)))
                .andExpect(header().string(HttpHeaders.LINK, notNullValue()))
                .andExpect(
                        jsonPath(
                                "$.results.*.to.primaryAccession",
                                contains(
                                        "Q00001", "Q00002", "Q00003", "Q00004", "Q00005", "Q00006",
                                        "Q00007", "Q00008", "Q00009", "Q00010")));
    }

    @Test
    void testIdMappingWithIncludeIsoformAndIsIsoformTrue() throws Exception {
        // when
        IdMappingJob job = getJobOperation().createAndPutJobInCache(this.maxFromIdsAllowed);
        ResultActions response =
                mockMvc.perform(
                        get(getIdMappingResultPath(), job.getJobId())
                                .header(ACCEPT, MediaType.APPLICATION_JSON)
                                .param("query", "is_isoform:true")
                                .param("includeIsoform", "true")
                                .param("size", "10"));
        // then
        response.andDo(log())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.results.size()", Matchers.is(4)))
                .andExpect(header().string(X_TOTAL_RESULTS, String.valueOf(4)))
                .andExpect(header().string(HttpHeaders.LINK, nullValue()))
                .andExpect(
                        jsonPath(
                                "$.results.*.to.primaryAccession",
                                contains("Q00005-2", "Q00010-2", "Q00015-2", "Q00020-2")));
    }

    @Test
    void testIdMappingQueryIsoformAndIncludeIsoformFalse() throws Exception {
        // when
        IdMappingJob job = getJobOperation().createAndPutJobInCache(this.maxFromIdsAllowed);
        ResultActions response =
                mockMvc.perform(
                        get(getIdMappingResultPath(), job.getJobId())
                                .header(ACCEPT, MediaType.APPLICATION_JSON)
                                .param("query", "is_isoform:false")
                                .param("includeIsoform", "false")
                                .param("size", "10"));
        // then
        response.andDo(log())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.results.size()", Matchers.is(10)))
                .andExpect(header().string(X_TOTAL_RESULTS, String.valueOf(20)))
                .andExpect(header().string(HttpHeaders.LINK, notNullValue()))
                .andExpect(
                        jsonPath(
                                "$.results.*.to.primaryAccession",
                                contains(
                                        "Q00001", "Q00002", "Q00003", "Q00004", "Q00005", "Q00006",
                                        "Q00007", "Q00008", "Q00009", "Q00010")));
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
    void testCanReturnLineageData() throws Exception {
        // when
        IdMappingJob job = getJobOperation().createAndPutJobInCache(5);

        ResultActions response =
                mockMvc.perform(
                        get(UNIPROTKB_ID_MAPPING_RESULT_PATH, job.getJobId())
                                .header(ACCEPT, MediaType.APPLICATION_JSON)
                                .param("fields", "accession,lineage"));
        // then
        response.andDo(log())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().doesNotExist("Content-Disposition"))
                .andExpect(jsonPath("$.results.size()", is(5)))
                .andExpect(
                        jsonPath(
                                "$.results.*.to.primaryAccession",
                                containsInAnyOrder(
                                        "Q00001", "Q00002", "Q00003", "Q00004", "Q00005")))
                .andExpect(
                        jsonPath(
                                "$.results.*.to.lineages[0].taxonId",
                                contains(9607, 9607, 9607, 9607, 9607)))
                .andExpect(
                        jsonPath(
                                "$.results.*.to.lineages[1].taxonId",
                                contains(9608, 9608, 9608, 9608, 9608)));
    }

    @Test
    void streamRdfCanReturnSuccess() throws Exception {
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
                                                "From\tEntry\tEntry Name\tReviewed\tProtein names\tGene Names\tOrganism\tLength")))
                .andExpect(
                        content()
                                .string(
                                        containsString(
                                                "Q00001\tQ00001\tQ00001_HUMAN\tunreviewed\tFibroblast growth factor receptor 2 (FGFR-2) (EC 2.7.10.1) (K-sam) (KGFR) (Keratinocyte growth factor receptor) (CD antigen CD332)\tFGFR2 BEK KGFR KSAM; gene 1 gene 1 gene 1\tHomo sapiens (Human)\t821\n"
                                                        + "Q00002\tQ00002\tFGFR12345_HUMAN\treviewed\tFibroblast growth factor receptor 2 (FGFR-2) (EC 2.7.10.1) (K-sam) (KGFR) (Keratinocyte growth factor receptor) (CD antigen CD332)\tFGFR2 BEK KGFR KSAM; gene 2 gene 2 gene 2\tHomo sapiens (Human)\t821\n"
                                                        + "Q00003\tQ00003\tQ00003_HUMAN\tunreviewed\tFibroblast growth factor receptor 2 (FGFR-2) (EC 2.7.10.1) (K-sam) (KGFR) (Keratinocyte growth factor receptor) (CD antigen CD332)\tFGFR2 BEK KGFR KSAM; gene 3 gene 3 gene 3\tHomo sapiens (Human)\t821\n"
                                                        + "Q00004\tQ00004\tFGFR12345_HUMAN\treviewed\tFibroblast growth factor receptor 2 (FGFR-2) (EC 2.7.10.1) (K-sam) (KGFR) (Keratinocyte growth factor receptor) (CD antigen CD332)\tFGFR2 BEK KGFR KSAM; gene 4 gene 4 gene 4\tHomo sapiens (Human)\t821\n"
                                                        + "Q00005\tQ00005\tQ00005_HUMAN\tunreviewed\tFibroblast growth factor receptor 2 (FGFR-2) (EC 2.7.10.1) (K-sam) (KGFR) (Keratinocyte growth factor receptor) (CD antigen CD332)\tFGFR2 BEK KGFR KSAM; gene 5 gene 5 gene 5\tHomo sapiens (Human)\t821\n")));
    }

    @Test
    void testIdMappingWithSplitQuerySuccess() throws Exception {
        // when
        IdMappingJob job = getJobOperation().createAndPutJobInCache(this.maxIdsWithFacets);
        ResultActions response =
                mockMvc.perform(
                        get(getIdMappingResultPath(), job.getJobId())
                                .header(ACCEPT, MediaType.APPLICATION_JSON)
                                .param("facets", "reviewed")
                                .param("query", "id_default:FGFR12345")
                                .param("size", "10"));
        // then
        response.andDo(log())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.results.size()", is(5)))
                .andExpect(jsonPath("$.facets.size()", is(1)))
                .andExpect(jsonPath("$.facets[0].values.*.value", contains("true")))
                .andExpect(
                        jsonPath("$.facets[0].values.*.label", contains("Reviewed (Swiss-Prot)")))
                .andExpect(jsonPath("$.facets[0].values.*.count", contains(5)));
    }

    @Test
    void testIdMappingWithProteinVersions() throws Exception {
        // when
        IdMappingJob job =
                getJobOperation()
                        .createAndPutJobInCache(
                                UNIPROTKB_AC_ID_STR, UNIPROTKB_STR, "Q00001,Q00002.2,Q00003.3");
        ResultActions response =
                mockMvc.perform(
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
    void testIdMappingWithTremblProteinIdIgnoresAfterUnderScore() throws Exception {
        // when
        IdMappingJob job =
                getJobOperation()
                        .createAndPutJobInCache(
                                UNIPROTKB_AC_ID_STR,
                                UNIPROTKB_STR,
                                "Q00001.2,Q00002_TREMBL2,Q00003_TREMBL3");
        ResultActions response =
                mockMvc.perform(
                        get(getIdMappingResultPath(), job.getJobId())
                                .param("fields", "accession")
                                .header(ACCEPT, APPLICATION_JSON_VALUE));
        // then
        response.andDo(log())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.results.size()", is(3)))
                .andExpect(
                        jsonPath(
                                "$.results.*.from",
                                containsInAnyOrder("Q00001.2", "Q00002_TREMBL2", "Q00003_TREMBL3")))
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
                mockMvc.perform(
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
                mockMvc.perform(
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

    @Test
    void testIdMappingWithRepeatedAccessionSubSequenceValid() throws Exception {
        // when
        IdMappingJob job =
                getJobOperation()
                        .createAndPutJobInCache(
                                UNIPROTKB_AC_ID_STR,
                                UNIPROTKB_STR,
                                "Q00001[10-20],Q00002[20-30],Q00001[15-20]");
        ResultActions response =
                mockMvc.perform(
                        get(getIdMappingResultPath(), job.getJobId())
                                .header(ACCEPT, FASTA_MEDIA_TYPE_VALUE)
                                .param("subsequence", "true"));
        // then
        response.andDo(log())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, FASTA_MEDIA_TYPE_VALUE))
                .andExpect(content().string(containsString(">tr|Q00001|10-20\nLVVVTMATLSL\n")))
                .andExpect(content().string(containsString(">sp|Q00002|20-30\nLARPSFSLVED")))
                .andExpect(content().string(containsString(">tr|Q00001|15-20\nMATLSL")));
    }

    @Override
    protected String getDefaultSearchQuery() {
        return "FGF1"; // geneName
    }
}
