package org.uniprot.api.common.repository.solrstream;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

import lombok.Builder;

import org.apache.http.client.HttpClient;
import org.apache.solr.client.solrj.io.SolrClientCache;
import org.apache.solr.client.solrj.io.stream.StreamContext;
import org.apache.solr.client.solrj.io.stream.TupleStream;
import org.apache.solr.client.solrj.io.stream.expr.DefaultStreamFactory;
import org.apache.solr.client.solrj.io.stream.expr.StreamExpression;
import org.apache.solr.client.solrj.io.stream.expr.StreamFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class is responsible for simplifying the creation of {@link TupleStream} instances, which
 * enable the exporting of entire result sets from Solr. This template class should be initialised
 * with correct configuration details, e.g., zookeeper address and collection. This template
 * instance can then be used to create specific {@link TupleStream}s for a given query, using the
 * original configuration details specified in the template.
 *
 * <p>Created 21/08/18
 *
 * @author Edd
 * @author sahmad
 */
@Builder
public class FacetTupleStreamTemplate {
    private static final Logger LOGGER = LoggerFactory.getLogger(FacetTupleStreamTemplate.class);
    private String zookeeperHost;
    private String collection;
    private HttpClient httpClient;

    public TupleStream create(SolrStreamingFacetRequest request) {
        try {
            // create a solr streaming facet function call for each `facet`
            List<StreamExpression> facetExpressions =
                    request.getFacets().stream()
                            .map(
                                    facet ->
                                            new FacetStreamExpression.FacetStreamExpressionBuilder(
                                                            this.collection, facet, request)
                                                    .build())
                            .collect(Collectors.toList());

            // we can replace list with plist function when solr >= 7.5
            ListStreamExpression listStreamExpression = new ListStreamExpression(facetExpressions);
            StreamFactory streamFactory =
                    new DefaultStreamFactory()
                            .withCollectionZkHost(this.collection, this.zookeeperHost);
            TupleStream tupleStream = streamFactory.constructStream(listStreamExpression);
            StreamContext clientContext = createStreamContext();
            tupleStream.setStreamContext(clientContext);
            return tupleStream;
        } catch (IOException e) {
            LOGGER.error("Could not create TupleStream", e);
            throw new IllegalStateException();
        }
    }

    /**
     * For tweaking, see: https://www.mail-archive.com/solr-user@lucene.apache.org/msg131338.html
     */
    private StreamContext createStreamContext() {
        StreamContext streamContext = new StreamContext();
        streamContext.workerID =
                this.collection.hashCode(); // this should be the same for each collection, so that
        // they share client caches
        streamContext.numWorkers = 1;
        SolrClientCache solrClientCache = new SolrClientCache(this.httpClient);
        streamContext.setSolrClientCache(solrClientCache);
        return streamContext;
    }
}
