package org.uniprot.api.common.repository.stream.rdf;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import lombok.Builder;
import lombok.extern.slf4j.Slf4j;
import net.jodah.failsafe.Failsafe;
import net.jodah.failsafe.RetryPolicy;

import org.uniprot.api.common.repository.search.SolrRequest;
import org.uniprot.api.common.repository.stream.common.BatchIterable;
import org.uniprot.api.common.repository.stream.document.DocumentIdStream;
import org.uniprot.api.rest.service.RDFService;

/**
 * @author sahmad
 * @created 26/01/2021
 */
@Slf4j
@Builder
public class RDFStreamer {
    private final RDFService<String> rdfService;
    private final RetryPolicy<Object> rdfFetchRetryPolicy; // retry policy for RDF rest call
    private final String rdfProlog; // rdf prefix
    private final int rdfBatchSize; // number of accession in rdf rest request
    private final DocumentIdStream idStream;

    protected Stream<String> fetchIds(SolrRequest solrRequest) {
        return idStream.fetchIds(solrRequest);
    }

    public Stream<String> idsToRDFStoreStream(SolrRequest solrRequest) {
        List<String> accessions = fetchIds(solrRequest).collect(Collectors.toList());
        return streamRDFXML(accessions.stream());
    }

    public Stream<String> streamRDFXML(Stream<String> entryIds) {
        RDFStreamer.BatchRDFStoreIterable batchRDFStoreIterable =
                new RDFStreamer.BatchRDFStoreIterable(
                        entryIds::iterator, rdfService, rdfFetchRetryPolicy, rdfBatchSize);

        Stream<String> rdfStringStream =
                StreamSupport.stream(batchRDFStoreIterable.spliterator(), false)
                        .flatMap(Collection::stream)
                        .onClose(
                                () ->
                                        log.info(
                                                "Finished streaming over search results and fetching from RDF server."));

        // prepend rdf prolog then rdf data and then append closing rdf tag
        return Stream.concat(
                Stream.of(rdfProlog),
                Stream.concat(rdfStringStream, Stream.of(RDFService.RDF_CLOSE_TAG)));
    }

    // iterable for RDF streaming
    private static class BatchRDFStoreIterable extends BatchIterable<String> {
        private final RDFService<String> rdfService;
        private final RetryPolicy<Object> retryPolicy;

        BatchRDFStoreIterable(
                Iterable<String> sourceIterable,
                RDFService<String> rdfService,
                RetryPolicy<Object> retryPolicy,
                int batchSize) {
            super(sourceIterable, batchSize);
            this.rdfService = rdfService;
            this.retryPolicy = retryPolicy;
        }

        @Override
        protected List<String> convertBatch(List<String> batch) {
            return Failsafe.with(retryPolicy)
                    .onFailure(
                            throwable ->
                                    log.error(
                                            "Call to RDF server failed for accessions {} with error {}",
                                            batch,
                                            throwable.getFailure().getMessage()))
                    .get(() -> rdfService.getEntries(batch));
        }
    }
}
