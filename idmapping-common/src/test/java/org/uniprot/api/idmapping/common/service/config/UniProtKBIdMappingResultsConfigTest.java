package org.uniprot.api.idmapping.common.service.config;

import static org.junit.jupiter.api.Assertions.*;

import org.apache.http.client.HttpClient;
import org.apache.solr.client.solrj.SolrClient;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.BeanCreationException;
import org.uniprot.api.rest.respository.UniProtKBRepositoryConfigProperties;
import org.uniprot.api.uniprotkb.common.repository.store.ResultsConfig;

class UniProtKBIdMappingResultsConfigTest {

    @Test
    void uniProtKBSolrClientWithZk() {
        UniProtKBRepositoryConfigProperties configProps = new UniProtKBRepositoryConfigProperties();
        HttpClient httpClient = Mockito.mock(HttpClient.class);
        configProps.setZkHost("localhostMapping:2021");
        ResultsConfig config = new ResultsConfig();
        SolrClient solrClient = config.uniProtKBSolrClient(httpClient, configProps);
        assertNotNull(solrClient);
    }

    @Test
    void uniProtKBSolrClientWithHttpPost() {
        UniProtKBRepositoryConfigProperties configProps = new UniProtKBRepositoryConfigProperties();
        HttpClient httpClient = Mockito.mock(HttpClient.class);
        configProps.setHttphost("localhostMapping");
        ResultsConfig config = new ResultsConfig();
        SolrClient solrClient = config.uniProtKBSolrClient(httpClient, configProps);
        assertNotNull(solrClient);
    }

    @Test
    void uniProtKBSolrClientWithUserAndPassword() {
        UniProtKBRepositoryConfigProperties configProps = new UniProtKBRepositoryConfigProperties();
        HttpClient httpClient = Mockito.mock(HttpClient.class);
        configProps.setHttphost("localhost");
        configProps.setUsername("userMapping");
        configProps.setPassword("password");
        ResultsConfig config = new ResultsConfig();
        SolrClient solrClient = config.uniProtKBSolrClient(httpClient, configProps);
        assertNotNull(solrClient);
    }

    @Test
    void uniProtKBSolrClientWrongProperties() {
        UniProtKBRepositoryConfigProperties configProps = new UniProtKBRepositoryConfigProperties();
        HttpClient httpClient = Mockito.mock(HttpClient.class);
        ResultsConfig config = new ResultsConfig();
        assertThrows(
                BeanCreationException.class,
                () -> config.uniProtKBSolrClient(httpClient, configProps));
    }
}
