package uk.ac.ebi.uniprot.api.proteome.controller;

import static org.mockito.Mockito.mock;

import java.io.IOException;
import java.net.URISyntaxException;

import org.apache.http.client.HttpClient;
import org.apache.solr.client.solrj.SolrClient;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
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
    public SolrClient proteomeSolrClient(DataStoreManager dataStoreManager) throws URISyntaxException {
        ClosableEmbeddedSolrClient solrClient = new ClosableEmbeddedSolrClient(SolrCollection.proteome);
        addStoreInfo(dataStoreManager, solrClient);
        return solrClient;
    }

    private void addStoreInfo(DataStoreManager dsm, ClosableEmbeddedSolrClient proteomeSolrClient) throws URISyntaxException {
        dsm.addDocConverter(DataStoreManager.StoreType.PROTEOME, new ProteomeEntryConverter(TaxonomyRepoMocker.getTaxonomyRepo()));
        dsm.addSolrClient(DataStoreManager.StoreType.PROTEOME, proteomeSolrClient);
    }
}

