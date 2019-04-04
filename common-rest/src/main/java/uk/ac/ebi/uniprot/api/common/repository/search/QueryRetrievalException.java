package uk.ac.ebi.uniprot.api.common.repository.search;

/**
 * Represents a problem when retrieving information from Solr.
 *
 * @author lgonzales
 */
public class QueryRetrievalException extends RuntimeException {

    public QueryRetrievalException(String message,Throwable cause) {
        super(message,cause);
    }
}
