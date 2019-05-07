package uk.ac.ebi.uniprot.repository;

import org.apache.solr.client.solrj.SolrClient;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;
import org.springframework.data.solr.core.SolrTemplate;
import uk.ac.ebi.uniprot.indexer.ClosableEmbeddedSolrClient;
import uk.ac.ebi.uniprot.indexer.DataStoreManager;
import uk.ac.ebi.uniprot.indexer.SolrDataStoreManager;
import uk.ac.ebi.uniprot.search.SolrCollection;

import java.io.IOException;

@TestConfiguration
public class DataStoreTestConfig {

    @Bean(destroyMethod = "close")
    public DataStoreManager dataStoreManager() throws IOException {
        SolrDataStoreManager sdsm = new SolrDataStoreManager();
        DataStoreManager dm = new DataStoreManager(sdsm);
        //ClosableEmbeddedSolrClient solrClient = new ClosableEmbeddedSolrClient(SolrCollection.disease);
        //dm.addSolrClient(DataStoreManager.StoreType.DISEASE, solrClient);
        return dm;
    }

    @Bean
    @Profile("offline")
    public SolrClient solrClient(DataStoreManager dataStoreManager) {
        ClosableEmbeddedSolrClient solrClient = new ClosableEmbeddedSolrClient(SolrCollection.disease);
        dataStoreManager.addSolrClient(DataStoreManager.StoreType.DISEASE, solrClient);
        return solrClient;
    }

    @Bean
    @Profile("offline")
    public SolrTemplate solrTemplate(SolrClient uniProtSolrClient) {
        return new SolrTemplate(uniProtSolrClient);
    }
}
