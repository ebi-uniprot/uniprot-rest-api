package org.uniprot.api.uniref.common.queue;

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
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.context.annotation.PropertySource;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.web.client.RestTemplate;
import org.uniprot.api.rest.controller.AbstractAsyncDownloadIT;
import org.uniprot.api.rest.download.*;
import org.uniprot.api.rest.download.configuration.RedisConfiguration;
import org.uniprot.api.rest.download.queue.BaseAbstractMessageListener;
import org.uniprot.api.rest.request.DownloadRequest;
import org.uniprot.api.uniref.common.repository.DataStoreTestConfig;
import org.uniprot.api.uniref.common.repository.UniRefQueryRepository;
import org.uniprot.api.uniref.common.repository.store.UniRefStoreConfig;
import org.uniprot.api.uniref.common.request.UniRefDownloadRequest;
import org.uniprot.api.uniref.common.utils.UniRefAsyncDownloadUtils;
import org.uniprot.core.uniref.UniRefEntryLight;
import org.uniprot.store.datastore.UniProtStoreClient;
import org.uniprot.store.search.SolrCollection;

@ActiveProfiles(profiles = {"offline", "asyncDownload", "integration"})
@EnableConfigurationProperties
@PropertySource("classpath:application.properties")
@ContextConfiguration(
        classes = {
            DataStoreTestConfig.class,
            UniRefStoreConfig.class,
            AsyncDownloadTestConfig.class,
            RedisConfiguration.class
        })
@ExtendWith(SpringExtension.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
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
}
