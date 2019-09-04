package org.uniprot.api.common.repository.search;

/**
 * Represents a problem when retrieving information from Solr.
 *
 * @author lgonzales
 */
public class QueryBoostCreationException extends RuntimeException {
    public QueryBoostCreationException(String message, Throwable cause) {
        super(message, cause);
    }

    public QueryBoostCreationException(String message) {
        super(message);
    }
}
