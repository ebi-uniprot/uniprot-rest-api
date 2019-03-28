package uk.ac.ebi.uniprot.api.suggester.service;

/**
 * Represents a problem when retrieving suggestions.
 *
 * Created 18/07/18
 *
 * @author Edd
 */
public class SuggestionRetrievalException extends RuntimeException {
    SuggestionRetrievalException(String message) {
        super(message);
    }
}
