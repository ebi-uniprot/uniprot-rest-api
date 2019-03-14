package uk.ac.ebi.uniprot.common.exception;

/**
 * Represents a problem when executing a service.
 *
 * @author lgonzales
 */
public class ServiceException extends RuntimeException {

    public ServiceException(String message,Throwable cause) {
        super(message,cause);
    }

    public ServiceException(String message) {
        super(message);
    }
}
