package uk.ac.ebi.uniprot.api.proteome.controller;

import static org.mockito.Mockito.mock;

import java.io.IOException;
import java.net.URISyntaxException;

import org.apache.http.client.HttpClient;
import org.apache.solr.client.solrj.SolrClient;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;

import uk.ac.ebi.uniprot.indexer.ClosableEmbeddedSolrClient;
import uk.ac.ebi.uniprot.indexer.DataStoreManager;
import uk.ac.ebi.uniprot.indexer.SolrDataStoreManager;
import uk.ac.ebi.uniprot.indexer.proteome.ProteomeEntryConverter;
import uk.ac.ebi.uniprot.indexer.uniprot.mockers.TaxonomyRepoMocker;
import uk.ac.ebi.uniprot.search.SolrCollection;

/**
 *
 * @author jluo
 * @date: 30 Apr 2019
 *
*/
@TestConfiguration
public class ProteomeDataStoreTestConfig {
	@Bean(destroyMethod = "close")
    public DataStoreManager dataStoreManager() throws IOException,URISyntaxException{
        SolrDataStoreManager sdsm = new SolrDataStoreManager();
        DataStoreManager dataStoreManager=  new DataStoreManager(sdsm);
        return dataStoreManager;
    }

    @Bean
    @Profile("proteome_offline")
    public HttpClient httpClient() {
        return mock(HttpClient.class);
    }
    @Bean("proteome")
    @Profile("proteome_offline")
    public SolrClient proteomeSolrClient(DataStoreManager dataStoreManager) throws URISyntaxException {
        ClosableEmbeddedSolrClient solrClient = new ClosableEmbeddedSolrClient(SolrCollection.proteome);
        addStoreInfoProteome(dataStoreManager, solrClient, DataStoreManager.StoreType.PROTEOME );
        return solrClient;
    }
    
//    @Bean("genecentric")
//    @Profile("offline")
//    public SolrClient genecentricSolrClient(DataStoreManager dataStoreManager) throws URISyntaxException {
//        ClosableEmbeddedSolrClient solrClient = new ClosableEmbeddedSolrClient(SolrCollection.genecentric);
//        addStoreInfoProteome(dataStoreManager, solrClient,  DataStoreManager.StoreType.GENECENTRIC );
//        return solrClient;
//    }

    private void addStoreInfoProteome(DataStoreManager dsm, ClosableEmbeddedSolrClient solrClient, DataStoreManager.StoreType store ) throws URISyntaxException {
        dsm.addSolrClient(store, solrClient);
    }
}

