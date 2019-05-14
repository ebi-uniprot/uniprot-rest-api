package uk.ac.ebi.uniprot.api;

import org.apache.http.client.HttpClient;
import org.apache.solr.client.solrj.SolrClient;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;
import uk.ac.ebi.uniprot.indexer.ClosableEmbeddedSolrClient;
import uk.ac.ebi.uniprot.indexer.DataStoreManager;
import uk.ac.ebi.uniprot.indexer.SolrDataStoreManager;
import uk.ac.ebi.uniprot.search.SolrCollection;

import java.io.IOException;
import java.net.URISyntaxException;

import static org.mockito.Mockito.mock;

@TestConfiguration
public class DataStoreTestConfig {

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
    public SolrClient taxonomySolrClient(DataStoreManager dataStoreManager) throws URISyntaxException {
        ClosableEmbeddedSolrClient solrClient = new ClosableEmbeddedSolrClient(SolrCollection.taxonomy);
        addStoreInfo(dataStoreManager, solrClient);
        return solrClient;
    }

    private void addStoreInfo(DataStoreManager dsm, ClosableEmbeddedSolrClient taxonomySolrClient) throws URISyntaxException {
        dsm.addSolrClient(DataStoreManager.StoreType.TAXONOMY, taxonomySolrClient);
    }
}
