package org.uniprot.api.idmapping.common.service.config;

import static org.junit.jupiter.api.Assertions.*;

import org.apache.solr.client.solrj.SolrClient;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.BeanCreationException;
import org.uniprot.api.rest.respository.UniProtKBRepositoryConfigProperties;
import org.uniprot.api.uniprotkb.common.repository.store.ResultsConfig;

class UniProtKBIdMappingResultsConfigTest {

    @Test
    void uniProtKBSolrClientWithZk() {
        UniProtKBRepositoryConfigProperties configProps = new UniProtKBRepositoryConfigProperties();
        configProps.setZkHost("localhostMapping:2021");
        ResultsConfig config = new ResultsConfig();
        SolrClient solrClient = config.uniProtKBSolrClient(configProps);
        assertNotNull(solrClient);
    }

    @Test
    void uniProtKBSolrClientWithHttpPost() {
        UniProtKBRepositoryConfigProperties configProps = new UniProtKBRepositoryConfigProperties();
        configProps.setHttphost("localhostMapping");
        ResultsConfig config = new ResultsConfig();
        SolrClient solrClient = config.uniProtKBSolrClient(configProps);
        assertNotNull(solrClient);
    }

    @Test
    void uniProtKBSolrClientWithUserAndPassword() {
        UniProtKBRepositoryConfigProperties configProps = new UniProtKBRepositoryConfigProperties();
        configProps.setHttphost("localhost");
        configProps.setUsername("userMapping");
        configProps.setPassword("password");
        ResultsConfig config = new ResultsConfig();
        SolrClient solrClient = config.uniProtKBSolrClient(configProps);
        assertNotNull(solrClient);
    }

    @Test
    void uniProtKBSolrClientWrongProperties() {
        UniProtKBRepositoryConfigProperties configProps = new UniProtKBRepositoryConfigProperties();
        ResultsConfig config = new ResultsConfig();
        assertThrows(BeanCreationException.class, () -> config.uniProtKBSolrClient(configProps));
    }
}
