package org.uniprot.api.async.download.controller;

import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.equalTo;
import static org.springframework.http.HttpHeaders.ACCEPT;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.uniprot.api.rest.output.UniProtMediaType.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

import org.apache.solr.client.solrj.SolrClient;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.provider.Arguments;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.autoconfigure.web.client.AutoConfigureWebClient;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.web.client.RestTemplate;
import org.uniprot.api.async.download.AsyncDownloadRestApp;
import org.uniprot.api.async.download.messaging.config.common.RedisConfiguration;
import org.uniprot.api.async.download.messaging.repository.DownloadJobRepository;
import org.uniprot.api.async.download.messaging.repository.UniRefDownloadJobRepository;
import org.uniprot.api.async.download.model.job.DownloadJob;
import org.uniprot.api.async.download.model.job.uniref.UniRefDownloadJob;
import org.uniprot.api.common.repository.solrstream.FacetTupleStreamTemplate;
import org.uniprot.api.common.repository.stream.common.TupleStreamTemplate;
import org.uniprot.api.rest.controller.ControllerITUtils;
import org.uniprot.api.rest.download.model.JobStatus;
import org.uniprot.api.rest.output.UniProtMediaType;
import org.uniprot.api.rest.output.context.FileType;
import org.uniprot.api.rest.validation.error.ErrorHandlerConfig;
import org.uniprot.api.uniprotkb.common.repository.UniProtKBDataStoreTestConfig;
import org.uniprot.api.uniref.common.repository.UniRefDataStoreTestConfig;
import org.uniprot.api.uniref.common.repository.search.UniRefQueryRepository;
import org.uniprot.api.uniref.common.util.UniRefAsyncDownloadUtils;
import org.uniprot.core.uniref.UniRefEntryLight;
import org.uniprot.store.datastore.UniProtStoreClient;
import org.uniprot.store.search.SolrCollection;

import com.jayway.jsonpath.JsonPath;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@ActiveProfiles(profiles = {"offline", "idmapping"})
@WebMvcTest({UniRefDownloadController.class})
@ContextConfiguration(
        classes = {
            UniRefDataStoreTestConfig.class,
            UniProtKBDataStoreTestConfig.class,
            AsyncDownloadRestApp.class,
            ErrorHandlerConfig.class,
            RedisConfiguration.class
        })
