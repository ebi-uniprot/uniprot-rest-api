package org.uniprot.api.idmapping.service.config;

import static org.junit.jupiter.api.Assertions.*;

import org.apache.http.client.HttpClient;
import org.apache.solr.client.solrj.SolrClient;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.BeanCreationException;
import org.uniprot.api.rest.respository.UniProtKBRepositoryConfigProperties;

class UniProtKBIdMappingResultsConfigTest {

    @Test
    void uniProtKBSolrClientWithZk() {
        UniProtKBIdMappingResultsConfig config = new UniProtKBIdMappingResultsConfig();
        UniProtKBRepositoryConfigProperties configProps = new UniProtKBRepositoryConfigProperties();
        HttpClient httpClient = Mockito.mock(HttpClient.class);
        configProps.setZkHost("localhostMapping:2021");
        SolrClient solrClient = config.uniProtKBSolrClient(httpClient, configProps);
        assertNotNull(solrClient);
    }

    @Test
    void uniProtKBSolrClientWithHttpPost() {
        UniProtKBIdMappingResultsConfig config = new UniProtKBIdMappingResultsConfig();
        UniProtKBRepositoryConfigProperties configProps = new UniProtKBRepositoryConfigProperties();
        HttpClient httpClient = Mockito.mock(HttpClient.class);
        configProps.setHttphost("localhostMapping");
        SolrClient solrClient = config.uniProtKBSolrClient(httpClient, configProps);
        assertNotNull(solrClient);
    }

    @Test
    void uniProtKBSolrClientWithUserAndPassword() {
        UniProtKBIdMappingResultsConfig config = new UniProtKBIdMappingResultsConfig();
        UniProtKBRepositoryConfigProperties configProps = new UniProtKBRepositoryConfigProperties();
        HttpClient httpClient = Mockito.mock(HttpClient.class);
        configProps.setHttphost("localhost");
        configProps.setUsername("userMapping");
        configProps.setPassword("password");
        SolrClient solrClient = config.uniProtKBSolrClient(httpClient, configProps);
        assertNotNull(solrClient);
    }

    @Test
    void uniProtKBSolrClientWrongProperties() {
        UniProtKBIdMappingResultsConfig config = new UniProtKBIdMappingResultsConfig();
        UniProtKBRepositoryConfigProperties configProps = new UniProtKBRepositoryConfigProperties();
        HttpClient httpClient = Mockito.mock(HttpClient.class);
        assertThrows(
                BeanCreationException.class,
                () -> config.uniProtKBSolrClient(httpClient, configProps));
    }
}
