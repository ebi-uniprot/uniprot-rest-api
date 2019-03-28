package uk.ac.ebi.uniprot.api.common.repository.search;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrRequest;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.embedded.EmbeddedSolrServer;
import org.apache.solr.common.util.NamedList;
import org.apache.solr.core.CoreContainer;

import uk.ac.ebi.uniprot.api.common.repository.search.SolrCollection;

import java.io.File;
import java.io.IOException;

/**
 * Created 19/09/18
 *
 * @author Edd
 */
public class ClosableEmbeddedSolrClient extends SolrClient {
    private static final String SOLR_HOME = "solr.home";
    private final EmbeddedSolrServer server;

    public ClosableEmbeddedSolrClient(SolrCollection collection) {
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
        server.close();
    }
}
