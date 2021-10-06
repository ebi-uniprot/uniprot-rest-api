package org.uniprot.api.common.exception;

/**
 * Represents a problem in the service layer, whose associated message is important and should
 * be shown to the client.
 *
 * Created 06/10/2021
 *
 * @author Edd
 */
public class ImportantMessageServiceException extends RuntimeException {
    public ImportantMessageServiceException(String message, Throwable cause) {
        super(message, cause);
    }

    public ImportantMessageServiceException(String message) {
        super(message);
    }
}
