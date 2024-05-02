package org.uniprot.api.common.repository.solrstream;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.solr.client.solrj.io.stream.StreamContext;
import org.apache.solr.client.solrj.io.stream.TupleStream;
import org.apache.solr.client.solrj.io.stream.expr.StreamExpression;
import org.apache.solr.client.solrj.io.stream.expr.StreamFactory;
import org.uniprot.api.common.repository.search.facet.FacetConfig;

import lombok.Builder;
import lombok.extern.slf4j.Slf4j;

/**
 * This class is responsible for simplifying the creation of {@link TupleStream} instances for facet
 * functions and/or a search function of solr streaming, which enable us to get facets from Solr.
 * This template class should be initialised with correct configuration details, e.g., zookeeper
 * address and collection. This template instance can then be used to create specific {@link
 * TupleStream}s for a given query, using the original configuration details specified in the
 * template.
 *
 * <p>Created 21/08/18
 *
 * @author Edd
 * @author sahmad
 */
@Builder
@Slf4j
public class FacetTupleStreamTemplate extends AbstractTupleStreamTemplate {
    private final String zookeeperHost;
    private final String collection;

    public TupleStream create(SolrStreamFacetRequest request, FacetConfig facetConfig) {
        try {
            List<StreamExpression> expressions = new ArrayList<>();
            // add search function if needed
            if (request.isSearchAccession()) {
                StreamExpression searchExpression =
                        new SearchStreamExpression(this.collection, request);
                expressions.add(searchExpression);
            }
            // create a solr streaming facet function call for each `facet`
            List<StreamExpression> facetExpressions =
                    request.getFacets().stream()
                            .map(
                                    facet ->
                                            new FacetStreamExpression(
                                                    this.collection, facet, request, facetConfig))
                            .collect(Collectors.toList());

            expressions.addAll(facetExpressions);
            // we can replace list with plist function when solr >= 7.5
            ListStreamExpression listStreamExpression = new ListStreamExpression(expressions);
            StreamFactory streamFactory = getStreamFactory(this.zookeeperHost, this.collection);
            TupleStream tupleStream = streamFactory.constructStream(listStreamExpression);
            StreamContext clientContext = getStreamContext(this.collection);
            tupleStream.setStreamContext(clientContext);
            return tupleStream;
        } catch (IOException e) {
            log.error("Could not create TupleStream", e);
            throw new IllegalStateException();
        }
    }
}
