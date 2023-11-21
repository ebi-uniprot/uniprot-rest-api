package org.uniprot.api.common.repository.stream.rdf;

import lombok.extern.slf4j.Slf4j;
import net.jodah.failsafe.Failsafe;
import net.jodah.failsafe.RetryPolicy;
import org.uniprot.api.common.repository.stream.common.BatchIterable;
import org.uniprot.api.rest.service.RdfService;

import java.util.Collection;
import java.util.List;
import java.util.function.LongConsumer;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

@Slf4j
public class RdfStreamer {
    private final int batchSize;
    private final PrologProvider prologProvider;
    private final RdfServiceFactory rdfServiceFactory;
    private final RetryPolicy<Object> rdfFetchRetryPolicy;
    private final RdfEntryCountProvider rdfEntryCountProvider;

    public RdfStreamer(
            int batchSize,
            PrologProvider prologProvider,
            RdfServiceFactory rdfServiceFactory,
            RetryPolicy<Object> rdfFetchRetryPolicy,
            RdfEntryCountProvider rdfEntryCountProvider) {
        this.batchSize = batchSize;
        this.prologProvider = prologProvider;
        this.rdfServiceFactory = rdfServiceFactory;
        this.rdfFetchRetryPolicy = rdfFetchRetryPolicy;
        this.rdfEntryCountProvider = rdfEntryCountProvider;
    }

    public Stream<String> stream(Stream<String> entryIds, String dataType, String format) {
        BatchRdfXmlStoreIterable batchRDFXMLStoreIterable =
                getBatchRdfXmlStoreIterable(entryIds, dataType, format);

        Stream<String> rdfStringStream =
                StreamSupport.stream(batchRDFXMLStoreIterable.spliterator(), false)
                        .flatMap(Collection::stream)
                        .onClose(
                                () ->
                                        log.debug(
                                                "Finished streaming over search results and fetching from RDF server."));

        // prepend rdf prolog then rdf data and then append closing rdf tag
        return getFullResponseAsStream(dataType, format, rdfStringStream);
    }

    public Stream<String> stream(
            Stream<String> entryIds, String dataType, String format, LongConsumer consumer) {
        BatchRdfXmlStoreIterable batchRDFXMLStoreIterable =
                getBatchRdfXmlStoreIterable(entryIds, dataType, format);

        Stream<String> rdfStringStream =
                StreamSupport.stream(batchRDFXMLStoreIterable.spliterator(), false)
                        .flatMap(Collection::stream)
                        .map(
                                singleBatchResponse -> {
                                    consumer.accept(
                                            rdfEntryCountProvider.getEntryCount(
                                                    singleBatchResponse, dataType, format));
                                    return singleBatchResponse;
                                })
                        .onClose(
                                () ->
                                        log.debug(
                                                "Finished streaming over search results and fetching from RDF server."));

        return getFullResponseAsStream(dataType, format, rdfStringStream);
    }

    private BatchRdfXmlStoreIterable getBatchRdfXmlStoreIterable(
            Stream<String> entryIds, String dataType, String format) {
        return new BatchRdfXmlStoreIterable(
                entryIds::iterator,
                rdfServiceFactory.getRdfService(dataType, format),
                rdfFetchRetryPolicy,
                batchSize);
    }

    private Stream<String> getFullResponseAsStream(
            String dataType, String format, Stream<String> rdfStringStream) {
        // prepend rdf prolog then rdf data and then append closing rdf tag
        return Stream.concat(
                Stream.of(prologProvider.getProLog(dataType, format)),
                Stream.concat(rdfStringStream, Stream.of(prologProvider.getClosingTag(format))));
    }

    // iterable for RDF streaming
    private static class BatchRdfXmlStoreIterable extends BatchIterable<String> {
        private final RdfService<String> rdfService;
        private final RetryPolicy<Object> retryPolicy;

        BatchRdfXmlStoreIterable(
                Iterable<String> sourceIterable,
                RdfService<String> rdfService,
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
