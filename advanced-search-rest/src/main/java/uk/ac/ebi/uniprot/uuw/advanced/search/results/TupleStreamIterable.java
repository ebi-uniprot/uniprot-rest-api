package uk.ac.ebi.uniprot.uuw.advanced.search.results;

import org.apache.solr.client.solrj.io.Tuple;
import org.apache.solr.client.solrj.io.stream.TupleStream;
import org.slf4j.Logger;

import java.util.Iterator;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * Created 21/08/18
 *
 * @author Edd
 */
public class TupleStreamIterable implements Iterable<String> {
    private static final Logger LOGGER = getLogger(TupleStreamIterable.class);
    private final TupleStream tupleStream;
    private final String id;
    private Tuple current = new Tuple();
    private Tuple next = current;
    private boolean atEnd = false;

    public TupleStreamIterable(TupleStream tupleStream, String id) {
        this.tupleStream = tupleStream;
        this.id = id;
    }

    @Override
    public Iterator<String> iterator() {
        return new Iterator<String>() {
            @Override
            public boolean hasNext() {
//                try {
//                    current = tupleStream.read();
//                    boolean eof = current.EOF;
//                    if (eof) {
//                        tupleStream.close();
//                    }
//                    return !eof;
//                } catch (Exception e) {
//                    LOGGER.error("Error whilst iterating through Solr results stream", e);
//                    throw new IllegalStateException(e);
//                }

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
                }
                return current.getString(id);
            }

            private Tuple nextTupleFromStream() {
                try {
                    return tupleStream.read();
                } catch (Exception e) {
                    LOGGER.error("Error whilst iterating through Solr results stream", e);
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