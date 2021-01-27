package org.uniprot.api.common.repository.stream.rdf;

import java.util.stream.Stream;

import lombok.Builder;
import net.jodah.failsafe.RetryPolicy;

import org.uniprot.api.common.repository.search.SolrRequest;
import org.uniprot.api.common.repository.stream.document.TupleStreamDocumentIdStream;
import org.uniprot.api.rest.service.RDFService;

/**
 * @author sahmad
 * @created 26/01/2021
 */
public class TupleStreamRDFStreamer extends AbstractRDFStreamer {
    private final TupleStreamDocumentIdStream idStream;

    @Builder
    public TupleStreamRDFStreamer(
            RDFService<String> rdfService,
            RetryPolicy<Object> rdfFetchRetryPolicy,
            String rdfProlog,
            int rdfBatchSize,
            TupleStreamDocumentIdStream idStream) {
        super(rdfService, rdfFetchRetryPolicy, rdfProlog, rdfBatchSize);
        this.idStream = idStream;
    }

    @Override
    protected Stream<String> fetchIds(SolrRequest solrRequest) {
        return idStream.fetchIds(solrRequest);
    }
}
