package uk.ac.ebi.uniprot.common.exception;

/**
 * Represents a problem when do not find the requested entry.
 *
 * @author lgonzales
 */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message, Throwable cause) {
        super(message,cause);
    }

    public ResourceNotFoundException(String message) {
        super(message);
    }
}