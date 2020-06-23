package org.uniprot.api.common.repository.store;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

import lombok.Builder;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import org.apache.http.client.HttpClient;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.io.SolrClientCache;
import org.apache.solr.client.solrj.io.stream.StreamContext;
import org.apache.solr.client.solrj.io.stream.TupleStream;
import org.apache.solr.client.solrj.io.stream.expr.*;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.uniprot.api.common.exception.ServiceException;
import org.uniprot.api.common.repository.search.SolrRequest;
import org.uniprot.api.common.repository.search.SolrRequestConverter;
import org.uniprot.core.util.Utils;
import org.uniprot.store.search.SolrCollection;

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
 */
@Getter
@Builder
@Slf4j
public class TupleStreamTemplate {
    private final StreamerConfigProperties streamConfig;
    private final HttpClient httpClient;
    private final SolrClient solrClient;
    private final SolrCollection collection;
    private final SolrRequestConverter solrRequestConverter;
    private StreamFactory streamFactory;
    private StreamContext streamContext;

    public TupleStream create(SolrRequest request) {
        initTupleStreamFactory(streamConfig.getZkHost(), streamConfig.getCollection());
        initStreamContext(streamConfig.getZkHost(), httpClient);
        validateResponse(request);

        TupleStreamBuilder streamBuilder =
                TupleStreamBuilder.builder()
                        .streamFactory(streamFactory)
                        .idField(streamConfig.getIdFieldName())
                        .requestHandler(streamConfig.getRequestHandler())
                        .streamContext(streamContext)
                        .build();

        return streamBuilder.createFor(request);
    }

    void validateResponse(SolrRequest request) {
        SolrRequest slimRequest =
                SolrRequest.builder()
                        .query(request.getQuery())
                        .filterQueries(request.getFilterQueries())
                        .queryBoosts(request.getQueryBoosts())
                        .build();
        try {
            QueryResponse response =
                    solrClient.query(
                            collection.name(), solrRequestConverter.toSolrQuery(slimRequest));
            if (response.getResults().getNumFound() > streamConfig.getStoreMaxCountToRetrieve()) {
                throw new ServiceException(
                        "Too many results to retrieve. Please refine your query or consider fetching batch by batch");
            }
        } catch (SolrServerException | IOException e) {
            throw new ServiceException("Server error when querying Solr", e);
        }
        log.debug("Request to stream is valid: " + request);
    }

    private void initTupleStreamFactory(String zookeeperHost, String collection) {
        if (streamFactory == null) {
            streamFactory =
                    new DefaultStreamFactory().withCollectionZkHost(collection, zookeeperHost);
            log.info("Created new DefaultStreamFactory");
        } else {
            log.info("DefaultStreamFactory already created");
        }
    }

    @Builder
    static class TupleStreamBuilder {
        private final StreamFactory streamFactory;
        private final String requestHandler;
        private final String idField;
        private final StreamContext streamContext;

        private TupleStream createFor(SolrRequest request) {
            try {
                StreamExpression requestExpression = new StreamExpression("search");
                requestExpression.addParameter(
                        new StreamExpressionValue(streamFactory.getDefaultCollection()));
                requestExpression.addParameter(
                        new StreamExpressionNamedParameter("q", request.getQuery()));
                requestExpression.addParameter(
                        new StreamExpressionNamedParameter(
                                "q.op", request.getDefaultQueryOperator().name()));
                if (!request.getFilterQueries().isEmpty()) {
                    String filterQuery =
                            request.getFilterQueries().stream()
                                    .collect(Collectors.joining(" ", "(", ")"));
                    requestExpression.addParameter(
                            new StreamExpressionNamedParameter("fq", filterQuery));
                }
                requestExpression.addParameter(
                        new StreamExpressionNamedParameter(
                                "fl", fieldsToReturn(idField, request.getSorts())));
                requestExpression.addParameter(
                        new StreamExpressionNamedParameter(
                                "sort", sortToString(request.getSorts())));
                requestExpression.addParameter(
                        new StreamExpressionNamedParameter("qt", requestHandler));

                requestExpression.addParameter(new StreamExpressionNamedParameter("df", "content"));

                TupleStream tupleStream = streamFactory.constructStream(requestExpression);
                tupleStream.setStreamContext(streamContext);
                return tupleStream;
            } catch (IOException e) {
                log.error("Could not create TupleStream", e);
                throw new IllegalStateException();
            }
        }

        static String fieldsToReturn(String idField, List<SolrQuery.SortClause> order) {
            String sortFields =
                    order.stream()
                            .map(SolrQuery.SortClause::getItem)
                            .filter(o -> !o.equalsIgnoreCase("score"))
                            .collect(Collectors.joining(","));
            return idField + (Utils.nullOrEmpty(sortFields) ? "" : "," + sortFields);
        }

        static String sortToString(List<SolrQuery.SortClause> order) {
            return order.stream()
                    .filter(o -> !o.getItem().equalsIgnoreCase("score"))
                    .map(o -> o.getItem() + " " + o.getOrder().name())
                    .collect(Collectors.joining(","));
        }
    }

    /**
     * For tweaking, see: https://www.mail-archive.com/solr-user@lucene.apache.org/msg131338.html
     */
    private void initStreamContext(String zookeeperHost, HttpClient httpClient) {
        if (streamContext == null) {
            StreamContext context = new StreamContext();
            // this should be the same for each collection, so that
            // they share client caches
            context.workerID = streamConfig.getCollection().hashCode();
            context.numWorkers = 1;
            SolrClientCache solrClientCache = new SolrClientCache(httpClient);
            solrClientCache.getCloudSolrClient(zookeeperHost);
            context.setSolrClientCache(solrClientCache);
            this.streamContext = context;
        }
    }
}
