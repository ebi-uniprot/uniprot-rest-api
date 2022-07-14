package org.uniprot.api.common.exception;

/**
 * @author sahmad
 * @created 14/07/2022
 */
public class ForbiddenRequestException extends RuntimeException {
    public ForbiddenRequestException(String message, Throwable cause) {
        super(message, cause);
    }

    public ForbiddenRequestException(String message) {
        super(message);
    }
}
