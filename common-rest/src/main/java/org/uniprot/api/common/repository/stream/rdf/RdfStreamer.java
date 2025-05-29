package org.uniprot.api.common.repository.stream.rdf;

import java.util.Collection;
import java.util.List;
import java.util.function.LongConsumer;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.uniprot.api.common.repository.stream.common.BatchIterable;
import org.uniprot.api.rest.service.RdfService;

import lombok.extern.slf4j.Slf4j;
import net.jodah.failsafe.Failsafe;
import net.jodah.failsafe.RetryPolicy;

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

    public Stream<String> stream(List<String> entryIds, String dataType, String format) {
        return stream(entryIds, dataType, format, null);
    }

    public Stream<String> stream(
            List<String> entryIds, String dataType, String format, LongConsumer consumer) {
        if (entryIds == null || entryIds.isEmpty()) {
            return Stream.empty();
        }
        // get the first batch to make a call to extract prolog
        List<String> firstBatch = entryIds.subList(0, Math.min(this.batchSize, entryIds.size()));
        BatchRdfXmlStoreIterable batchRDFXMLStoreIterable =
                getBatchRdfXmlStoreIterable(entryIds.stream(), dataType, format);

        Stream<String> rdfStringStream =
                createRdfStream(batchRDFXMLStoreIterable, dataType, format, consumer);
        //        String temp = concatenateWithPrologAndClosingTag(firstBatch, dataType, format,
        // rdfStringStream).collect(Collectors.joining());
        //        System.out.println(temp);
        //        return Stream.of(temp);
        return concatenateWithPrologAndClosingTag(firstBatch, dataType, format, rdfStringStream);
    }

    private Stream<String> createRdfStream(
            BatchRdfXmlStoreIterable batchIterator,
            String dataType,
            String format,
            LongConsumer consumer) {
        Stream<String> baseStream =
                StreamSupport.stream(batchIterator.spliterator(), false)
                        .flatMap(Collection::stream);

        if (consumer != null) {
            baseStream =
                    baseStream.map(
                            response -> {
                                consumer.accept(
                                        rdfEntryCountProvider.getEntryCount(
                                                response, dataType, format));
                                return response;
                            });
        }

        return baseStream.onClose(
                () ->
                        log.debug(
                                "Finished streaming over search results and fetching from RDF server."));
    }

    private BatchRdfXmlStoreIterable getBatchRdfXmlStoreIterable(
            Stream<String> entryIds, String dataType, String format) {
        return new BatchRdfXmlStoreIterable(
                entryIds::iterator,
                rdfServiceFactory.getRdfService(dataType, format),
                rdfFetchRetryPolicy,
                batchSize);
    }

    private Stream<String> concatenateWithPrologAndClosingTag(
            List<String> firstBatch,
            String dataType,
            String format,
            Stream<String> rdfStringStream) {
        // prepend rdf prolog then rdf data and then append closing rdf tag
        return Stream.concat(
                Stream.of(
                        prologProvider.getProLog(
                                firstBatch, this.rdfServiceFactory, dataType, format)),
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
