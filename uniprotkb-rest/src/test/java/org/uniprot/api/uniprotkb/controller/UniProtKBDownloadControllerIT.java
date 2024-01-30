package org.uniprot.api.uniprotkb.controller;

import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.*;
import static org.springframework.http.HttpHeaders.ACCEPT;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.MediaType.APPLICATION_XML_VALUE;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.log;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.uniprot.api.rest.output.UniProtMediaType.*;
import static org.uniprot.api.uniprotkb.utils.UniProtKBAsyncDownloadUtils.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Stream;

import lombok.extern.slf4j.Slf4j;

import org.apache.solr.client.solrj.SolrClient;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.client.AutoConfigureWebClient;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.web.client.RestTemplate;
import org.testcontainers.shaded.org.awaitility.Awaitility;
import org.uniprot.api.common.repository.solrstream.FacetTupleStreamTemplate;
import org.uniprot.api.common.repository.stream.common.TupleStreamTemplate;
import org.uniprot.api.common.repository.stream.store.uniprotkb.TaxonomyLineageRepository;
import org.uniprot.api.rest.controller.AbstractDownloadControllerIT;
import org.uniprot.api.rest.download.AsyncDownloadTestConfig;
import org.uniprot.api.rest.download.configuration.RedisConfiguration;
import org.uniprot.api.rest.download.model.DownloadJob;
import org.uniprot.api.rest.download.model.JobStatus;
import org.uniprot.api.rest.download.repository.DownloadJobRepository;
import org.uniprot.api.rest.output.PredefinedAPIStatus;
import org.uniprot.api.rest.output.UniProtMediaType;
import org.uniprot.api.rest.output.context.FileType;
import org.uniprot.api.rest.validation.ValidDownloadRequest;
import org.uniprot.api.rest.validation.error.ErrorHandlerConfig;
import org.uniprot.api.uniprotkb.UniProtKBREST;
import org.uniprot.api.uniprotkb.queue.UniProtKBMessageListener;
import org.uniprot.api.uniprotkb.repository.DataStoreTestConfig;
import org.uniprot.api.uniprotkb.repository.search.impl.UniprotQueryRepository;
import org.uniprot.core.uniprotkb.UniProtKBEntry;
import org.uniprot.store.datastore.UniProtStoreClient;
import org.uniprot.store.search.SolrCollection;

import com.jayway.jsonpath.JsonPath;

@Slf4j
@ActiveProfiles(profiles = {"offline", "asyncDownload"})
@WebMvcTest({UniProtKBDownloadController.class})
@ContextConfiguration(
        classes = {
            DataStoreTestConfig.class,
            UniProtKBREST.class,
            ErrorHandlerConfig.class,
            AsyncDownloadTestConfig.class,
            RedisConfiguration.class
        })
