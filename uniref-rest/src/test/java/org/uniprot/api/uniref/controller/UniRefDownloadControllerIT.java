package org.uniprot.api.uniref.controller;

import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.is;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.log;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.uniprot.api.uniref.controller.TestUtils.uncompressFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

import lombok.extern.slf4j.Slf4j;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.client.AutoConfigureWebClient;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.ResultActions;
import org.uniprot.api.common.repository.solrstream.FacetTupleStreamTemplate;
import org.uniprot.api.common.repository.stream.common.TupleStreamTemplate;
import org.uniprot.api.rest.download.AsyncDownloadTestConfig;
import org.uniprot.api.rest.download.configuration.RedisConfiguration;
import org.uniprot.api.rest.download.model.DownloadJob;
import org.uniprot.api.rest.download.model.JobStatus;
import org.uniprot.api.rest.download.repository.DownloadJobRepository;
import org.uniprot.api.rest.output.context.FileType;
import org.uniprot.api.rest.validation.error.ErrorHandlerConfig;
import org.uniprot.api.uniref.repository.DataStoreTestConfig;
import org.uniprot.store.search.SolrCollection;

import com.jayway.jsonpath.JsonPath;

@Slf4j
@ActiveProfiles(profiles = {"offline", "asyncDownload"})
@WebMvcTest({UniRefDownloadController.class})
@ContextConfiguration(
        classes = {
            DataStoreTestConfig.class,
            ErrorHandlerConfig.class,
            AsyncDownloadTestConfig.class,
            RedisConfiguration.class
        })
@ExtendWith(SpringExtension.class)
@AutoConfigureWebClient
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class UniRefDownloadControllerIT extends AbstractDownloadControllerIT {
    @Autowired private FacetTupleStreamTemplate facetTupleStreamTemplate;
    @Autowired private TupleStreamTemplate tupleStreamTemplate;

    @Autowired private DownloadJobRepository downloadJobRepository;

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

    @ParameterizedTest
    @EnumSource(
            value = JobStatus.class,
            names = {"PROCESSING", "UNFINISHED"})
    void statusInProcessingOrUnFinishedReturnsRunning(JobStatus status) throws Exception {
        String jobId = UUID.randomUUID().toString();
        DownloadJob.DownloadJobBuilder builder = DownloadJob.builder();
        DownloadJob job = builder.id(jobId).status(status).build();
        DownloadJobRepository repo = getDownloadJobRepository();
        repo.save(job);
        await().until(() -> repo.existsById(jobId));

        ResultActions response = callGetJobStatus(jobId);

        // then
        response.andDo(log())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.jobStatus", is(JobStatus.RUNNING.toString())))
                .andExpect(jsonPath("$.errors").doesNotExist());
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
        return UniRefDownloadController.DOWNLOAD_RESOURCE;
    }

    protected DownloadJobRepository getDownloadJobRepository() {
        return this.downloadJobRepository;
    }
}
