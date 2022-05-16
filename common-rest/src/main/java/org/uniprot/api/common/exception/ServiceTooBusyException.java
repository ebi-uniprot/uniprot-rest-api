package org.uniprot.api.common.exception;

/**
 * Created 13/05/2022
 *
 * @author Edd
 */
public class ServiceTooBusyException extends RuntimeException {
    public ServiceTooBusyException() {
        super();
    }

    public ServiceTooBusyException(String message, Throwable cause) {
        super(message, cause);
    }

    public ServiceTooBusyException(String message) {
        super(message);
    }
}
