package org.uniprot.api.uniprotkb.queue;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;
import static org.testcontainers.shaded.org.awaitility.Awaitility.await;
import static org.uniprot.api.rest.download.queue.RedisUtil.*;
import static org.uniprot.api.rest.output.UniProtMediaType.HDF5_MEDIA_TYPE;
import static org.uniprot.api.uniprotkb.utils.UniProtKBAsyncDownloadUtils.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Stream;

import org.apache.solr.client.solrj.SolrClient;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.provider.Arguments;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.autoconfigure.web.client.AutoConfigureWebClient;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.context.annotation.PropertySource;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.web.client.RestTemplate;
import org.uniprot.api.common.repository.stream.store.uniprotkb.TaxonomyLineageRepository;
import org.uniprot.api.rest.controller.AbstractAsyncDownloadIT;
import org.uniprot.api.rest.download.AsyncDownloadTestConfig;
import org.uniprot.api.rest.download.configuration.RedisConfiguration;
import org.uniprot.api.rest.download.model.DownloadJob;
import org.uniprot.api.rest.download.model.JobStatus;
import org.uniprot.api.rest.download.queue.BaseAbstractMessageListener;
import org.uniprot.api.rest.output.UniProtMediaType;
import org.uniprot.api.rest.output.context.FileType;
import org.uniprot.api.rest.request.DownloadRequest;
import org.uniprot.api.uniprotkb.UniProtKBREST;
import org.uniprot.api.uniprotkb.controller.UniProtKBDownloadController;
import org.uniprot.api.uniprotkb.controller.request.UniProtKBDownloadRequest;
import org.uniprot.api.uniprotkb.repository.DataStoreTestConfig;
import org.uniprot.api.uniprotkb.repository.search.impl.UniprotQueryRepository;
import org.uniprot.api.uniprotkb.repository.store.UniProtStoreConfig;
import org.uniprot.core.uniprotkb.UniProtKBEntry;
import org.uniprot.store.datastore.UniProtStoreClient;
import org.uniprot.store.indexer.DataStoreManager;
import org.uniprot.store.indexer.uniprotkb.processor.InactiveEntryConverter;
import org.uniprot.store.search.SolrCollection;
import org.uniprot.store.spark.indexer.uniprot.converter.UniProtEntryConverter;

@ActiveProfiles(profiles = {"offline", "asyncDownload", "integration"})
@EnableConfigurationProperties
@PropertySource("classpath:application.properties")
@ContextConfiguration(
        classes = {
            DataStoreTestConfig.class,
            UniProtKBREST.class,
            UniProtStoreConfig.class,
            AsyncDownloadTestConfig.class,
            RedisConfiguration.class,
            EmbeddingsTestConsumer.class
        })
