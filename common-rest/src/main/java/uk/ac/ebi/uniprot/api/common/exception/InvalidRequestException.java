package uk.ac.ebi.uniprot.api.common.exception;

/**
 * Represents an invalid request originating from a web service layer.
 *
 * @author Edd
 */
public class InvalidRequestException extends RuntimeException {

    public InvalidRequestException(String message, Throwable cause) {
        super(message,cause);
    }

    public InvalidRequestException(String message) {
        super(message);
    }
}
