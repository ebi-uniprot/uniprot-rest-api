package org.uniprot.api.rest.respository;

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
import org.uniprot.core.uniprotkb.UniProtKBEntry;

import static org.junit.jupiter.api.Assertions.*;

class RepositoryConfigTest {

    @Test
    void uniProtKBSolrClientWithZk() {
        RepositoryConfig config = new RepositoryConfig();
        HttpClient httpClient = Mockito.mock(HttpClient.class);
        RepositoryConfigProperties configProps = config.repositoryConfigProperties();
        configProps.setZkHost("localhost:2021");
        SolrClient solrClient = config.solrClient(httpClient, configProps);
        assertNotNull(solrClient);
    }

    @Test
    void testHttpClient() {
        RepositoryConfig config = new RepositoryConfig();
        RepositoryConfigProperties configProps = config.repositoryConfigProperties();
        configProps.setHttphost("localhost");
        HttpClient httpClient = config.httpClient(configProps);
        assertNotNull(httpClient);
    }

    @Test
    void testHttpClientWithUserAndPassword() {
        RepositoryConfig config = new RepositoryConfig();
        RepositoryConfigProperties configProps = config.repositoryConfigProperties();
        configProps.setHttphost("localhost");
        configProps.setUsername("user");
        configProps.setPassword("password");
        HttpClient httpClient = config.httpClient(configProps);
        assertNotNull(httpClient);
    }

    @Test
    void uniProtKBSolrClientWrongProperties() {
        RepositoryConfig config = new RepositoryConfig();
        HttpClient httpClient = Mockito.mock(HttpClient.class);
        RepositoryConfigProperties configProps = config.repositoryConfigProperties();
        assertThrows(BeanCreationException.class, () ->config.solrClient(httpClient, configProps));
    }

    @Test
    void testUniProtKBRepositoryConfigProperties() {
        RepositoryConfig config = new RepositoryConfig();
        UniProtKBRepositoryConfigProperties configProps = config.uniProtKBRepositoryConfigProperties();
        assertNotNull(configProps);
    }

}