package org.uniprot.api.uniref.queue;

import static org.mockito.Mockito.*;
import static org.uniprot.api.rest.output.UniProtMediaType.FASTA_MEDIA_TYPE;
import static org.uniprot.api.uniref.utils.UniRefAsyncDownloadUtils.saveEntriesInSolrAndStore;
import static org.uniprot.api.uniref.utils.UniRefAsyncDownloadUtils.setUp;

import java.util.List;
import java.util.stream.Stream;

import org.apache.solr.client.solrj.SolrClient;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.provider.Arguments;
import org.springframework.beans.factory.annotation.Autowired;
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
import org.uniprot.api.rest.controller.AbstractAsyncDownloadIT;
import org.uniprot.api.rest.download.AsyncDownloadTestConfig;
import org.uniprot.api.rest.download.configuration.RedisConfiguration;
import org.uniprot.api.rest.download.queue.AbstractMessageListener;
import org.uniprot.api.rest.request.DownloadRequest;
import org.uniprot.api.uniref.UniRefRestApplication;
import org.uniprot.api.uniref.controller.UniRefDownloadController;
import org.uniprot.api.uniref.repository.DataStoreTestConfig;
import org.uniprot.api.uniref.repository.UniRefQueryRepository;
import org.uniprot.api.uniref.repository.store.UniRefStoreConfig;
import org.uniprot.api.uniref.request.UniRefDownloadRequest;
import org.uniprot.core.uniref.UniRefEntryLight;
import org.uniprot.store.datastore.UniProtStoreClient;
import org.uniprot.store.search.SolrCollection;

@ActiveProfiles(profiles = {"offline", "asyncDownload", "integration"})
@EnableConfigurationProperties
@PropertySource("classpath:application.properties")
@ContextConfiguration(
        classes = {
            DataStoreTestConfig.class,
            UniRefRestApplication.class,
            UniRefStoreConfig.class,
            AsyncDownloadTestConfig.class,
            RedisConfiguration.class
        })
@ExtendWith(SpringExtension.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@WebMvcTest({UniRefDownloadController.class})
@AutoConfigureWebClient
public class UniRefAsyncDownloadIT extends AbstractAsyncDownloadIT {

    @SpyBean private UniRefMessageListener uniRefMessageListener;

    @Autowired private UniRefQueryRepository unirefQueryRepository;

    @Autowired
    private UniProtStoreClient<UniRefEntryLight> storeClient; // in memory voldemort store client

    @MockBean(name = "unirefRdfRestTemplate")
    private RestTemplate restTemplate;

    @Autowired private SolrClient solrClient;

    @BeforeAll
    public void runSaveEntriesInSolrAndStore() throws Exception {
        prepareDownloadFolders();
        saveEntriesInSolrAndStore(unirefQueryRepository, cloudSolrClient, solrClient, storeClient);
    }

    @BeforeEach
    void setUpRestTemplate() {
        setUp(restTemplate);
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
    protected AbstractMessageListener getMessageListener() {
        return this.uniRefMessageListener;
    }

    @Override
    protected List<SolrCollection> getSolrCollections() {
        return List.of(SolrCollection.uniref);
    }

    @Override
    protected String getMessageSuccessQuery() {
        return "identity:*";
    }

    @Override
    protected String getMessageSuccessAfterRetryQuery() {
        return "uniprot_id:q9h9k5";
    }

    @Override
    protected String getMessageFailAfterMaximumRetryQuery() {
        return "id:UniRef100_UPI00001109EE";
    }

    @Override
    protected String getMessageWithUnhandledExceptionQuery() {
        return "field:value";
    }
}
