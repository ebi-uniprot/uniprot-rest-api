package uk.ac.ebi.uniprot.repository;

import org.apache.http.client.HttpClient;
import org.apache.solr.client.solrj.SolrClient;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;
import org.springframework.data.solr.core.SolrTemplate;
import org.springframework.data.solr.server.support.EmbeddedSolrServerFactory;
import uk.ac.ebi.uniprot.indexer.DataStoreManager;
import uk.ac.ebi.uniprot.indexer.SolrDataStoreManager;

import java.io.IOException;

import static org.mockito.Mockito.mock;

@TestConfiguration
public class DataStoreTestConfig {
//    @Value(("${solr.home}"))
//    private String solrHome;

    @Bean(destroyMethod = "close")
    public DataStoreManager dataStoreManager() throws IOException {
        SolrDataStoreManager sdsm = new SolrDataStoreManager();
        return new DataStoreManager(sdsm);
    }

    @Bean
    @Profile("offline")
    public HttpClient httpClient() {
        return mock(HttpClient.class);
    }

    @Bean
    @Profile("offline")
    public SolrClient uniProtSolrClient(DataStoreManager dataStoreManager) throws Exception {
//        ClosableEmbeddedSolrClient solrClient = new ClosableEmbeddedSolrClient(SolrCollection.disease);
//        dataStoreManager.addSolrClient(DataStoreManager.StoreType.DISEASE, solrClient);
//        dataStoreManager.addSolrClient(DataStoreManager.StoreType.DISEASE, solrClient);
//        return solrClient;
        EmbeddedSolrServerFactory factory = new EmbeddedSolrServerFactory(System.getProperty("solr.home"));
        return factory.getSolrClient();
    }

    @Bean
    @Profile("offline")
    public SolrTemplate solrTemplate(SolrClient uniProtSolrClient) {
        return new SolrTemplate(uniProtSolrClient);
    }
}
