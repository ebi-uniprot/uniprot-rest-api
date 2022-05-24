package org.uniprot.api.common.repository.stream.document;

import java.io.IOException;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import lombok.Builder;
import lombok.extern.slf4j.Slf4j;

import org.apache.solr.client.solrj.io.stream.TupleStream;
import org.uniprot.api.common.repository.search.SolrRequest;
import org.uniprot.api.common.repository.stream.common.TupleStreamIterable;
import org.uniprot.api.common.repository.stream.common.TupleStreamTemplate;
import org.uniprot.api.common.repository.stream.store.StreamerConfigProperties;

/**
 * @author sahmad
 * @created 26/01/2021
 */
@Builder
@Slf4j
public class TupleStreamDocumentIdStream implements DocumentIdStream {
    private final TupleStreamTemplate tupleStreamTemplate;
    private final StreamerConfigProperties streamConfig;

    @SuppressWarnings("squid:S2095")
    public Stream<String> fetchIds(SolrRequest solrRequest) {
        TupleStream tupleStream = tupleStreamTemplate.create(solrRequest);
        try {
            tupleStream.open();
            return StreamSupport.stream(
                            new TupleStreamIterable(tupleStream, streamConfig.getIdFieldName())
                                    .spliterator(),
                            false)
                    .onClose(() -> closeTupleStream(tupleStream));
        } catch (Exception e) {
            closeTupleStream(tupleStream);
            throw new IllegalStateException(e);
        }
    }

    private void closeTupleStream(TupleStream tupleStream) {
        try {
            tupleStream.close();
            log.info("TupleStream closed: {}", tupleStream.getStreamNodeId());
        } catch (IOException e) {
            String message = "Error when closing TupleStream";
            log.error(message, e);
            throw new IllegalStateException(message, e);
        }
    }
}
