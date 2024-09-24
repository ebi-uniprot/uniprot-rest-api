package org.uniprot.api.async.download.controller;

import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.http.HttpHeaders.ACCEPT;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.uniprot.api.rest.output.UniProtMediaType.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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
import org.uniprot.api.async.download.common.UniParcAsyncDownloadUtils;
import org.uniprot.api.async.download.messaging.config.common.RedisConfiguration;
import org.uniprot.api.async.download.messaging.repository.DownloadJobRepository;
import org.uniprot.api.async.download.messaging.repository.UniParcDownloadJobRepository;
import org.uniprot.api.async.download.model.job.DownloadJob;
import org.uniprot.api.async.download.model.job.uniparc.UniParcDownloadJob;
import org.uniprot.api.common.repository.solrstream.FacetTupleStreamTemplate;
import org.uniprot.api.common.repository.stream.common.TupleStreamTemplate;
import org.uniprot.api.idmapping.common.service.TestConfig;
import org.uniprot.api.rest.download.model.JobStatus;
import org.uniprot.api.rest.output.UniProtMediaType;
import org.uniprot.api.rest.output.context.FileType;
import org.uniprot.api.rest.validation.error.ErrorHandlerConfig;
import org.uniprot.api.uniparc.common.repository.UniParcDataStoreTestConfig;
import org.uniprot.api.uniparc.common.repository.search.UniParcQueryRepository;
import org.uniprot.api.uniparc.common.repository.store.crossref.UniParcCrossReferenceStoreClient;
import org.uniprot.api.uniprotkb.common.repository.UniProtKBDataStoreTestConfig;
import org.uniprot.core.uniparc.UniParcEntryLight;
import org.uniprot.store.datastore.UniProtStoreClient;
import org.uniprot.store.search.SolrCollection;

import com.jayway.jsonpath.JsonPath;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@ActiveProfiles(profiles = {"offline", "idmapping"})
@WebMvcTest({UniParcDownloadController.class})
@ContextConfiguration(
        classes = {
            TestConfig.class,
            UniParcDataStoreTestConfig.class,
            UniProtKBDataStoreTestConfig.class,
            AsyncDownloadRestApp.class,
            ErrorHandlerConfig.class,
            RedisConfiguration.class
        })
@ExtendWith(SpringExtension.class)
@AutoConfigureWebClient
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class UniParcDownloadControllerIT extends AbstractDownloadControllerIT {

    @Autowired private UniParcAsyncConfig uniParcAsyncConfig;

    @Qualifier("uniParcFacetTupleStreamTemplate")
    @Autowired
    private FacetTupleStreamTemplate facetTupleStreamTemplate;

    @Autowired
    @Qualifier("uniParcTupleStreamTemplate")
    private TupleStreamTemplate tupleStreamTemplate;

    @Autowired private UniParcDownloadJobRepository downloadJobRepository;
    @Autowired private UniParcQueryRepository uniParcQueryRepository;
    @Autowired private SolrClient solrClient;

    @Autowired
    private UniProtStoreClient<UniParcEntryLight>
            uniParcLightStoreClient; // in memory voldemort store client

    @Autowired private UniParcCrossReferenceStoreClient uniParcCrossReferenceStoreClient;

    @MockBean(name = "uniParcRdfRestTemplate")
    private RestTemplate restTemplate;

    @Autowired private MockMvc mockMvc;

    @BeforeAll
    public void runSaveEntriesInSolrAndStore() throws Exception {
        prepareDownloadFolders();
        UniParcAsyncDownloadUtils.saveEntriesInSolrAndStore(
                uniParcQueryRepository,
                cloudSolrClient,
                solrClient,
                uniParcLightStoreClient,
                uniParcCrossReferenceStoreClient);
    }

    @BeforeEach
    void setUpRestTemplate() {
        UniParcAsyncDownloadUtils.setUp(restTemplate);
    }

    @Test
    void runJobWithFieldsJsonAndVerify() throws Exception {
        // when
        String query = "Human";
        String fields = "upi,length,organism";
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
        List<String> ids = JsonPath.read(resultsJson, "$.results.*.uniParcId");
        assertEquals(12, ids.size());
        List<Map<String, Integer>> sequences = JsonPath.read(resultsJson, "$.results.*.sequence");
        sequences.forEach(s -> assertTrue(s.containsKey("length")));
        assertEquals(12, sequences.size());
        List<String> mostRecentCrossRefUpdated =
                JsonPath.read(resultsJson, "$.results.*.mostRecentCrossRefUpdated");
        assertEquals(12, mostRecentCrossRefUpdated.size());
        List<String> oldestCrossRefCreated =
                JsonPath.read(resultsJson, "$.results.*.oldestCrossRefCreated");
        assertEquals(12, oldestCrossRefCreated.size());
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
                                "upi101", "upi202", "upi303", "upi404", "upi505", "upi606",
                                "upi707", "upi808", "upi909", "upi1010", "upi1111", "upi1212")
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
                                "upi101", "upi202", "upi303", "upi404", "upi505", "upi606",
                                "upi707", "upi808", "upi909", "upi1010", "upi1111", "upi1212")));
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
        return List.of(SolrCollection.uniparc);
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
        return UniParcDownloadController.DOWNLOAD_RESOURCE;
    }

    @Override
    protected String submitJobWithQuery() {
        return "uniparc:*";
    }

    @Override
    protected String submitJobWithSortFields() {
        return "length desc";
    }

    @Override
    protected List<String> submitJobWithSortResultIds() {
        List<String> resultIds = new ArrayList<String>();
        resultIds.add("upi101");
        resultIds.add("upi202");
        resultIds.add("upi303");
        resultIds.add("upi404");
        resultIds.add("upi505");
        resultIds.add("upi606");
        resultIds.add("upi707");
        resultIds.add("upi808");
        resultIds.add("upi909");
        resultIds.add("upi1010");
        resultIds.add("upi1111");
        resultIds.add("upi1212");
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
        return "Invalid format received, 'text/plain;format=obo'. Expected one of [text/plain;format=fasta, text/plain;format=tsv, application/json, application/vnd.openxmlformats-officedocument.spreadsheetml.sheet, text/plain;format=list, application/rdf+xml, text/turtle, application/n-triples, application/xml].";
    }

    @Override
    protected String getRunQueryWhichReturnsEmptyResult() {
        return "uniparc:noid";
    }

    @Override
    protected String getQueryForJSONAndTSVRunJobWithFields() {
        return "Human";
    }

    @Override
    protected String getQueryFieldsForJSONAndTSVRunJobWithFields() {
        return "upi,length,organism";
    }

    @Override
    protected String getRunJobHeaderWithFieldsTSV() {
        return "Entry\tLength\tOrganisms";
    }

    @Override
    protected String getResultIdStringToMatch() {
        return "$.results.*.uniParcId";
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
        UniParcDownloadJob.UniParcDownloadJobBuilder builder = UniParcDownloadJob.builder();
        UniParcDownloadJob job =
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
        return uniParcAsyncConfig;
    }
}
