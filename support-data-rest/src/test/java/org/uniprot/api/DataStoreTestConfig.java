package org.uniprot.api;

import org.apache.http.client.HttpClient;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.core.CoreContainer;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;
import org.uniprot.api.common.repository.search.SolrRequest;
import org.uniprot.api.common.repository.search.SolrRequestConverter;
import org.uniprot.store.indexer.ClosableEmbeddedSolrClient;
import org.uniprot.store.indexer.DataStoreManager;
import org.uniprot.store.indexer.SolrDataStoreManager;
import org.uniprot.store.search.SolrCollection;

import java.io.File;
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
    public SolrClient solrClient(DataStoreManager dataStoreManager) throws URISyntaxException {
        CoreContainer container = new CoreContainer(new File(System.getProperty(ClosableEmbeddedSolrClient.SOLR_HOME)).getAbsolutePath());
        container.load();
        ClosableEmbeddedSolrClient solrClient = new ClosableEmbeddedSolrClient(container, SolrCollection.taxonomy);
        dataStoreManager.addSolrClient(DataStoreManager.StoreType.TAXONOMY, solrClient);

        solrClient = new ClosableEmbeddedSolrClient(container, SolrCollection.keyword);
        dataStoreManager.addSolrClient(DataStoreManager.StoreType.KEYWORD, solrClient);

        solrClient = new ClosableEmbeddedSolrClient(container, SolrCollection.literature);
        dataStoreManager.addSolrClient(DataStoreManager.StoreType.LITERATURE, solrClient);

        ClosableEmbeddedSolrClient diseaseSolrClient = new ClosableEmbeddedSolrClient(container, SolrCollection.disease);
        dataStoreManager.addSolrClient(DataStoreManager.StoreType.DISEASE, diseaseSolrClient);

        ClosableEmbeddedSolrClient xrefSolrClient = new ClosableEmbeddedSolrClient(container, SolrCollection.crossref);
        dataStoreManager.addSolrClient(DataStoreManager.StoreType.CROSSREF, xrefSolrClient);

        return solrClient;
    }

    @Bean
    @Profile("offline")
    public SolrRequestConverter requestConverter() {
        return new SolrRequestConverter() {
            @Override
            public SolrQuery toSolrQuery(SolrRequest request) {
                SolrQuery solrQuery = super.toSolrQuery(request);

                // required for tests, because EmbeddedSolrServer is not sharded
                solrQuery.setParam("distrib", "false");
                solrQuery.setParam("terms.mincount", "1");

                return solrQuery;
            }
        };
    }
  
}