@ExtendWith(SpringExtension.class)
@AutoConfigureWebClient
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class UniRefDownloadControllerIT extends AbstractDownloadControllerIT {

    @Autowired private UniRefAsyncConfig uniRefAsyncConfig;

    @Qualifier("uniRefFacetTupleStreamTemplate")
    @Autowired
    private FacetTupleStreamTemplate facetTupleStreamTemplate;

    @Autowired
    @Qualifier("uniRefTupleStreamTemplate")
    private TupleStreamTemplate tupleStreamTemplate;

    @Autowired private UniRefDownloadJobRepository downloadJobRepository;
    @Autowired private UniRefQueryRepository uniRefQueryRepository;
    @Autowired private SolrClient solrClient;

    @Qualifier("uniRefLightStoreClient")
    @Autowired
    private UniProtStoreClient<UniRefEntryLight> storeClient; // in memory voldemort store client

    @MockBean(name = "uniRefRdfRestTemplate")
    private RestTemplate restTemplate;

    @Autowired private MockMvc mockMvc;

    @BeforeAll
    public void runSaveEntriesInSolrAndStore() throws Exception {
        prepareDownloadFolders();
        UniRefAsyncDownloadUtils.saveEntriesInSolrAndStore(
                uniRefQueryRepository, cloudSolrClient, solrClient, storeClient, 4);
    }

    @BeforeEach
    void setUpRestTemplate() {
        ControllerITUtils.mockRestTemplateResponsesForRDFFormats(restTemplate, "uniref");
    }

    @Test
    void runJobWithFieldsJsonAndVerify() throws Exception {
        // when
        String query = "Human";
        String fields = "id,name,organism";
        String jobId = callRunAPIAndVerify(query, fields, null, "json", false);
        // then
        await().until(() -> getDownloadJobRepository().existsById(jobId));
        await().until(jobProcessed(jobId), equalTo(JobStatus.FINISHED));
        getAndVerifyDetails(jobId);
        String fileWithExt = jobId + FileType.GZIP.getExtension();
        Path resultFilePath = Path.of(getTestAsyncConfig().getResultFolder() + "/" + fileWithExt);
        Assertions.assertTrue(Files.exists(resultFilePath));
        // uncompress the gz file
        Path unzippedFile = Path.of(getTestAsyncConfig().getResultFolder() + "/" + jobId);
        uncompressFile(resultFilePath, unzippedFile);
        Assertions.assertTrue(Files.exists(unzippedFile));
        String resultsJson = Files.readString(unzippedFile);
        List<String> ids = JsonPath.read(resultsJson, "$.results.*.id");
        Assertions.assertEquals(12, ids.size());
        List<String> uniRefIds = JsonPath.read(resultsJson, "$.results.*.name");
        Assertions.assertEquals(12, uniRefIds.size());
        List<String> representativeMember =
                JsonPath.read(resultsJson, "$.results.*.representativeMember");
        Assertions.assertEquals(12, representativeMember.size());
        List<String> members = JsonPath.read(resultsJson, "$.results.*.members");
        Assertions.assertEquals(12, members.size());
        List<String> organisms = JsonPath.read(resultsJson, "$.results.*.organisms");
        Assertions.assertEquals(12, organisms.size());
    }

    @Override
    protected MockMvc getMockMvcObject() {
        return this.mockMvc;
    }

    @Override
    protected ResultActions callPostJobStatus(
            String query,
            String fields,
            String sort,
            String format,
            boolean includeIsoform,
            boolean force)
            throws Exception {
        MockHttpServletRequestBuilder requestBuilder =
                post(getDownloadAPIsBasePath() + "/run")
                        .header(ACCEPT, MediaType.APPLICATION_JSON)
                        .param("query", query)
                        .param("fields", fields)
                        .param("sort", sort)
                        .param("format", Objects.isNull(format) ? null : format)
                        .param("force", String.valueOf(force));
        ResultActions response = this.mockMvc.perform(requestBuilder);
        return response;
    }

    @Override
    protected void verifyIdsAndResultFiles(String jobId) throws IOException {
        verifyIdsFile(jobId);
        // verify result file
        String fileWithExt = jobId + FileType.GZIP.getExtension();
        Path resultFilePath = Path.of(getTestAsyncConfig().getResultFolder() + "/" + fileWithExt);
        Assertions.assertTrue(Files.exists(resultFilePath));
        // uncompress the gz file
        Path unzippedFile = Path.of(getTestAsyncConfig().getResultFolder() + "/" + jobId);
        uncompressFile(resultFilePath, unzippedFile);
        Assertions.assertTrue(Files.exists(unzippedFile));
        String resultsJson = Files.readString(unzippedFile);
        List<String> ids = JsonPath.read(resultsJson, "$.results.*.id");
        Assertions.assertTrue(
                List.of(
                                "UniRef100_P03901",
                                "UniRef100_P03902",
                                "UniRef100_P03903",
                                "UniRef100_P03904",
                                "UniRef50_P03901",
                                "UniRef50_P03902",
                                "UniRef50_P03903",
                                "UniRef50_P03904",
                                "UniRef90_P03901",
                                "UniRef90_P03902",
                                "UniRef90_P03903",
                                "UniRef90_P03904")
                        .containsAll(ids));
    }

    @Override
    protected void verifyIdsFile(String jobId) throws IOException {
        // verify the ids file
        Path idsFilePath = Path.of(getTestAsyncConfig().getIdsFolder() + "/" + jobId);
        Assertions.assertTrue(Files.exists(idsFilePath));
        List<String> ids = Files.readAllLines(idsFilePath);
        Assertions.assertNotNull(ids);
        Assertions.assertTrue(
                ids.containsAll(
                        List.of(
                                "UniRef100_P03901",
                                "UniRef100_P03902",
                                "UniRef100_P03903",
                                "UniRef100_P03904",
                                "UniRef50_P03901",
                                "UniRef50_P03902",
                                "UniRef50_P03903",
                                "UniRef50_P03904",
                                "UniRef90_P03901",
                                "UniRef90_P03902",
                                "UniRef90_P03903",
                                "UniRef90_P03904")));
    }

    @Override
    protected Stream<Arguments> getSupportedFormats() {
        return List.of(
                        "json",
                        FASTA_MEDIA_TYPE_VALUE,
                        TSV_MEDIA_TYPE_VALUE,
                        APPLICATION_JSON_VALUE,
                        XLS_MEDIA_TYPE_VALUE,
                        LIST_MEDIA_TYPE_VALUE,
                        RDF_MEDIA_TYPE_VALUE,
                        TURTLE_MEDIA_TYPE_VALUE,
                        N_TRIPLES_MEDIA_TYPE_VALUE)
                .stream()
                .map(Arguments::of);
    }

    @Override
    protected List<SolrCollection> getSolrCollections() {
        return List.of(SolrCollection.uniref);
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
        return UniRefDownloadController.DOWNLOAD_RESOURCE;
    }

    @Override
    protected String submitJobWithQuery() {
        return "identity:*";
    }

    @Override
    protected String submitJobWithSortFields() {
        return "id desc";
    }

    @Override
    protected List<String> submitJobWithSortResultIds() {
        List<String> resultIds = new ArrayList<String>();
        resultIds.add("UniRef90_P03904");
        resultIds.add("UniRef90_P03903");
        resultIds.add("UniRef90_P03902");
        resultIds.add("UniRef90_P03901");
        resultIds.add("UniRef50_P03904");
        resultIds.add("UniRef50_P03903");
        resultIds.add("UniRef50_P03902");
        resultIds.add("UniRef50_P03901");
        resultIds.add("UniRef100_P03904");
        resultIds.add("UniRef100_P03903");
        resultIds.add("UniRef100_P03902");
        resultIds.add("UniRef100_P03901");
        return resultIds;
    }

    @Override
    protected String getSubmitJobAllFormatQuery() {
        return "Human";
    }

    @Override
    protected MediaType getUnsupportedFormat() {
        return UniProtMediaType.valueOf(OBO_MEDIA_TYPE_VALUE);
    }

    @Override
    protected String getUnsupportedFormatErrorMsg() {
        return "Invalid format received, 'text/plain;format=obo'. Expected one of [text/plain;format=fasta, text/plain;format=tsv, application/json, application/vnd.openxmlformats-officedocument.spreadsheetml.sheet, text/plain;format=list, application/rdf+xml, text/turtle, application/n-triples].";
    }

    @Override
    protected String getRunQueryWhichReturnsEmptyResult() {
        return "identity:noid";
    }

    @Override
    protected String getQueryForJSONAndTSVRunJobWithFields() {
        return "Human";
    }

    @Override
    protected String getQueryFieldsForJSONAndTSVRunJobWithFields() {
        return "id,name,organism";
    }

    @Override
    protected String getRunJobHeaderWithFieldsTSV() {
        return "Cluster ID\tCluster Name\tOrganisms";
    }

    @Override
    protected String getResultIdStringToMatch() {
        return "$.results.*.id";
    }

    @Override
    protected DownloadJob getDownloadJob(
            String jobId,
            String errMsg,
            String query,
            String sort,
            String fields,
            JobStatus jobStatus,
            String format,
            int retried) {
        UniRefDownloadJob.UniRefDownloadJobBuilder builder = UniRefDownloadJob.builder();
        UniRefDownloadJob job =
                builder.id(jobId)
                        .status(jobStatus)
                        .error(errMsg)
                        .format(format)
                        .query(query)
                        .sort(sort)
                        .fields(fields)
                        .retried(retried)
                        .build();
        return job;
    }

    protected DownloadJobRepository getDownloadJobRepository() {
        return this.downloadJobRepository;
    }

    @Override
    protected TestAsyncConfig getTestAsyncConfig() {
        return uniRefAsyncConfig;
    }
}