@ExtendWith(SpringExtension.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@WebMvcTest({UniProtKBDownloadController.class})
@AutoConfigureWebClient
public class UniProtKBAsyncDownloadIT extends AbstractAsyncDownloadIT {

    @Value("${async.download.embeddings.maxEntryCount}")
    protected long maxEntryCount;

    @SpyBean private UniProtKBMessageListener uniProtKBMessageListener;

    @Autowired private UniprotQueryRepository uniprotQueryRepository;

    @RegisterExtension private static DataStoreManager storeManager = new DataStoreManager();

    @Autowired
    @Qualifier("uniProtKBSolrClient")
    private SolrClient solrClient;

    @Autowired private TaxonomyLineageRepository taxRepository;

    @Autowired
    private UniProtStoreClient<UniProtKBEntry> storeClient; // in memory voldemort store client

    @MockBean(name = "uniProtRdfRestTemplate")
    private RestTemplate restTemplate;

    @BeforeAll
    public void runSaveEntriesInSolrAndStore() throws Exception {
        initStoreManager();
        prepareDownloadFolders();
        saveEntriesInSolrAndStore(
                uniprotQueryRepository, cloudSolrClient, solrClient, storeClient, taxRepository);
        saveInactiveEntries(storeManager);
    }

    private static void initStoreManager() {
        storeManager.addDocConverter(
                DataStoreManager.StoreType.UNIPROT, new UniProtEntryConverter(new HashMap<>()));
        storeManager.addDocConverter(
                DataStoreManager.StoreType.INACTIVE_UNIPROT, new InactiveEntryConverter());
        storeManager.addSolrClient(DataStoreManager.StoreType.UNIPROT, SolrCollection.uniprot);
        storeManager.addSolrClient(
                DataStoreManager.StoreType.INACTIVE_UNIPROT, SolrCollection.uniprot);
    }

    @BeforeEach
    void setUpRestTemplate() {
        setUp(restTemplate);
    }

    @Test
    void startQuery_doesntProcessInactiveEntries() throws IOException {
        String query = "*";
        MediaType format = MediaType.APPLICATION_JSON;
        DownloadRequest request = createDownloadRequest(query, format);
        String jobId = this.messageService.sendMessage(request);
        // Producer
        verify(this.messageService, never()).alreadyProcessed(jobId);
        // redis entry created
        await().until(jobCreatedInRedis(downloadJobRepository, jobId));
        await().atMost(Duration.ofSeconds(20)).until(jobFinished(downloadJobRepository, jobId));
        verifyMessageListener(1, 0, 1);
        verifyRedisEntry(query, jobId, JobStatus.FINISHED, 0, false, 12);
        DownloadJob downloadJob = this.downloadJobRepository.findById(jobId).get();
        assertEquals(8, downloadJob.getUpdateCount());
        verifyIdsAndResultFiles(jobId);
    }

    @Disabled
    @Test
    void sendAndProcessEmbeddingsH5MessageSuccessfullyAfterRetry() throws IOException {
        doThrow(new RuntimeException("Forced exception for testing on call converter.fromMessage"))
                .doCallRealMethod()
                .when(this.messageConverter)
                .fromMessage(any());
        String query = getMessageSuccessAfterRetryQuery();
        MediaType format = UniProtMediaType.HDF5_MEDIA_TYPE;
        DownloadRequest request = createDownloadRequest(query, format);
        String jobId = this.messageService.sendMessage(request);
        // Producer
        verify(this.messageService, never()).alreadyProcessed(jobId);
        Awaitility.await().until(jobCreatedInRedis(downloadJobRepository, jobId));
        Awaitility.await()
                .atMost(Duration.ofSeconds(20))
                .until(jobErrored(downloadJobRepository, jobId));
        // verify  redis
        verifyRedisEntry(query, jobId, JobStatus.ERROR, 1, true, 0);
        // after certain delay the job should be reprocessed from kb side
        Awaitility.await()
                .atMost(Duration.ofSeconds(20))
                .until(jobUnfinished(downloadJobRepository, jobId));
        verifyRedisEntry(query, jobId, JobStatus.UNFINISHED, 1, true, 10);
        // then job should be picked by embeddings consumers and set to Running again
        //        await().until(jobRunning(jobId));
        // the job should be completed after sometime by embeddings consumer
        Awaitility.await()
                .atMost(Duration.ofSeconds(20))
                .until(jobFinished(downloadJobRepository, jobId));
        // verify ids file generated from solr
        verifyIdsFile(jobId);
        // verify result file doesn't exist yet
        String fileName = jobId + FileType.GZIP.getExtension();
        Path resultFilePath = Path.of(this.resultFolder + "/" + fileName);
        Assertions.assertFalse(Files.exists(resultFilePath));
    }

    @Override
    protected Stream<String> streamIds(DownloadRequest downloadRequest) {
        return this.uniProtKBMessageListener.streamIds(downloadRequest);
    }

    @Override
    protected DownloadRequest createDownloadRequest(String query, MediaType format) {
        UniProtKBDownloadRequest request = new UniProtKBDownloadRequest();
        request.setQuery(query);
        request.setFormat(format.toString());
        request.setLargeSolrStreamRestricted(false);
        return request;
    }

    @Override
    protected void verifyMessageListener(
            int timesOnMessage, int timesAddHeader, int timesStreamIds) {
        verify(this.uniProtKBMessageListener, atLeast(timesOnMessage)).onMessage(any());
        verify(this.uniProtKBMessageListener, atLeast(timesAddHeader))
                .addAdditionalHeaders(any(), any());
        verify(this.uniProtKBMessageListener, atLeast(timesStreamIds)).streamIds(any());
    }

    @Override
    protected List<SolrCollection> getSolrCollections() {
        return List.of(SolrCollection.uniprot, SolrCollection.taxonomy);
    }

    @Override
    protected Stream<Arguments> getSupportedTypes() {
        return Stream.of(
                Arguments.of(MediaType.APPLICATION_JSON, 1), Arguments.of(HDF5_MEDIA_TYPE, 2));
    }

    @Override
    protected BaseAbstractMessageListener getMessageListener() {
        return this.uniProtKBMessageListener;
    }

    @Override
    protected String getMessageSuccessQuery() {
        return "content:P";
    }

    @Override
    protected String getMessageSuccessAfterRetryQuery() {
        return "reviewed:true";
    }

    @Override
    protected String getMessageFailAfterMaximumRetryQuery() {
        return "accession:P12345";
    }

    @Override
    protected String getMessageWithUnhandledExceptionQuery() {
        return "field:value";
    }

    @Override
    protected int getMessageSuccessAfterRetryCount() {
        return 10;
    }
}
