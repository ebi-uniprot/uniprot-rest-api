package uk.ac.ebi.uniprot.uuw.advanced.search.repository;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrRequest;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.embedded.EmbeddedSolrServer;
import org.apache.solr.common.util.NamedList;
import org.apache.solr.core.CoreContainer;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import java.io.File;
import java.io.IOException;

/**
 * A test configuration providing {@link SolrClient} beans that override those set in {@link RepositoryConfig}.
 * For example, this allows us to use embedded Solr data stores, rather than live HTTP/Zookeeper stores hosted on
 * external VMs.
 *
 * Created 14/09/18
 *
 * @author Edd
 */
@TestConfiguration
public class SolrClientTestConfig {
    @Bean(destroyMethod = "close")
    @Primary
    public SolrClient uniProtSolrClient(RepositoryConfigProperties config) throws IOException {
        return new ClosableEmbeddedSolrClient(config, SolrCollection.uniprot);
    }

    public static class ClosableEmbeddedSolrClient extends SolrClient {
        private static final String SOLR_HOME = "solr.home";
        private final SolrDataStoreManager storeManager;
        private final EmbeddedSolrServer server;

        ClosableEmbeddedSolrClient(RepositoryConfigProperties config, SolrCollection collection) throws IOException {
            this.storeManager = new SolrDataStoreManager();
            CoreContainer container = new CoreContainer(new File(System.getProperty(SOLR_HOME)).getAbsolutePath());
            container.load();
            this.server = new EmbeddedSolrServer(container, collection.name());
        }

        @Override
        public NamedList<Object> request(SolrRequest solrRequest, String s) throws SolrServerException, IOException {
            return server.request(solrRequest, s);
        }

        @Override
        public void close() throws IOException {
            // close server (=> close CoreContainer) before deleting temporary directory maintained by storeManager
            server.close();
            storeManager.cleanUp();
        }
    }
}
