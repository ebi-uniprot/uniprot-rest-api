package uk.ac.ebi.uniprot.uuw.advanced.search.http.converter2;

import java.util.stream.Stream;

/**
 * Class whose purpose is to be thrown within a {@link Stream}'s processing, which indicates that an error
 * has occurred, and the {@link Stream} should be closed.
 *
 * Created 31/01/17
 * @author Edd
 */
class StopStreamException extends RuntimeException {
    StopStreamException() {
        super();
    }

    StopStreamException(String message) {
        super(message);
    }

    StopStreamException(String message, Throwable cause) {
        super(message, cause);
    }
}
