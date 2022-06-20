package org.uniprot.api.rest.respository;

import static org.junit.jupiter.api.Assertions.*;

import org.apache.http.client.HttpClient;
import org.apache.solr.client.solrj.SolrClient;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.BeanCreationException;

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
        assertThrows(BeanCreationException.class, () -> config.solrClient(httpClient, configProps));
    }

    @Test
    void testUniProtKBRepositoryConfigProperties() {
        RepositoryConfig config = new RepositoryConfig();
        UniProtKBRepositoryConfigProperties configProps =
                config.uniProtKBRepositoryConfigProperties();
        assertNotNull(configProps);
    }
}
