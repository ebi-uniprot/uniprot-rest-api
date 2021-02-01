package org.uniprot.api.common.repository.stream.store;

import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import lombok.extern.slf4j.Slf4j;

import org.apache.solr.client.solrj.io.Tuple;
import org.apache.solr.client.solrj.io.stream.TupleStream;
import org.mockito.stubbing.OngoingStubbing;

/**
 * @author sahmad
 * @created 27/01/2021
 */
@Slf4j
public class TupleStreamUtils {
    public static TupleStream tupleStream(Collection<String> values) {
        TupleStream mockTupleStream = mock(TupleStream.class);

        try {
            OngoingStubbing<Tuple> ongoingStubbing = lenient().when(mockTupleStream.read());
            for (String value : values) {
                log.debug("hello " + value);
                ongoingStubbing = ongoingStubbing.thenReturn(tuple(value));
            }

            ongoingStubbing.thenReturn(endTuple());
        } catch (IOException e) {
            log.error("Error when tupleStream", e);
        }

        return mockTupleStream;
    }

    private static Tuple tuple(String accession) {
        Map<String, String> valueMap = new HashMap<>();
        valueMap.put("id", accession);
        return new Tuple(valueMap);
    }

    private static Tuple endTuple() {
        Map<String, String> eofMap = new HashMap<>();
        eofMap.put("EOF", "");
        return new Tuple(eofMap);
    }
}
