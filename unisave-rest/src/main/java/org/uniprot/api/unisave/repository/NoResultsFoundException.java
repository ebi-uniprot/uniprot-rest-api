package org.uniprot.api.unisave.repository;

public class NoResultsFoundException extends RuntimeException {
    public NoResultsFoundException (String message, Throwable cause) {
        super(message, cause);
    }

    public NoResultsFoundException (String message) {
        super(message);
    }
}