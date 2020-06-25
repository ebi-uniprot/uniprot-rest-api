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
import org.apache.solr.client.solrj.io.stream.StreamContext;
import org.apache.solr.client.solrj.io.stream.TupleStream;
import org.apache.solr.client.solrj.io.stream.expr.StreamExpression;
import org.apache.solr.client.solrj.io.stream.expr.StreamExpressionNamedParameter;
import org.apache.solr.client.solrj.io.stream.expr.StreamExpressionValue;
import org.apache.solr.client.solrj.io.stream.expr.StreamFactory;
import org.uniprot.api.common.repository.search.SolrRequest;
import org.uniprot.api.common.repository.solrstream.AbstractTupleStreamTemplate;
import org.uniprot.core.util.Utils;

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
public class TupleStreamTemplate extends AbstractTupleStreamTemplate {
    private final StreamerConfigProperties streamConfig;
    private final HttpClient httpClient;

    public TupleStream create(SolrRequest request) {
        StreamContext streamContext =
                getStreamContext(
                        streamConfig.getZkHost(), streamConfig.getCollection(), httpClient);
        StreamFactory streamFactory =
                getStreamFactory(streamConfig.getZkHost(), streamConfig.getCollection());
        TupleStreamBuilder streamBuilder =
                TupleStreamBuilder.builder()
                        .streamFactory(streamFactory)
                        .idField(streamConfig.getIdFieldName())
                        .requestHandler(streamConfig.getRequestHandler())
                        .streamContext(streamContext)
                        .build();

        return streamBuilder.createFor(request);
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
}
