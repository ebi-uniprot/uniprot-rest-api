package uk.ac.ebi.uniprot.common.repository.store;

import lombok.Builder;
import org.apache.solr.client.solrj.io.SolrClientCache;
import org.apache.solr.client.solrj.io.stream.StreamContext;
import org.apache.solr.client.solrj.io.stream.TupleStream;
import org.apache.solr.client.solrj.io.stream.expr.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Sort;

import java.io.IOException;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/**
 * This class is responsible for simplifying the creation {@link TupleStream} instances, which enable the exporting
 * of entire result sets from Solr. This template class is initialised with correct configuration details,
 * e.g., zookeeper address and collection, in {@link ResultsConfig}. This template instance can then be used to
 * create specific {@link TupleStream}s for a given query, using the original configuration details specified in the
 * template.
 *
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

    public TupleStream create(String query,String filterQuery) {
        return create(query, filterQuery, key, new Sort(Sort.Direction.ASC, key));
    }

    public TupleStream create(String query, String filterQuery,String key, Sort sort) {
        TupleStreamBuilder streamBuilder = TupleStreamBuilder.builder()
                .zookeeperHost(zookeeperHost)
                .collection(collection)
                .key(key)
                .order(sort)
                .requestHandler(requestHandler)
                .streamContext(createStreamContext())
                .build();

        return streamBuilder.createFor(query,filterQuery);
    }

    @Builder
    static class TupleStreamBuilder {
        private final String collection;
        private String zookeeperHost;
        private String requestHandler;
        private Sort order;
        private String key;
        private StreamContext streamContext;

        private TupleStream createFor(String query,String filterQuery) {
            try {
                StreamFactory streamFactory = new DefaultStreamFactory()
                        .withCollectionZkHost(collection, zookeeperHost);

                StreamExpression requestExpression = new StreamExpression("search");
                requestExpression.addParameter(new StreamExpressionValue(collection));
                requestExpression.addParameter(new StreamExpressionNamedParameter("q",query));
                if(filterQuery != null && !filterQuery.isEmpty()) {
                    requestExpression.addParameter(new StreamExpressionNamedParameter("fq", filterQuery));
                }
                requestExpression.addParameter(new StreamExpressionNamedParameter("fl",fieldsToReturn(key, order)));
                requestExpression.addParameter(new StreamExpressionNamedParameter("sort",sortToString(order)));
                requestExpression.addParameter(new StreamExpressionNamedParameter("qt","/export"));

                TupleStream tupleStream = streamFactory.constructStream(requestExpression);
                tupleStream.setStreamContext(streamContext);
                return tupleStream;
            } catch (IOException e) {
                LOGGER.error("Could not create TupleStream", e);
                throw new IllegalStateException();
            }
        }

        static String fieldsToReturn(String key, Sort order) {
            String sortFields = StreamSupport.stream(order.spliterator(), false)
                    .map(Sort.Order::getProperty)
                    .collect(Collectors.joining(","));
            return key + (Objects.isNull(sortFields) ? "" : "," + sortFields);
        }

        static String sortToString(Sort order) {
            return StreamSupport.stream(order.spliterator(), false)
                    .map(o -> o.getProperty() + " " + getSortDirection(o.getDirection()))
                    .collect(Collectors.joining(","));
        }

        private static String getSortDirection(Sort.Direction direction) {
            if (direction.isAscending()) {
                return "asc";
            } else {
                return "desc";
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