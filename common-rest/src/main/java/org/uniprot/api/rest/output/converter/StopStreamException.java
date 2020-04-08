package org.uniprot.api.rest.output.converter;

import java.util.stream.Stream;

/**
 * Class whose purpose is to be thrown within a {@link Stream}'s processing, which indicates that an
 * error has occurred, and the {@link Stream} should be closed.
 *
 * <p>Created 31/01/17
 *
 * @author Edd
 */
class StopStreamException extends RuntimeException {
    private static final long serialVersionUID = 457330583994736749L;

    StopStreamException(String message, Throwable cause) {
        super(message, cause);
    }
}
