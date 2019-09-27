package org.uniprot.api.uniref.repository;

import org.apache.http.client.HttpClient;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;
import org.uniprot.api.common.repository.search.SolrRequest;
import org.uniprot.api.common.repository.search.SolrRequestConverter;
import org.uniprot.api.uniref.repository.store.UniRefStoreClient;
import org.uniprot.store.datastore.voldemort.uniref.VoldemortInMemoryUniRefEntryStore;

import java.net.URISyntaxException;

import static org.mockito.Mockito.mock;

/**
 *
 * @author jluo
 * @date: 23 Aug 2019
 *
*/
@TestConfiguration
public class DataStoreTestConfig {

	    @Bean
	    @Profile("offline")
	    public HttpClient httpClient() {
	        return mock(HttpClient.class);
	    }

	    @Bean
	    @Profile("offline")
        public SolrClient unirefSolrClient() throws URISyntaxException {
            return mock(SolrClient.class);
	    }

		@Bean
	    @Profile("offline")
        public UniRefStoreClient unirefStoreClient() {
            return new UniRefStoreClient(VoldemortInMemoryUniRefEntryStore.getInstance("uniref"));
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

