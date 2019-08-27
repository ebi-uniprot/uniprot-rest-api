package org.uniprot.api.uniref.repository;

import static org.mockito.Mockito.mock;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;

import org.apache.http.client.HttpClient;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.core.CoreContainer;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;
import org.uniprot.api.common.repository.search.SolrRequest;
import org.uniprot.api.common.repository.search.SolrRequestConverter;
import org.uniprot.api.uniref.repository.store.UniRefStoreClient;
import org.uniprot.store.datastore.voldemort.uniref.VoldemortInMemoryUniRefEntryStore;
import org.uniprot.store.indexer.ClosableEmbeddedSolrClient;
import org.uniprot.store.indexer.DataStoreManager;
import org.uniprot.store.indexer.SolrDataStoreManager;
import org.uniprot.store.indexer.uniprot.mockers.TaxonomyRepoMocker;
import org.uniprot.store.indexer.uniref.UniRefDocumentConverter;
import org.uniprot.store.search.SolrCollection;

/**
 *
 * @author jluo
 * @date: 23 Aug 2019
 *
*/
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
	    public SolrClient unirefSolrClient(DataStoreManager dataStoreManager) throws URISyntaxException {
	        CoreContainer container = new CoreContainer(new File(System.getProperty(ClosableEmbeddedSolrClient.SOLR_HOME)).getAbsolutePath());
	        container.load();
	        ClosableEmbeddedSolrClient solrClient = new ClosableEmbeddedSolrClient(container, SolrCollection.uniref);
	        addUniRefStoreInfo(dataStoreManager, solrClient);
	        return solrClient;
	    }

	    @SuppressWarnings("rawtypes")
		@Bean
	    @Profile("offline")
	    public UniRefStoreClient unirefStoreClient(DataStoreManager dsm) {
	    	UniRefStoreClient storeClient = new UniRefStoreClient(VoldemortInMemoryUniRefEntryStore
	                                                                        .getInstance("uniref"));
	        dsm.addStore(DataStoreManager.StoreType.UNIREF, storeClient);
	        return storeClient;
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

	    private void addUniRefStoreInfo(DataStoreManager dsm, ClosableEmbeddedSolrClient unirefSolrClient) throws URISyntaxException {
	        dsm.addDocConverter(DataStoreManager.StoreType.UNIREF,
	                            new UniRefDocumentConverter(TaxonomyRepoMocker.getTaxonomyRepo()));
	       
	        dsm.addSolrClient(DataStoreManager.StoreType.UNIREF, unirefSolrClient);
	      
	    }
}

