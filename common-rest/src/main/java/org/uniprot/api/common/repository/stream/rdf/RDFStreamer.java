package org.uniprot.api.common.repository.stream.rdf;

import lombok.extern.slf4j.Slf4j;
import net.jodah.failsafe.Failsafe;
import net.jodah.failsafe.RetryPolicy;
import org.uniprot.api.common.repository.stream.common.BatchIterable;
import org.uniprot.api.rest.service.RDFXMLClient;

import java.util.Collection;
import java.util.List;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

@Slf4j
public class RDFStreamer {
    private final int batchSize;
    private final PrologProvider prologProvider;
    private final RDFXMLClient RDFXMLClient;
    private final RetryPolicy<Object> rdfFetchRetryPolicy;

    public RDFStreamer(
            int batchSize,
            PrologProvider prologProvider,
            RDFXMLClient RDFXMLClient,
            RetryPolicy<Object> rdfFetchRetryPolicy) {
        this.batchSize = batchSize;
        this.prologProvider = prologProvider;
        this.RDFXMLClient = RDFXMLClient;
        this.rdfFetchRetryPolicy = rdfFetchRetryPolicy;
    }

    public Stream<String> stream(Stream<String> entryIds, String type, String format) {
        BatchRDFXMLStoreIterable batchRDFXMLStoreIterable =
                new BatchRDFXMLStoreIterable(
                        type,
                        format,
                        entryIds::iterator,
                        RDFXMLClient,
                        rdfFetchRetryPolicy,
                        batchSize);

        Stream<String> rdfStringStream =
                StreamSupport.stream(batchRDFXMLStoreIterable.spliterator(), false)
                        .flatMap(Collection::stream)
                        .onClose(
                                () ->
                                        log.debug(
                                                "Finished streaming over search results and fetching from RDF server."));

        // prepend rdf prolog then rdf data and then append closing rdf tag
        return Stream.concat(
                Stream.of(prologProvider.getProLog(type, format)),
                Stream.concat(rdfStringStream, Stream.of(prologProvider.getClosingTag(format))));
    }

    // iterable for RDF streaming
    private static class BatchRDFXMLStoreIterable extends BatchIterable<String> {
        private final String type;
        private final String format;
        private final RDFXMLClient RDFXMLClient;
        private final RetryPolicy<Object> retryPolicy;

        BatchRDFXMLStoreIterable(
                String type,
                String format,
                Iterable<String> sourceIterable,
                RDFXMLClient RDFXMLClient,
                RetryPolicy<Object> retryPolicy,
                int batchSize) {
            super(sourceIterable, batchSize);
            this.type = type;
            this.format = format;
            this.RDFXMLClient = RDFXMLClient;
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
                    .get(() -> RDFXMLClient.getEntries(batch, type, format));
        }
    }
}
