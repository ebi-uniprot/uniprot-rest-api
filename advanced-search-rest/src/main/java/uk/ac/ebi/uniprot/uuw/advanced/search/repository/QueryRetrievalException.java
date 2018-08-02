package uk.ac.ebi.uniprot.uuw.advanced.search.repository;

/**
 * Represents a problem when retrieving information from Solr.
 *
 * @author lgonzales
 */
public class QueryRetrievalException extends RuntimeException {

    QueryRetrievalException(String message,Throwable cause) {
        super(message,cause);
    }
}
