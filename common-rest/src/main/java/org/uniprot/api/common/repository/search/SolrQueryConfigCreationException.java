package org.uniprot.api.common.repository.search;

/**
 * Represents a problem when creating query boosts.
 *
 * @author Edd
 */
public class SolrQueryConfigCreationException extends RuntimeException {
    public SolrQueryConfigCreationException(String message, Throwable cause) {
        super(message, cause);
    }

    public SolrQueryConfigCreationException(String message) {
        super(message);
    }
}
