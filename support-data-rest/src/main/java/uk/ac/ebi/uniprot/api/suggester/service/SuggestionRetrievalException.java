package uk.ac.ebi.uniprot.api.suggester.service;

/**
 * Created 18/05/19
 *
 * @author Edd
 */
public class SuggestionRetrievalException extends RuntimeException {
    /**
     * Represents a problem when retrieving suggestions.
     * <p>
     * Created 18/07/18
     *
     * @author Edd
     */
    SuggestionRetrievalException(String message, Throwable cause) {
        super(message, cause);
    }
}
