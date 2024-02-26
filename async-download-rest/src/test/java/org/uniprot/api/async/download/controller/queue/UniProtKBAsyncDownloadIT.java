package org.uniprot.api.async.download.controller.queue;

import static org.awaitility.Awaitility.await;
import static org.mockito.Mockito.*;
import static org.uniprot.api.async.download.queue.common.RedisUtil.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.stream.Stream;

import org.apache.solr.client.solrj.SolrClient;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
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
import org.uniprot.api.async.download.AsyncDownloadRestApp;
import org.uniprot.api.async.download.controller.AbstractAsyncDownloadIT;
import org.uniprot.api.async.download.controller.UniProtKBDownloadController;
import org.uniprot.api.async.download.queue.AsyncDownloadTestConfig;
import org.uniprot.api.async.download.queue.common.BaseAbstractMessageListener;
import org.uniprot.api.async.download.queue.common.ProducerMessageService;
import org.uniprot.api.async.download.queue.common.RedisConfiguration;
import org.uniprot.api.async.download.queue.embeddings.EmbeddingsTestConsumer;
import org.uniprot.api.async.download.queue.uniprotkb.UniProtKBDownloadRequest;
import org.uniprot.api.async.download.queue.uniprotkb.UniProtKBMessageListener;
import org.uniprot.api.common.repository.solrstream.FacetTupleStreamTemplate;
import org.uniprot.api.common.repository.stream.common.TupleStreamTemplate;
import org.uniprot.api.common.repository.stream.store.uniprotkb.TaxonomyLineageRepository;
import org.uniprot.api.idmapping.common.service.IdMappingJobCacheService;
import org.uniprot.api.idmapping.common.service.TestConfig;
import org.uniprot.api.rest.download.model.JobStatus;
import org.uniprot.api.rest.output.UniProtMediaType;
import org.uniprot.api.rest.output.context.FileType;
import org.uniprot.api.rest.request.DownloadRequest;
import org.uniprot.api.uniprotkb.common.repository.UniProtKBDataStoreTestConfig;
import org.uniprot.api.uniprotkb.common.repository.search.UniprotQueryRepository;
import org.uniprot.api.uniprotkb.common.repository.store.UniProtStoreConfig;
import org.uniprot.api.uniprotkb.common.utils.UniProtKBAsyncDownloadUtils;
import org.uniprot.core.uniprotkb.UniProtKBEntry;
import org.uniprot.store.datastore.UniProtStoreClient;
import org.uniprot.store.search.SolrCollection;

@ActiveProfiles(profiles = {"offline", "asyncDownload", "integration"})
@EnableConfigurationProperties
@PropertySource("classpath:application.properties")
@ContextConfiguration(
        classes = {
            TestConfig.class,
            UniProtKBDataStoreTestConfig.class,
            AsyncDownloadRestApp.class,
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

    @Value("${download.idFilesFolder}")
    protected String idsFolder;

    @Value("${download.resultFilesFolder}")
    protected String resultFolder;

    @Value("${async.download.uniprotkb.queueName}")
    private String downloadQueue;

    @Value("${async.download.uniprotkb.retryQueueName}")
    private String retryQueue;

    @Value(("${async.download.uniprotkb.rejectedQueueName}"))
    private String rejectedQueue;

    @Qualifier("uniProtKBTupleStream")
    @Autowired
    private TupleStreamTemplate tupleStreamTemplate;

    @Autowired private FacetTupleStreamTemplate uniProtKBFacetTupleStreamTemplate;

    @Qualifier("uniProtKB")
    @SpyBean
    private ProducerMessageService messageService;

    @Value("${async.download.embeddings.maxEntryCount}")
    protected long maxEntryCount;

    @SpyBean private UniProtKBMessageListener uniProtKBMessageListener;

    @Autowired private UniprotQueryRepository uniprotQueryRepository;

    @Autowired
    @Qualifier("uniProtKBSolrClient")
    private SolrClient solrClient;

    @Autowired private TaxonomyLineageRepository taxRepository;

    @Qualifier("uniProtStoreClient")
    @Autowired
    private UniProtStoreClient<UniProtKBEntry> storeClient; // in memory voldemort store client

    @MockBean(name = "uniProtRdfRestTemplate")
    private RestTemplate restTemplate;

    @Autowired protected IdMappingJobCacheService cacheService;

    @BeforeAll
    public void runSaveEntriesInSolrAndStore() throws Exception {
        prepareDownloadFolders();
        UniProtKBAsyncDownloadUtils.saveEntriesInSolrAndStore(
                uniprotQueryRepository, cloudSolrClient, solrClient, storeClient, taxRepository);
    }

    @BeforeEach
    void setUpRestTemplate() {
        UniProtKBAsyncDownloadUtils.setUp(restTemplate);
    }

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
        await().until(jobCreatedInRedis(downloadJobRepository, jobId));
        await().atMost(Duration.ofSeconds(20)).until(jobErrored(downloadJobRepository, jobId));
        // verify  redis
        verifyRedisEntry(query, jobId, JobStatus.ERROR, 1, true, 0);
        // after certain delay the job should be reprocessed from kb side
        await().atMost(Duration.ofSeconds(20)).until(jobUnfinished(downloadJobRepository, jobId));
        verifyRedisEntry(query, jobId, JobStatus.UNFINISHED, 1, true, 10);
        // then job should be picked by embeddings consumers and set to Running again
        //        await().until(jobRunning(jobId));
        // the job should be completed after sometime by embeddings consumer
        await().atMost(Duration.ofSeconds(20)).until(jobFinished(downloadJobRepository, jobId));
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
                Arguments.of(MediaType.APPLICATION_JSON, 1),
                Arguments.of(UniProtMediaType.HDF5_MEDIA_TYPE, 2));
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

    @Override
    protected ProducerMessageService getProducerMessageService() {
        return this.messageService;
    }

    @Override
    protected TupleStreamTemplate getTupleStreamTemplate() {
        return this.tupleStreamTemplate;
    }

    @Override
    protected FacetTupleStreamTemplate getFacetTupleStreamTemplate() {
        return this.uniProtKBFacetTupleStreamTemplate;
    }

    @Override
    protected String getIdsFolder() {
        return this.idsFolder;
    }

    @Override
    protected String getResultFolder() {
        return this.resultFolder;
    }

    @Override
    protected String getDownloadQueue() {
        return this.downloadQueue;
    }

    @Override
    protected String getRejectedQueue() {
        return this.rejectedQueue;
    }

    @Override
    protected String getRetryQueue() {
        return this.retryQueue;
    }
}
