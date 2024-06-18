package org.uniprot.api.async.download.messaging.consumer.uniref;

import com.jayway.jsonpath.JsonPath;
import org.apache.solr.client.solrj.SolrClient;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.provider.Arguments;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.web.client.RestTemplate;
import org.uniprot.api.async.download.AsyncDownloadRestApp;
import org.uniprot.api.async.download.common.UniRefAsyncDownloadUtils;
import org.uniprot.api.async.download.controller.TestAsyncConfig;
import org.uniprot.api.async.download.controller.UniRefAsyncConfig;
import org.uniprot.api.async.download.messaging.config.common.RedisConfiguration;
import org.uniprot.api.async.download.messaging.config.uniref.UniRefDownloadConfigProperties;
import org.uniprot.api.async.download.messaging.consumer.SolrIdMessageConsumerIT;
import org.uniprot.api.async.download.messaging.repository.UniRefDownloadJobRepository;
import org.uniprot.api.async.download.messaging.result.uniref.UniRefAsyncDownloadFileHandler;
import org.uniprot.api.async.download.model.job.uniref.UniRefDownloadJob;
import org.uniprot.api.async.download.model.request.uniref.UniRefDownloadRequest;
import org.uniprot.api.common.repository.solrstream.FacetTupleStreamTemplate;
import org.uniprot.api.common.repository.stream.common.TupleStreamTemplate;
import org.uniprot.api.idmapping.common.service.TestConfig;
import org.uniprot.api.rest.download.model.JobStatus;
import org.uniprot.api.rest.output.context.FileType;
import org.uniprot.api.rest.validation.error.ErrorHandlerConfig;
import org.uniprot.api.uniprotkb.common.repository.UniProtKBDataStoreTestConfig;
import org.uniprot.api.uniref.common.repository.UniRefDataStoreTestConfig;
import org.uniprot.api.uniref.common.repository.search.UniRefQueryRepository;
import org.uniprot.core.uniref.UniRefEntryLight;
import org.uniprot.store.datastore.UniProtStoreClient;
import org.uniprot.store.search.SolrCollection;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.uniprot.api.rest.download.model.JobStatus.FINISHED;
import static org.uniprot.api.rest.output.UniProtMediaType.*;

@ActiveProfiles(profiles = {"offline", "idmapping"})
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
@WebAppConfiguration
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class UniRefMessageConsumerIT
        extends SolrIdMessageConsumerIT<UniRefDownloadRequest, UniRefDownloadJob> {
    @Autowired
    private UniRefContentBasedAndRetriableMessageConsumer uniRefMessageConsumer;
    @Autowired
    private UniRefQueryRepository uniRefQueryRepository;
    @Autowired
    private SolrClient solrClient;
    @Autowired()
    @Qualifier("uniRefLightStoreClient")
    private UniProtStoreClient<UniRefEntryLight> storeClient;
    @Autowired
    private UniRefAsyncConfig uniRefAsyncConfig;
    @Autowired
    private UniRefDownloadJobRepository uniRefDownloadJobRepository;
    @Autowired
    private UniRefDownloadConfigProperties uniRefDownloadConfigProperties;
    @Autowired
    private UniRefAsyncDownloadFileHandler uniRefAsyncDownloadFileHandler;
    @Qualifier("uniRefFacetTupleStreamTemplate")
    @Autowired
    private FacetTupleStreamTemplate facetTupleStreamTemplate;
    @Autowired
    @Qualifier("uniRefTupleStreamTemplate")
    private TupleStreamTemplate tupleStreamTemplate;
    @MockBean(name = "uniRefRdfRestTemplate")
    private RestTemplate restTemplate;

    @BeforeAll
    void beforeAll() throws Exception {
        prepareDownloadFolders();
        UniRefAsyncDownloadUtils.saveEntriesInSolrAndStore(
                uniRefQueryRepository, cloudSolrClient, solrClient, storeClient);
    }

    @BeforeEach
    void setUp() {
        asyncDownloadFileHandler = uniRefAsyncDownloadFileHandler;
        downloadJobRepository = uniRefDownloadJobRepository;
        messageConsumer = uniRefMessageConsumer;
        downloadConfigProperties = uniRefDownloadConfigProperties;
        downloadRequest = new UniRefDownloadRequest();
        downloadRequest.setFormat("json");
        downloadRequest.setQuery("Human");
        downloadRequest.setFields("id,name,organism");
        UniRefAsyncDownloadUtils.setUp(restTemplate);
    }

    @Override
    protected TestAsyncConfig getTestAsyncConfig() {
        return uniRefAsyncConfig;
    }

    @Override
    protected List<SolrCollection> getSolrCollections() {
        return List.of(SolrCollection.uniref);
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
    protected void verifyIdsFiles(String id) throws Exception {
        Path idsFilePath = Path.of(getTestAsyncConfig().getIdsFolder() + "/" + id);
        assertTrue(Files.exists(idsFilePath));
        List<String> ids = Files.readAllLines(idsFilePath);
        assertNotNull(ids);
        assertTrue(
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
    protected void verifyResultFile(String id) throws Exception {
        String fileWithExt = id + FileType.GZIP.getExtension();
        Path resultFilePath = Path.of(getTestAsyncConfig().getResultFolder() + "/" + fileWithExt);
        assertTrue(Files.exists(resultFilePath));
        // uncompress the gz file
        Path unzippedFile = Path.of(getTestAsyncConfig().getResultFolder() + "/" + id);
        uncompressFile(resultFilePath, unzippedFile);
        assertTrue(Files.exists(unzippedFile));
        String resultsJson = Files.readString(unzippedFile);
        List<String> ids = JsonPath.read(resultsJson, "$.results.*.id");
        assertEquals(12, ids.size());
        List<String> uniRefIds = JsonPath.read(resultsJson, "$.results.*.name");
        assertEquals(12, uniRefIds.size());
        List<String> representativeMember =
                JsonPath.read(resultsJson, "$.results.*.representativeMember");
        assertEquals(12, representativeMember.size());
        List<String> members = JsonPath.read(resultsJson, "$.results.*.members");
        assertEquals(12, members.size());
        List<String> organisms = JsonPath.read(resultsJson, "$.results.*.organisms");
        assertEquals(12, organisms.size());
    }

    @Override
    protected void assertJobSpecifics(UniRefDownloadJob job, String format) {
        assertEquals(12, job.getProcessedEntries());
        assertEquals(FINISHED, job.getStatus());
        assertEquals(Objects.equals(format, LIST_MEDIA_TYPE_VALUE) ? 12 : 7, job.getUpdateCount());
    }

    @Override
    protected void saveDownloadJob(String id, int retryCount, JobStatus jobStatus, long updateCount, long processedEntries) {
        uniRefDownloadJobRepository.save(
                UniRefDownloadJob.builder().id(id).status(jobStatus).updateCount(updateCount)
                        .processedEntries(processedEntries)
                        .retried(retryCount).build());
        System.out.println();
    }

    @Override
    protected Stream<Arguments> getSupportedFormats() {
        return Stream.of(
                        "json",
                        FASTA_MEDIA_TYPE_VALUE,
                        TSV_MEDIA_TYPE_VALUE,
                        APPLICATION_JSON_VALUE,
                        XLS_MEDIA_TYPE_VALUE,
                        LIST_MEDIA_TYPE_VALUE,
                        RDF_MEDIA_TYPE_VALUE,
                        TURTLE_MEDIA_TYPE_VALUE,
                        N_TRIPLES_MEDIA_TYPE_VALUE)
                .map(Arguments::of);
    }
}
