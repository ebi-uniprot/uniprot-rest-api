package org.uniprot.api.uniprotkb.controller;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import lombok.extern.slf4j.Slf4j;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.client.AutoConfigureWebClient;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.uniprot.api.common.repository.solrstream.FacetTupleStreamTemplate;
import org.uniprot.api.common.repository.stream.common.TupleStreamTemplate;
import org.uniprot.api.rest.download.MessageQueueTestConfig;
import org.uniprot.api.rest.download.configuration.RedisConfiguration;
import org.uniprot.api.rest.validation.error.ErrorHandlerConfig;
import org.uniprot.api.uniprotkb.UniProtKBREST;
import org.uniprot.api.uniprotkb.repository.DataStoreTestConfig;
import org.uniprot.store.search.SolrCollection;

import com.jayway.jsonpath.JsonPath;

@Slf4j
@ActiveProfiles(profiles = "offline")
@WebMvcTest({UniProtKBDownloadController.class})
@ContextConfiguration(
        classes = {
            DataStoreTestConfig.class,
            UniProtKBREST.class,
            ErrorHandlerConfig.class,
            MessageQueueTestConfig.class,
            RedisConfiguration.class
        })
@ExtendWith(SpringExtension.class)
@AutoConfigureWebClient
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class UniProtKBDownloadControllerIT extends AbstractDownloadControllerIT {
    // TODO currently we manually need to create test ids and result folders
    // download.idFilesFolder=target/download/ids
    // download.resultFilesFolder=target/download/result
    // ideally they  should be created on  start of application if not already created
    @Autowired private FacetTupleStreamTemplate facetTupleStreamTemplate;
    @Autowired private TupleStreamTemplate tupleStreamTemplate;

    @Override
    protected void verifyIdsAndResultFiles(String jobId) throws IOException {
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
        // verify result file
        Path resultFilePath = Path.of(this.resultFolder + "/" + jobId);
        Assertions.assertTrue(Files.exists(resultFilePath));
        String resultsJson = Files.readString(resultFilePath);
        List<String> primaryAccessions = JsonPath.read(resultsJson, "$.results.*.primaryAccession");
        Assertions.assertTrue(
                List.of(
                                "P00001", "P00002", "P00003", "P00004", "P00005", "P00006",
                                "P00007", "P00008", "P00009", "P00010")
                        .containsAll(primaryAccessions));
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

    @Override
    protected void getAndVerifyDetails(String query) {
        // TODO  implement once details endpoint is created
    }

    protected String getDownloadAPIsBasePath() {
        return UniProtKBDownloadController.DOWNLOAD_RESOURCE;
    }
}
