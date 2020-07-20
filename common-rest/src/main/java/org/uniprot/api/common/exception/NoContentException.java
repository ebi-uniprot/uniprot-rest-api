package org.uniprot.api.common.exception;

/**
 * Represents no content for a valid entity.
 *
 * @author Edd
 */
public class NoContentException extends RuntimeException {

    public NoContentException(String message, Throwable cause) {
        super(message, cause);
    }

    public NoContentException(String message) {
        super(message);
    }
}
