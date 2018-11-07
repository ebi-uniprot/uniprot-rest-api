package uk.ac.ebi.uniprot.rest.output.converter;

import java.util.stream.Stream;

/**
 * Class whose purpose is to be thrown within a {@link Stream}'s processing, which indicates that an error
 * has occurred, and the {@link Stream} should be closed.
 *
 * Created 31/01/17
 * @author Edd
 */
public class StopStreamException extends RuntimeException {
    public StopStreamException() {
        super();
    }

    public StopStreamException(String message) {
        super(message);
    }

    public StopStreamException(String message, Throwable cause) {
        super(message, cause);
    }
}