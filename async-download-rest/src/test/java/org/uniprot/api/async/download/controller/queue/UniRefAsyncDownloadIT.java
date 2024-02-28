package org.uniprot.api.async.download.controller.queue;

import static org.mockito.Mockito.*;
import static org.uniprot.api.rest.output.UniProtMediaType.FASTA_MEDIA_TYPE;

import java.util.List;
import java.util.stream.Stream;

import org.apache.solr.client.solrj.SolrClient;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInstance;
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
import org.uniprot.api.async.download.controller.UniRefDownloadController;
import org.uniprot.api.async.download.queue.AsyncDownloadTestConfig;
import org.uniprot.api.async.download.queue.common.BaseAbstractMessageListener;
import org.uniprot.api.async.download.queue.common.ProducerMessageService;
import org.uniprot.api.async.download.queue.common.RedisConfiguration;
import org.uniprot.api.async.download.queue.uniref.UniRefDownloadRequest;
import org.uniprot.api.async.download.queue.uniref.UniRefMessageListener;
import org.uniprot.api.common.repository.solrstream.FacetTupleStreamTemplate;
import org.uniprot.api.common.repository.stream.common.TupleStreamTemplate;
import org.uniprot.api.rest.request.DownloadRequest;
import org.uniprot.api.rest.request.HashGenerator;
import org.uniprot.api.uniref.common.UniRefAsyncDownloadUtils;
import org.uniprot.api.uniref.common.repository.UniRefDataStoreTestConfig;
import org.uniprot.api.uniref.common.repository.search.UniRefQueryRepository;
import org.uniprot.api.uniref.common.repository.store.UniRefStoreConfig;
import org.uniprot.core.uniref.UniRefEntryLight;
import org.uniprot.store.datastore.UniProtStoreClient;
import org.uniprot.store.search.SolrCollection;

@ActiveProfiles(profiles = {"offline", "asyncDownload", "integration"})
@EnableConfigurationProperties
@PropertySource("classpath:application.properties")
@ContextConfiguration(
        classes = {
            UniRefDataStoreTestConfig.class,
            AsyncDownloadRestApp.class,
            UniRefStoreConfig.class,
            AsyncDownloadTestConfig.class,
            RedisConfiguration.class
        })
@ExtendWith(SpringExtension.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@WebMvcTest({UniRefDownloadController.class})
@AutoConfigureWebClient
public class UniRefAsyncDownloadIT extends AbstractAsyncDownloadIT {
    @Autowired private HashGenerator<DownloadRequest> uniRefHashGenerator;

    @Value("${async.download.uniref.retryMaxCount}")
    private int maxRetry;

    @Value("${async.download.uniref.result.idFilesFolder}")
    protected String idsFolder;

    @Value("${async.download.uniref.result.resultFilesFolder}")
    protected String resultFolder;

    @Value("${async.download.uniref.queueName}")
    private String downloadQueue;

    @Value("${async.download.uniref.retryQueueName}")
    private String retryQueue;

    @Value(("${async.download.uniref.rejectedQueueName}"))
    private String rejectedQueue;

    @Qualifier("uniRefTupleStreamTemplate")
    @Autowired
    private TupleStreamTemplate tupleStreamTemplate;

    @Autowired private FacetTupleStreamTemplate uniRefFacetTupleStreamTemplate;

    @Qualifier("uniRef")
    @SpyBean
    private ProducerMessageService messageService;

    @SpyBean private UniRefMessageListener uniRefMessageListener;

    @Autowired private UniRefQueryRepository unirefQueryRepository;

    @Qualifier("uniRefLightStoreClient")
    @Autowired
    private UniProtStoreClient<UniRefEntryLight> storeClient; // in memory voldemort store client

    @MockBean(name = "unirefRdfRestTemplate")
    private RestTemplate restTemplate;

    @Autowired private SolrClient solrClient;

    @BeforeAll
    public void runSaveEntriesInSolrAndStore() throws Exception {
        prepareDownloadFolders();
        UniRefAsyncDownloadUtils.saveEntriesInSolrAndStore(
                unirefQueryRepository, cloudSolrClient, solrClient, storeClient);
    }

    @BeforeEach
    void setUpRestTemplate() {
        UniRefAsyncDownloadUtils.setUp(restTemplate);
    }

    @Override
    protected Stream<String> streamIds(DownloadRequest downloadRequest) {
        return this.uniRefMessageListener.streamIds(downloadRequest);
    }

    @Override
    protected UniRefDownloadRequest createDownloadRequest(String query, MediaType format) {
        UniRefDownloadRequest request = new UniRefDownloadRequest();
        request.setQuery(query);
        request.setFormat(format.toString());
        request.setLargeSolrStreamRestricted(false);
        return request;
    }

    @Override
    protected void verifyMessageListener(
            int timesOnMessage, int timesAddHeader, int timesStreamIds) {
        verify(this.uniRefMessageListener, atLeast(timesOnMessage)).onMessage(any());
        verify(this.uniRefMessageListener, atLeast(timesAddHeader))
                .addAdditionalHeaders(any(), any());
        verify(this.uniRefMessageListener, atLeast(timesStreamIds)).streamIds(any());
    }

    @Override
    protected Stream<Arguments> getSupportedTypes() {
        return Stream.of(
                Arguments.of(MediaType.APPLICATION_JSON, 1), Arguments.of(FASTA_MEDIA_TYPE, 2));
    }

    @Override
    protected BaseAbstractMessageListener getMessageListener() {
        return this.uniRefMessageListener;
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
        return this.uniRefFacetTupleStreamTemplate;
    }

    @Override
    protected String getMessageSuccessQuery() {
        return "identity:*";
    }

    @Override
    protected String getMessageSuccessAfterRetryQuery() {
        return "id:UniRef50_P03901";
    }

    @Override
    protected String getMessageFailAfterMaximumRetryQuery() {
        return "id:UniRef100_UPI00001109EE";
    }

    @Override
    protected String getMessageWithUnhandledExceptionQuery() {
        return "field:value";
    }

    @Override
    protected int getMessageSuccessAfterRetryCount() {
        return 1;
    }

    @Override
    protected ProducerMessageService getProducerMessageService() {
        return this.messageService;
    }

    @Override
    protected int getMaxRetry() {
        return this.maxRetry;
    }

    @Override
    protected HashGenerator<DownloadRequest> getHashGenerator() {
        return this.uniRefHashGenerator;
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
