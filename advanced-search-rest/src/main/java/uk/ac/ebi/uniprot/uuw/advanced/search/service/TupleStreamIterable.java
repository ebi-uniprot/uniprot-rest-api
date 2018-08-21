package uk.ac.ebi.uniprot.uuw.advanced.search.service;

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
public class TupleStreamIterable  implements Iterable<String> {
    private static final Logger LOGGER = getLogger(TupleStreamIterable.class);
    private static final String KEY = "accession_exact";
    private final TupleStream tupleStream;
    private Tuple current;

    public TupleStreamIterable(TupleStream tupleStream) {
        this.tupleStream = tupleStream;
    }

    @Override
    public Iterator<String> iterator() {
        return new Iterator<String>() {
            @Override
            public boolean hasNext() {
                try {
                    current = tupleStream.read();
                    boolean eof = current.EOF;
                    if (eof) {
                        tupleStream.close();
                    }
                    return !eof;
                } catch (Exception e) {
                    LOGGER.error("Error whilst iterating through Solr results stream", e);
                    throw new IllegalStateException(e);
                }
            }

            @Override
            public String next() {
                return current.getString(KEY);
            }
        };
    }
}