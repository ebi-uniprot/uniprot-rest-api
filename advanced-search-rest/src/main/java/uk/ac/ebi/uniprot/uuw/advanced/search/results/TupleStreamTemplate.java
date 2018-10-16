package uk.ac.ebi.uniprot.uuw.advanced.search.results;

import lombok.Builder;
import org.apache.solr.client.solrj.io.SolrClientCache;
import org.apache.solr.client.solrj.io.stream.StreamContext;
import org.apache.solr.client.solrj.io.stream.TupleStream;
import org.apache.solr.client.solrj.io.stream.expr.DefaultStreamFactory;
import org.apache.solr.client.solrj.io.stream.expr.StreamFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Sort;

import java.io.IOException;

/**
 * Created 21/08/18
 *
 * @author Edd
 */
@Builder
public class TupleStreamTemplate {
    private static final Logger LOGGER = LoggerFactory.getLogger(TupleStreamTemplate.class);
    private String zookeeperHost;
    private String requestHandler;
    private String key;
    private String collection;

    public TupleStream create(String query) {
        return create(query, key, new Sort(Sort.Direction.ASC, key));
    }

    public TupleStream create(String query, String key, Sort sort) {
        TupleStreamBuilder streamBuilder = TupleStreamBuilder.builder()
                .zookeeperHost(zookeeperHost)
                .collection(collection)
                .key(key)
                .order(sort)
                .requestHandler(requestHandler)
                .streamContext(createStreamContext())
                .build();

        return streamBuilder.createFor(query);
    }

    @Builder
    private static class TupleStreamBuilder {
        private final String collection;
        private String zookeeperHost;
        private String requestHandler;
        private Sort order;
        private String key;
        private StreamContext streamContext;

        private TupleStream createFor(String query) {
            try {
                StreamFactory streamFactory = new DefaultStreamFactory()
                        .withCollectionZkHost(collection, zookeeperHost);
                String request =
                        String.format("search(%s, q=\"%s\", fl=\"%s\", sort=\"%s\", qt=\"/export\")",
                                      collection, query, key, order.toString());

                TupleStream tupleStream = streamFactory.constructStream(request);
                tupleStream.setStreamContext(streamContext);
                return tupleStream;
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
        streamContext.workerID = collection
                .hashCode(); // this should be the same for each collection, so that they share client caches
        streamContext.numWorkers = 1;
        SolrClientCache solrClientCache = new SolrClientCache();
        streamContext.setSolrClientCache(solrClientCache);
        return streamContext;
    }
}