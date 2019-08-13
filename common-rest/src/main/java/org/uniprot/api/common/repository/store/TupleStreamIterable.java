package org.uniprot.api.common.repository.store;

import net.jodah.failsafe.Failsafe;
import net.jodah.failsafe.RetryPolicy;
import org.apache.solr.client.solrj.io.Tuple;
import org.apache.solr.client.solrj.io.stream.TupleStream;
import org.slf4j.Logger;

import java.io.IOException;
import java.time.Duration;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.concurrent.TimeUnit;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * This class wraps an existing {@link TupleStream}, originating from a streaming result set from Solr, and creates
 * an {@link Iterable} of its contents. This simplifies iterating through all results in a {@link TupleStream}.
 *
 * Created 21/08/18
 *
 * @author Edd
 */
class TupleStreamIterable implements Iterable<String> {
    private static final int MAX_RETRIES = 5;
    private static final Logger LOGGER = getLogger(TupleStreamIterable.class);
    private static final int DELAY = 500;
    private final TupleStream tupleStream;
    private final String id;
    private final RetryPolicy<Object> retryPolicy;
    private Tuple current = new Tuple();
    private Tuple next = current;
    private boolean atEnd = false;

    TupleStreamIterable(TupleStream tupleStream, String id) {
        this.tupleStream = tupleStream;
        this.id = id;
        retryPolicy = new RetryPolicy<>()
                .handle(IOException.class)          
                .withDelay(Duration.ofMillis(DELAY))
         //      .withDelay(500,TimeUnit.MILLISECONDS)
               .withMaxRetries(5);
        
    }

    @Override
    public Iterator<String> iterator() {
        return new Iterator<String>() {
            @Override
            public boolean hasNext() {
                if (!atEnd && next == current) {   // using ==, since we're interested in the reference
                    next = nextTupleFromStream();
                    if (next.EOF) {
                        closeTupleStream();
                        atEnd = true;
                    }
                }
                return !atEnd;
            }

            @Override
            public String next() {
                if (!atEnd && next != current) { // hasNext has already retrieved the next element, to see if it exists
                    current = next;
                } else {
                    current = nextTupleFromStream();
                    if(current == null){
                        throw new NoSuchElementException();
                    }
                }
                return current.getString(id);
            }

            private Tuple nextTupleFromStream() {
                try {
                    return Failsafe.with(retryPolicy).get(tupleStream::read);
                } catch (Exception e) {
                    LOGGER.error("Error whilst iterating through Solr results stream", e);
                    closeTupleStream();
                    throw new IllegalStateException(e);
                }
            }

            private void closeTupleStream() {
                try {
                    tupleStream.close();
                } catch (Exception e) {
                    LOGGER.error("Error whilst closing Solr results stream", e);
                    throw new IllegalStateException(e);
                }
            }
        };
    }
}