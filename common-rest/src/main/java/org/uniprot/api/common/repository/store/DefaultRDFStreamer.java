package org.uniprot.api.common.repository.store;

import java.util.stream.Stream;

import lombok.Builder;
import net.jodah.failsafe.RetryPolicy;

import org.uniprot.api.common.repository.search.SolrRequest;
import org.uniprot.api.rest.service.RDFService;
import org.uniprot.store.search.document.Document;

/**
 * @author sahmad
 * @created 26/01/2021
 */
public class DefaultRDFStreamer<D extends Document> extends AbstractRDFStreamer {
    private final DefaultDocumentIdStream<D> idStream;

    @Builder
    public DefaultRDFStreamer(
            RDFService<String> rdfService,
            RetryPolicy<Object> rdfFetchRetryPolicy,
            String rdfProlog,
            int rdfBatchSize,
            DefaultDocumentIdStream<D> idStream) {
        super(rdfService, rdfFetchRetryPolicy, rdfProlog, rdfBatchSize);
        this.idStream = idStream;
    }

    @Override
    protected Stream<String> fetchIds(SolrRequest solrRequest) {
        return idStream.fetchIds(solrRequest);
    }
}
