package org.uniprot.api.uniprotkb.repository.store;

import org.apache.http.client.HttpClient;
import org.apache.solr.client.solrj.SolrClient;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.web.client.RestTemplate;
import org.uniprot.api.common.repository.search.SolrRequestConverter;
import org.uniprot.api.common.repository.stream.common.TupleStreamTemplate;
import org.uniprot.api.common.repository.stream.document.TupleStreamDocumentIdStream;
import org.uniprot.api.common.repository.stream.rdf.RDFStreamer;
import org.uniprot.api.common.repository.stream.rdf.RDFStreamerConfigProperties;
import org.uniprot.api.common.repository.stream.store.StoreStreamer;
import org.uniprot.api.common.repository.stream.store.StreamerConfigProperties;
import org.uniprot.api.rest.respository.UniProtKBRepositoryConfigProperties;
import org.uniprot.core.uniprotkb.UniProtKBEntry;

import static org.junit.jupiter.api.Assertions.*;

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
        assertThrows(BeanCreationException.class, () ->config.uniProtKBSolrClient(httpClient, configProps));
    }

    @Test
    void testTupleStreamTemplate() {
        ResultsConfig config = new ResultsConfig();
        HttpClient httpClient = Mockito.mock(HttpClient.class);
        SolrClient solrClient = Mockito.mock(SolrClient.class);
        SolrRequestConverter converter = Mockito.mock(SolrRequestConverter.class);
        StreamerConfigProperties configProps = config.resultsConfigProperties();

        TupleStreamTemplate result = config.tupleStreamTemplate(configProps, httpClient, solrClient, converter);
        assertNotNull(result);
    }

    @Test
    void testUniProtRDFStreamer() {
        ResultsConfig configMock = Mockito.mock(ResultsConfig.class);
        ResultsConfig config = new ResultsConfig();

        RDFStreamerConfigProperties rdfConfig = new RDFStreamerConfigProperties();
        rdfConfig.setRetryDelayMillis(10);
        rdfConfig.setBatchSize(10);
        rdfConfig.setMaxRetries(1);
        Mockito.when(configMock.rdfConfigProperties()).thenReturn(rdfConfig);


        HttpClient httpClient = Mockito.mock(HttpClient.class);
        SolrClient solrClient = Mockito.mock(SolrClient.class);
        SolrRequestConverter converter = Mockito.mock(SolrRequestConverter.class);
        StreamerConfigProperties configProps = config.resultsConfigProperties();
        configProps.setStoreFetchRetryDelayMillis(10);
        TupleStreamTemplate tupleStreamTemplate = config.tupleStreamTemplate(configProps, httpClient, solrClient, converter);
        RestTemplate restTemplate = Mockito.mock(RestTemplate.class);
        StreamerConfigProperties streamConfig = config.resultsConfigProperties();
        TupleStreamDocumentIdStream documentIdStream = config.documentIdStream(tupleStreamTemplate, streamConfig);
        Mockito.when(configMock.uniProtRDFStreamer(restTemplate, documentIdStream)).thenCallRealMethod();
        RDFStreamer result = configMock.uniProtRDFStreamer(restTemplate, documentIdStream);
        assertNotNull(result);
    }

    @Test
    void testUniProtEntryStoreStreamer() {
        ResultsConfig config = new ResultsConfig();

        HttpClient httpClient = Mockito.mock(HttpClient.class);
        SolrClient solrClient = Mockito.mock(SolrClient.class);
        SolrRequestConverter converter = Mockito.mock(SolrRequestConverter.class);
        StreamerConfigProperties configProps = config.resultsConfigProperties();
        configProps.setStoreFetchRetryDelayMillis(10);
        TupleStreamTemplate tupleStreamTemplate = config.tupleStreamTemplate(configProps, httpClient, solrClient, converter);
        UniProtKBStoreClient uniprotClient = new UniProtKBStoreClient(null);
        TupleStreamDocumentIdStream documentIdStream = config.documentIdStream(tupleStreamTemplate, configProps);
        StoreStreamer<UniProtKBEntry> result = config.uniProtEntryStoreStreamer(uniprotClient, tupleStreamTemplate, configProps, documentIdStream);
        assertNotNull(result);
    }

}