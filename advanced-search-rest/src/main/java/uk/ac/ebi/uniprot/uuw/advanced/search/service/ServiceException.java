package uk.ac.ebi.uniprot.uuw.advanced.search.service;

/**
 * Represents a problem when executing a service.
 *
 * @author lgonzales
 */
public class ServiceException extends RuntimeException {

    ServiceException(String message,Throwable cause) {
        super(message,cause);
    }
}
