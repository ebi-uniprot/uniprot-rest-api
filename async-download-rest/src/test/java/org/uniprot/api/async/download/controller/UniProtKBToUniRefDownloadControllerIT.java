package org.uniprot.api.async.download.controller;

import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.equalTo;
import static org.springframework.http.HttpHeaders.ACCEPT;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.autoconfigure.web.client.AutoConfigureWebClient;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.web.client.RestTemplate;
import org.uniprot.api.async.download.AsyncDownloadRestApp;
import org.uniprot.api.async.download.common.UniRefAsyncDownloadUtils;
import org.uniprot.api.async.download.messaging.config.common.RedisConfiguration;
import org.uniprot.api.common.repository.solrstream.FacetTupleStreamTemplate;
import org.uniprot.api.common.repository.stream.common.TupleStreamTemplate;
import org.uniprot.api.common.repository.stream.store.uniprotkb.TaxonomyLineageRepository;
import org.uniprot.api.idmapping.common.service.TestConfig;
import org.uniprot.api.rest.download.model.JobStatus;
import org.uniprot.api.rest.output.context.FileType;
import org.uniprot.api.rest.validation.error.ErrorHandlerConfig;
import org.uniprot.api.uniprotkb.common.repository.UniProtKBDataStoreTestConfig;
import org.uniprot.api.uniprotkb.common.repository.search.UniprotQueryRepository;
import org.uniprot.api.uniprotkb.common.utils.UniProtKBAsyncDownloadUtils;
import org.uniprot.api.uniref.common.repository.UniRefDataStoreTestConfig;
import org.uniprot.api.uniref.common.repository.search.UniRefQueryRepository;
import org.uniprot.core.uniprotkb.UniProtKBEntry;
import org.uniprot.core.uniref.UniRefEntryLight;
import org.uniprot.store.datastore.UniProtStoreClient;
import org.uniprot.store.search.SolrCollection;

import com.jayway.jsonpath.JsonPath;

@ActiveProfiles(profiles = {"offline", "idmapping"})
@WebMvcTest({UniProtKBToUniRefDownloadController.class})
@ContextConfiguration(
        classes = {
            TestConfig.class,
            UniRefDataStoreTestConfig.class,
            UniProtKBDataStoreTestConfig.class,
            AsyncDownloadRestApp.class,
            ErrorHandlerConfig.class,
            RedisConfiguration.class
        })
@ExtendWith(SpringExtension.class)
@AutoConfigureWebClient
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class UniProtKBToUniRefDownloadControllerIT extends MapDownloadControllerIT {

    @Qualifier("uniProtKBFacetTupleStreamTemplate")
    @Autowired
    private FacetTupleStreamTemplate uniProtKBFacetTupleStreamTemplate;

    @Autowired
    @Qualifier("uniProtKBTupleStream")
    private TupleStreamTemplate uniProtKBTupleStream;

    @Qualifier("uniProtStoreClient")
    @Autowired
    private UniProtStoreClient<UniProtKBEntry> uniProtKBSolrClient;

    @Qualifier("uniRefFacetTupleStreamTemplate")
    @Autowired
    private FacetTupleStreamTemplate uniRefFacetTupleStreamTemplate;

    @Autowired
    @Qualifier("uniRefTupleStreamTemplate")
    private TupleStreamTemplate uniRefTupleStreamTemplate;

    @Autowired private UniRefQueryRepository uniRefQueryRepository;
    @Autowired private UniprotQueryRepository uniprotQueryRepository;
    @Autowired private TaxonomyLineageRepository taxRepository;

    @Qualifier("uniRefLightStoreClient")
    @Autowired
    private UniProtStoreClient<UniRefEntryLight>
            uniRefStoreClient; // in memory voldemort store client

    @MockBean(name = "uniRefRdfRestTemplate")
    private RestTemplate restTemplate;

    @BeforeEach
    void setUp() {
        UniRefAsyncDownloadUtils.setUp(restTemplate);
    }

    @BeforeAll
    void beforeAll() throws Exception {
        startClusterForMapping(uniRefTupleStreamTemplate, uniRefFacetTupleStreamTemplate);
        initBeforeAll();
        UniProtKBAsyncDownloadUtils.saveEntriesInSolrAndStore(
                uniprotQueryRepository,
                cloudSolrClient,
                solrClient,
                uniProtKBSolrClient,
                taxRepository);
        UniRefAsyncDownloadUtils.saveEntriesInSolrAndStoreForMapping(
                uniRefQueryRepository,
                cloudSolrClient,
                solrClient,
                uniRefStoreClient,
                new String[] {"", "P00001", "P00005", " P00007", "P00010"});
    }

    @Override
    protected List<SolrCollection> getSolrCollections() {
        return List.of(SolrCollection.uniprot, SolrCollection.taxonomy, SolrCollection.uniref);
    }

    protected String getDownloadAPIsBasePath() {
        return UniProtKBToUniRefDownloadController.DOWNLOAD_RESOURCE;
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
    protected String getSubmitJobAllFormatQuery() {
        return "reviewed:true";
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
    protected TupleStreamTemplate getTupleStreamTemplate() {
        return this.uniProtKBTupleStream;
    }

    @Override
    protected FacetTupleStreamTemplate getFacetTupleStreamTemplate() {
        return this.uniProtKBFacetTupleStreamTemplate;
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
                        .param("to", "UniRef")
                        .param("format", Objects.isNull(format) ? null : format)
                        .param("includeIsoform", String.valueOf(includeIsoform))
                        .param("force", String.valueOf(force));
        return this.mockMvc.perform(requestBuilder);
    }

    @Test
    void runJobWithFieldsJsonAndVerify() throws Exception {
        // when
        String query = getQueryForJSONAndTSVRunJobWithFields();
        String fields = getQueryFieldsForJSONAndTSVRunJobWithFields();
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
}
