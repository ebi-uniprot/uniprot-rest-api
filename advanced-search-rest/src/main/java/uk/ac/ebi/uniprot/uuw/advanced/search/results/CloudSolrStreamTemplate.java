package uk.ac.ebi.uniprot.uuw.advanced.search.results;

import lombok.Builder;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.io.SolrClientCache;
import org.apache.solr.client.solrj.io.stream.CloudSolrStream;
import org.apache.solr.client.solrj.io.stream.StreamContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * Created 21/08/18
 *
 * @author Edd
 */
@Builder
public class CloudSolrStreamTemplate {
    private static final Logger LOGGER = LoggerFactory.getLogger(CloudSolrStreamTemplate.class);
    private String zookeeperHost;
    private String requestHandler;
    private SolrQuery.ORDER order;
    private String key;
    private String collection;
    private StreamContext streamContext;

    public CloudSolrStream create(String query) {
        CloudSolrStreamBuilder streamBuilder = CloudSolrStreamBuilder.builder()
                .zookeeperHost(zookeeperHost)
                .collection(collection)
                .key(key)
                .order(order)
                .requestHandler(requestHandler)
                .streamContext(createStreamContext())
                .build();

        return streamBuilder.build(query);
    }

    @Builder
    private static class CloudSolrStreamBuilder {
        private final String collection;
        private String zookeeperHost;
        private String requestHandler;
        private SolrQuery.ORDER order;
        private String key;
        private String query;
        private StreamContext streamContext;

        private CloudSolrStream build(String query) {
            SolrQuery solrQuery = new SolrQuery();
            solrQuery.setQuery(query);
            solrQuery.setSort(key, order);
            solrQuery.setFields(key);
            solrQuery.setRequestHandler("/export");

            try {
                CloudSolrStream cStream = new CloudSolrStream(zookeeperHost, collection, solrQuery);
                cStream.setStreamContext(streamContext);
                return cStream;
            } catch (IOException e) {
                LOGGER.error("Could not create CloudSolrStream", e);
                throw new IllegalStateException();
            }
        }

    }

    /**
     * For tweaking, see: https://www.mail-archive.com/solr-user@lucene.apache.org/msg131338.html
     */
    private StreamContext createStreamContext() {
        StreamContext streamContext = new StreamContext();
        streamContext.workerID = collection.hashCode(); // this should be the same for each collection, so that they share client caches
        streamContext.numWorkers = 1;
        SolrClientCache solrClientCache = new SolrClientCache();
        streamContext.setSolrClientCache(solrClientCache);
        return streamContext;
    }
}