@ExtendWith(SpringExtension.class)
@AutoConfigureWebClient
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class UniProtKBDownloadControllerIT extends AbstractDownloadControllerIT {

    @Autowired private FacetTupleStreamTemplate facetTupleStreamTemplate;
    @Autowired private TupleStreamTemplate tupleStreamTemplate;
    @Autowired private DownloadJobRepository downloadJobRepository;
    @Autowired private UniprotQueryRepository uniprotQueryRepository;
    @Autowired private TaxonomyLineageRepository taxRepository;
    @Autowired private UniProtStoreClient<UniProtKBEntry> storeClient;
    @Autowired private MockMvc mockMvc;

    @Value("${async.download.embeddings.maxEntryCount}")
    private long maxEntryCount;

    @Autowired
    @Qualifier("uniProtKBSolrClient")
    private SolrClient solrClient;

    @MockBean(name = "uniProtRdfRestTemplate")
    private RestTemplate restTemplate;

    @BeforeAll
    public void runSaveEntriesInSolrAndStore() throws Exception {
        prepareDownloadFolders();
        saveEntriesInSolrAndStore(
                uniprotQueryRepository, cloudSolrClient, solrClient, storeClient, taxRepository);
    }

    @BeforeEach
    void setUpRestTemplate() {
        setUp(restTemplate);
    }

    @Test
    protected void runJobWithFieldsJsonAndVerify() throws Exception {
        // when
        String query = getQueryForJSONAndTSVRunJobWithFields();
        String fields = getQueryFieldsForJSONAndTSVRunJobWithFields();
        String jobId = callRunAPIAndVerify(query, fields, null, "json", false);
        // then
        Awaitility.await().until(() -> getDownloadJobRepository().existsById(jobId));
        Awaitility.await().until(isJobFinished(jobId));
        getAndVerifyDetails(jobId);
        String fileWithExt = jobId + FileType.GZIP.getExtension();
        Path resultFilePath = Path.of(this.resultFolder + "/" + fileWithExt);
        Assertions.assertTrue(Files.exists(resultFilePath));
        // uncompress the gz file
        Path unzippedFile = Path.of(this.resultFolder + "/" + jobId);
        uncompressFile(resultFilePath, unzippedFile);
        Assertions.assertTrue(Files.exists(unzippedFile));
        String resultsJson = Files.readString(unzippedFile);
        List<String> primaryAccessions = JsonPath.read(resultsJson, "$.results.*.primaryAccession");
        Assertions.assertEquals(12, primaryAccessions.size());
        List<String> uniProtkbIds = JsonPath.read(resultsJson, "$.results.*.uniProtkbId");
        Assertions.assertEquals(12, uniProtkbIds.size());
        List<String> genes = JsonPath.read(resultsJson, "$.results.*.genes");
        Assertions.assertEquals(12, genes.size());
        List<String> comments = JsonPath.read(resultsJson, "$.results.*.comments");
        Assertions.assertEquals(0, comments.size());
        List<String> organisms = JsonPath.read(resultsJson, "$.results.*.organism");
        Assertions.assertEquals(0, organisms.size());
    }

    @Test
    void getDetailsWithAbortedForH5Job() throws Exception {
        String jobId = UUID.randomUUID().toString();
        DownloadJob.DownloadJobBuilder builder = DownloadJob.builder();
        String errMsg =
                String.format(
                        UniProtKBMessageListener.H5_LIMIT_EXCEED_MSG,
                        this.maxEntryCount,
                        totalNonIsoformEntries);
        String query = "key:value";
        DownloadJob job =
                builder.id(jobId)
                        .query(query)
                        .status(JobStatus.ABORTED)
                        .error(errMsg)
                        .format(HDF5_MEDIA_TYPE_VALUE)
                        .build();
        DownloadJobRepository repo = getDownloadJobRepository();
        repo.save(job);
        await().until(() -> repo.existsById(jobId));

        ResultActions response = callGetJobDetails(jobId);

        // then
        response.andDo(log())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.query", is(query)))
                .andExpect(jsonPath("$.format", is(HDF5_MEDIA_TYPE_VALUE)))
                .andExpect(jsonPath("$.errors").exists())
                .andExpect(jsonPath("$.errors.length()", is(1)))
                .andExpect(
                        jsonPath(
                                "$.errors[0].code",
                                is(PredefinedAPIStatus.LIMIT_EXCEED_ERROR.getCode())))
                .andExpect(jsonPath("$.errors[0].message", is(errMsg)));
    }

    @Test
    protected void getStatusReturnsAborted() throws Exception {
        String jobId = UUID.randomUUID().toString();
        DownloadJob.DownloadJobBuilder builder = DownloadJob.builder();
        String errMsg =
                String.format(
                        UniProtKBMessageListener.H5_LIMIT_EXCEED_MSG,
                        this.maxEntryCount,
                        totalNonIsoformEntries);
        DownloadJob job =
                builder.id(jobId)
                        .status(JobStatus.ABORTED)
                        .error(errMsg)
                        .format(HDF5_MEDIA_TYPE_VALUE)
                        .build();
        DownloadJobRepository repo = getDownloadJobRepository();
        repo.save(job);
        Awaitility.await().until(() -> repo.existsById(jobId));

        ResultActions response = callGetJobStatus(jobId);

        // then
        response.andDo(log())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.jobStatus", is(JobStatus.ABORTED.toString())))
                .andExpect(jsonPath("$.errors").exists())
                .andExpect(jsonPath("$.errors.length()", is(1)))
                .andExpect(
                        jsonPath(
                                "$.errors[0].code",
                                is(PredefinedAPIStatus.LIMIT_EXCEED_ERROR.getCode())))
                .andExpect(jsonPath("$.errors[0].message", is(errMsg)));
    }

    @Test
    void submitJobWithTaxonomyReturnField() throws Exception {
        // when
        String query = "content:*";
        String fields = "accession,lineage";
        String jobId = callRunAPIAndVerify(query, fields, null, null, false);
        // then
        await().until(() -> getDownloadJobRepository().existsById(jobId));
        await().until(jobProcessed(jobId), equalTo(JobStatus.FINISHED));
        getAndVerifyDetails(jobId);
        // verify result file
        Path resultFilePath =
                Path.of(this.resultFolder + "/" + jobId + FileType.GZIP.getExtension());
        Assertions.assertTrue(Files.exists(resultFilePath));
        Path unzippedFile = Path.of(this.resultFolder + "/" + jobId);
        uncompressFile(resultFilePath, unzippedFile);
        Assertions.assertTrue(Files.exists(unzippedFile));
        String resultsJson = Files.readString(unzippedFile);
        List<String> primaryAccessions = JsonPath.read(resultsJson, "$.results.*.primaryAccession");
        Assertions.assertEquals(12, primaryAccessions.size());
        List<String> lineages = JsonPath.read(resultsJson, "$.results.*.lineages");
        Assertions.assertFalse(lineages.isEmpty());
        Assertions.assertEquals(12, lineages.size());
    }

    @Test
    void submitJobWithIncludeIsoformFlag() throws Exception {
        // when
        String query = "*:*";
        String fields = "accession";
        String jobId = callRunAPIAndVerify(query, fields, null, null, true);
        // then
        await().until(() -> getDownloadJobRepository().existsById(jobId));
        await().until(jobProcessed(jobId), equalTo(JobStatus.FINISHED));
        getAndVerifyDetails(jobId);
        // verify result file
        Path resultFilePath =
                Path.of(this.resultFolder + "/" + jobId + FileType.GZIP.getExtension());
        Assertions.assertTrue(Files.exists(resultFilePath));
        Path unzippedFile = Path.of(this.resultFolder + "/" + jobId);
        uncompressFile(resultFilePath, unzippedFile);
        Assertions.assertTrue(Files.exists(unzippedFile));
        String resultsJson = Files.readString(unzippedFile);
        List<String> primaryAccessions = JsonPath.read(resultsJson, "$.results.*.primaryAccession");
        Assertions.assertEquals(14, primaryAccessions.size());
        Assertions.assertTrue(primaryAccessions.contains("P00001"));
        Assertions.assertTrue(primaryAccessions.contains("P00011-2"));
        Assertions.assertTrue(primaryAccessions.contains("P00012-2"));
    }

    @ParameterizedTest(name = "[{index}] format {0}")
    @MethodSource("getFormatsWithoutProjection")
    void submitJobWithFieldsNotSupported(String format) throws Exception {
        // when
        String query = "*:*";
        String fields = "accession,rhea";
        ResultActions resultActions =
                callPostJobStatus(query, fields, null, format.toString(), false);
        resultActions
                .andDo(log())
                .andExpect(status().is(HttpStatus.BAD_REQUEST.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.url").exists())
                .andExpect(
                        jsonPath(
                                "$.messages",
                                contains("'fields' are not supported for 'format' " + format)));
    }

    @Test
    void submitJob_H5_Format_Success() throws Exception {
        String query = "reviewed:true";
        MediaType format = HDF5_MEDIA_TYPE;
        String jobId = callRunAPIAndVerify(query, null, null, format.toString(), false);
        await().until(() -> getDownloadJobRepository().existsById(jobId));
        await().until(jobProcessed(jobId), Matchers.equalTo(JobStatus.RUNNING));
        verifyIdsFile(jobId);
        // result file should not exist yet
        String fileWithExt = jobId + FileType.GZIP.getExtension();
        Path resultFilePath = Path.of(this.resultFolder + "/" + fileWithExt);
        Assertions.assertFalse(Files.exists(resultFilePath));
    }

    @Test
    void submitJob_H5_Format_Star_Query_Aborted() throws Exception {
        String query = "*:*";
        MediaType format = HDF5_MEDIA_TYPE;
        String jobId = callRunAPIAndVerify(query, null, null, format.toString(), false);
        await().until(() -> getDownloadJobRepository().existsById(jobId));
        await().until(isJobAborted(jobId));
        // id file should not exist yet
        Path resultFilePath = Path.of(this.idsFolder + "/" + jobId);
        Assertions.assertFalse(Files.exists(resultFilePath));
    }

    @Test
    void submitJob_H5_Format_With_Isoform_Aborted() throws Exception {
        String query = "reviewed:true AND gene:*";
        MediaType format = HDF5_MEDIA_TYPE;
        String jobId = callRunAPIAndVerify(query, null, null, format.toString(), true);
        await().until(() -> getDownloadJobRepository().existsById(jobId));
        await().until(isJobAborted(jobId));
        // id file should not exist yet
        Path resultFilePath = Path.of(this.idsFolder + "/" + jobId);
        Assertions.assertFalse(Files.exists(resultFilePath));
    }

    @Override
    protected ResultActions callPostJobStatus(
            String query, String fields, String sort, String format, boolean includeIsoform)
            throws Exception {
        MockHttpServletRequestBuilder requestBuilder =
                post(getDownloadAPIsBasePath() + "/run")
                        .header(ACCEPT, MediaType.APPLICATION_JSON)
                        .param("query", query)
                        .param("fields", fields)
                        .param("sort", sort)
                        .param("format", Objects.isNull(format) ? null : format)
                        .param("includeIsoform", String.valueOf(includeIsoform));
        ResultActions response = this.mockMvc.perform(requestBuilder);
        return response;
    }

    @Override
    protected Stream<Arguments> getSupportedFormats() {
        return List.of(
                        "xml",
                        "json",
                        TSV_MEDIA_TYPE_VALUE,
                        FF_MEDIA_TYPE_VALUE,
                        LIST_MEDIA_TYPE_VALUE,
                        APPLICATION_XML_VALUE,
                        APPLICATION_JSON_VALUE,
                        FASTA_MEDIA_TYPE_VALUE,
                        GFF_MEDIA_TYPE_VALUE,
                        RDF_MEDIA_TYPE_VALUE,
                        TURTLE_MEDIA_TYPE_VALUE,
                        N_TRIPLES_MEDIA_TYPE_VALUE,
                        HDF5_MEDIA_TYPE_VALUE,
                        "h5")
                .stream()
                .map(Arguments::of);
    }

    @Override
    protected void verifyIdsAndResultFiles(String jobId) throws IOException {
        verifyIdsFile(jobId);
        // verify result file
        String fileWithExt = jobId + FileType.GZIP.getExtension();
        Path resultFilePath = Path.of(this.resultFolder + "/" + fileWithExt);
        Assertions.assertTrue(Files.exists(resultFilePath));
        // uncompress the gz file
        Path unzippedFile = Path.of(this.resultFolder + "/" + jobId);
        uncompressFile(resultFilePath, unzippedFile);
        Assertions.assertTrue(Files.exists(unzippedFile));
        String resultsJson = Files.readString(unzippedFile);
        List<String> primaryAccessions = JsonPath.read(resultsJson, "$.results.*.primaryAccession");
        Assertions.assertTrue(
                List.of(
                                "P00001", "P00002", "P00003", "P00004", "P00005", "P00006",
                                "P00007", "P00008", "P00009", "P00010", "P00013", "P00014")
                        .containsAll(primaryAccessions));
    }

    @Override
    protected void verifyIdsFile(String jobId) throws IOException {
        // verify the ids file
        Path idsFilePath = Path.of(this.idsFolder + "/" + jobId);
        Assertions.assertTrue(Files.exists(idsFilePath));
        List<String> ids = Files.readAllLines(idsFilePath);
        Assertions.assertNotNull(ids);
        Assertions.assertTrue(
                ids.containsAll(
                        List.of(
                                "P00001", "P00002", "P00003", "P00004", "P00005", "P00006",
                                "P00007", "P00008", "P00009", "P00010")));
    }

    private Stream<Arguments> getFormatsWithoutProjection() {
        return ValidDownloadRequest.FORMATS_WITH_NO_PROJECTION.stream().map(Arguments::of);
    }

    @Override
    protected MockMvc getMockMvcObject() {
        return this.mockMvc;
    }

    @Override
    protected List<SolrCollection> getSolrCollections() {
        return List.of(SolrCollection.uniprot, SolrCollection.taxonomy);
    }

    @Override
    protected TupleStreamTemplate getTupleStreamTemplate() {
        return this.tupleStreamTemplate;
    }

    @Override
    protected FacetTupleStreamTemplate getFacetTupleStreamTemplate() {
        return this.facetTupleStreamTemplate;
    }

    protected String getDownloadAPIsBasePath() {
        return UniProtKBDownloadController.DOWNLOAD_RESOURCE;
    }

    @Override
    protected String submitJobWithQuery() {
        return "content:*";
    }

    @Override
    protected String submitJobWithSortFields() {
        return "accession desc";
    }

    @Override
    protected List<String> submitJobWithSortResultIds() {
        List<String> resultIds = new ArrayList<String>();
        resultIds.add("P00014");
        resultIds.add("P00013");
        resultIds.add("P00010");
        resultIds.add("P00009");
        resultIds.add("P00008");
        resultIds.add("P00007");
        resultIds.add("P00006");
        resultIds.add("P00005");
        resultIds.add("P00004");
        resultIds.add("P00003");
        resultIds.add("P00002");
        resultIds.add("P00001");
        return resultIds;
    }

    @Override
    protected String getSubmitJobAllFormatQuery() {
        return "reviewed:true";
    }

    @Override
    protected MediaType getUnsupportedFormat() {
        return UniProtMediaType.valueOf(XLS_MEDIA_TYPE_VALUE);
    }

    @Override
    protected String getUnsupportedFormatErrorMsg() {
        return "Invalid format received, 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet'. Expected one of [text/plain;format=tsv, application/json, text/plain;format=flatfile, text/plain;format=list, application/xml, text/plain;format=fasta, text/plain;format=gff, application/rdf+xml, text/turtle, application/n-triples, application/x-hdf5].";
    }

    @Override
    protected String getRunQueryWhichReturnsEmptyResult() {
        return "content:khansamatola";
    }

    @Override
    protected String getQueryForJSONAndTSVRunJobWithFields() {
        return "content:FGFR";
    }

    @Override
    protected String getQueryFieldsForJSONAndTSVRunJobWithFields() {
        return "accession,id,gene_names";
    }

    @Override
    protected String getRunJobHeaderWithFieldsTSV() {
        return "Entry\tEntry Name\tGene Names";
    }

    @Override
    protected String getResultIdStringToMatch() {
        return "$.results.*.primaryAccession";
    }

    protected DownloadJobRepository getDownloadJobRepository() {
        return this.downloadJobRepository;
    }
}
