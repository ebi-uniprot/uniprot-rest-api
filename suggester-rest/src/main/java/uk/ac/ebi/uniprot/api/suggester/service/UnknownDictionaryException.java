package uk.ac.ebi.uniprot.api.suggester.service;

/**
 * This class represents an attempt to create an invalid dictionary.
 *
 * Created 18/07/18
 *
 * @author Edd
 */
public class UnknownDictionaryException extends RuntimeException {
    UnknownDictionaryException(String message) {
        super(message);
    }
}
