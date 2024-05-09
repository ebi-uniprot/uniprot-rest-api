package org.uniprot.api.uniprotkb.common.repository.store;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.apache.http.client.HttpClient;
import org.apache.solr.client.solrj.SolrClient;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.BeanCreationException;
import org.uniprot.api.common.repository.search.SolrRequestConverter;
import org.uniprot.api.common.repository.stream.common.TupleStreamTemplate;
import org.uniprot.api.common.repository.stream.document.TupleStreamDocumentIdStream;
import org.uniprot.api.common.repository.stream.store.StoreStreamer;
import org.uniprot.api.common.repository.stream.store.StoreStreamerConfig;
import org.uniprot.api.common.repository.stream.store.StreamerConfigProperties;
import org.uniprot.api.common.repository.stream.store.uniprotkb.TaxonomyLineageService;
import org.uniprot.api.common.repository.stream.store.uniprotkb.TaxonomyLineageServiceImpl;
import org.uniprot.api.rest.respository.UniProtKBRepositoryConfigProperties;
import org.uniprot.core.uniprotkb.UniProtKBEntry;

class ResultsConfigTest {

    @Test
    void uniProtKBSolrClientWithZk() {
        ResultsConfig config = new ResultsConfig();
        HttpClient httpClient = Mockito.mock(HttpClient.class);
        UniProtKBRepositoryConfigProperties configProps = new UniProtKBRepositoryConfigProperties();
        configProps.setZkHost("localhost:2021");
        SolrClient solrClient = config.uniProtKBSolrClient(httpClient, configProps);
        assertNotNull(solrClient);
    }

    @Test
    void uniProtKBSolrClientWithHttpPost() {
        ResultsConfig config = new ResultsConfig();
        HttpClient httpClient = Mockito.mock(HttpClient.class);
        UniProtKBRepositoryConfigProperties configProps = new UniProtKBRepositoryConfigProperties();
        configProps.setHttphost("localhost");
        SolrClient solrClient = config.uniProtKBSolrClient(httpClient, configProps);
        assertNotNull(solrClient);
    }

    @Test
    void uniProtKBSolrClientWithUserAndPassword() {
        ResultsConfig config = new ResultsConfig();
        HttpClient httpClient = Mockito.mock(HttpClient.class);
        UniProtKBRepositoryConfigProperties configProps = new UniProtKBRepositoryConfigProperties();
        configProps.setHttphost("localhost");
        configProps.setUsername("user");
        configProps.setPassword("password");
        SolrClient solrClient = config.uniProtKBSolrClient(httpClient, configProps);
        assertNotNull(solrClient);
    }

    @Test
    void uniProtKBSolrClientWrongProperties() {
        ResultsConfig config = new ResultsConfig();
        HttpClient httpClient = Mockito.mock(HttpClient.class);
        UniProtKBRepositoryConfigProperties configProps = new UniProtKBRepositoryConfigProperties();
        assertThrows(
                BeanCreationException.class,
                () -> config.uniProtKBSolrClient(httpClient, configProps));
    }

    @Test
    void testTupleStreamTemplate() {
        ResultsConfig config = new ResultsConfig();
        HttpClient httpClient = Mockito.mock(HttpClient.class);
        SolrClient solrClient = Mockito.mock(SolrClient.class);
        SolrRequestConverter converter = Mockito.mock(SolrRequestConverter.class);
        StreamerConfigProperties configProps = config.resultsConfigProperties();

        TupleStreamTemplate result = config.tupleStreamTemplate(configProps, solrClient, converter);
        assertNotNull(result);
    }

    @Test
    void testUniProtEntryStoreStreamerConfig() {
        ResultsConfig config = new ResultsConfig();

        HttpClient httpClient = Mockito.mock(HttpClient.class);
        SolrClient solrClient = Mockito.mock(SolrClient.class);
        SolrRequestConverter converter = Mockito.mock(SolrRequestConverter.class);
        TaxonomyLineageService taxonomyLineageService =
                Mockito.mock(TaxonomyLineageServiceImpl.class);
        StreamerConfigProperties configProps = config.resultsConfigProperties();
        configProps.setStoreFetchRetryDelayMillis(10);
        TupleStreamTemplate tupleStreamTemplate =
                config.tupleStreamTemplate(configProps, solrClient, converter);
        UniProtKBStoreClient uniprotClient = new UniProtKBStoreClient(null);
        TupleStreamDocumentIdStream documentIdStream =
                config.documentIdStream(tupleStreamTemplate, configProps);
        StoreStreamerConfig<UniProtKBEntry> result =
                config.uniProtKBStoreStreamerConfig(
                        uniprotClient, tupleStreamTemplate, configProps, documentIdStream);
        assertNotNull(result);
    }

    @Test
    void testUniProtEntryStoreStreamer() {
        ResultsConfig config = new ResultsConfig();

        TaxonomyLineageService taxonomyLineageService =
                Mockito.mock(TaxonomyLineageServiceImpl.class);
        StoreStreamerConfig<UniProtKBEntry> storeStreamerConfig =
                Mockito.mock(StoreStreamerConfig.class);

        StoreStreamer<UniProtKBEntry> result =
                config.uniProtEntryStoreStreamer(storeStreamerConfig, taxonomyLineageService);
        assertNotNull(result);
    }
}
